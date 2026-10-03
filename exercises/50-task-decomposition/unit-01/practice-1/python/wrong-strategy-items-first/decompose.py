"""Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md."""
import math


def review_changes(files, file_pass, cross_pass, max_lines=40):
    """Review each file alone (long files in parts), then let one pass read only the summaries of the files that were reviewed."""
    reviewed, failed, skipped = {}, {}, []
    for item in files:
        path, text = item["path"], item["text"]
        if not text.strip():
            skipped.append(path)  # nothing to review is not worth a model call
            continue
        lines = text.splitlines()
        parts = math.ceil(len(lines) / max_lines)
        findings, summaries = [], []
        try:
            for part in range(parts):
                chunk = "\n".join(lines[part * max_lines:(part + 1) * max_lines])
                result = file_pass(path, chunk, part + 1, parts)
                findings.extend(result["findings"])
                summaries.append(result["summary"])
        except Exception as error:  # one file failing must not stop the others
            failed[path] = str(error)
            continue
        reviewed[path] = {"findings": findings, "summary": " ".join(summaries), "parts": parts}
    cross, cross_error = [], None
    if len(reviewed) >= 2:  # a relation between files needs at least two of them
        try:
            cross = list(cross_pass([{"path": p, "summary": r["summary"]} for p, r in reviewed.items()]))
        except Exception as error:
            cross_error = str(error)
    return {"files": reviewed, "cross": cross, "failed": failed, "skipped": skipped, "cross_error": cross_error}


def run_adaptive(planner, worker, goal, max_steps=6):
    """Ask the planner what to do next after every step, and stop when it is done, stuck or out of steps."""
    steps = []

    def finish(status, summary="", reason=""):
        return {"status": status, "summary": summary, "steps": steps, "reason": reason}

    while True:
        plan = planner(goal, [dict(step) for step in steps])
        if not isinstance(plan, dict) or not isinstance(plan.get("done"), bool):
            return finish("bad_plan", reason="the planner reply could not be read")
        if plan["done"]:
            return finish("done", str(plan.get("summary") or ""))
        subtask = str(plan.get("next") or "").strip()
        if not subtask:
            return finish("stuck", reason="no next step")
        if subtask.lower() in {step["subtask"].lower() for step in steps}:
            return finish("stuck", reason=f"repeated subtask: {subtask}")
        if len(steps) >= max_steps:
            return finish("step_limit", reason="step limit reached")
        try:
            result = worker(subtask)
        except Exception as error:  # the planner decides what a failed step means
            result = f"ERROR: {error}"
        steps.append({"subtask": subtask, "result": result})


def choose_strategy(task):
    """fixed_chain, per_item_then_cross or adaptive, from what is known about the steps and whether the items affect each other."""
    steps_known, items = task.get("steps_known"), task.get("items")
    if not isinstance(steps_known, bool) or isinstance(items, bool) or not isinstance(items, int) or items < 0:
        raise ValueError("steps_known must be true or false and items a whole number of at least 0")
    if items >= 2 and task.get("items_interact") is True:
        return "per_item_then_cross"
    if not steps_known:
        return "adaptive"
    return "fixed_chain"
