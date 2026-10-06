"""Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md."""
import math
import logging

log = logging.getLogger(__name__)


def review_changes(files, file_pass, cross_pass, max_lines=40):
    """Review each file alone (long files in parts), then let one pass read only the summaries of the files that were reviewed."""
    log.debug("review_changes input: %r", files)
    reviewed, failed, skipped = {}, {}, []
    for item in files:
        path, text = item["path"], item["text"]
        # TODO 2 of 9 (finish this to pass e2): the blank file rule. When the file's text is blank, add its path to
        #   `skipped` and go on to the next file. Example: "\n  \n" -> skipped.
        lines = text.splitlines()
        # TODO 3 of 9 (finish this to pass e2): the number of parts. Receives the lines of the file and max_lines.
        #   Return how many parts of at most max_lines lines the file needs, rounding up. Example: 41 lines, max_lines 40
        #   -> 2.
        parts = 1
        findings, summaries = [], []
        try:
            for part in range(parts):
                # TODO 1 of 9 (finish this to pass m1, e1): the review of one part of a file. Slice the part's lines
                #   (part number 1..parts, max_lines per part), call the file pass with (path, text of the part, part
                #   number from 1, parts), add its findings to `findings` and its summary to `summaries`. Example: a 5
                #   line file, max_lines 3 -> two calls, parts 1 and 2 of 2.
                pass
        # TODO 4 of 9 (finish this to pass e3): the failing file. When the file pass throws, record the path with the
        #   error's message in `failed` and go on to the next file. Example: the pass raises "boom" for a.py -> failed
        #   {a.py: "boom"}, and b.py is still reviewed.
        except Exception:
            continue
        reviewed[path] = {"findings": findings, "summary": " ".join(summaries), "parts": parts}
    cross, cross_error = [], None
    # TODO 5 of 9 (finish this to pass e3): the guard of the cross pass. Run it only when at least two files were
    #   reviewed (a relation between files needs two). Example: one reviewed file and one failed -> no cross pass.
    if len(reviewed) >= 0:
        try:
            # TODO 6 of 9 (finish this to pass m1, e1): the cross pass call. Call the cross pass with one {path,
            #   summary} entry per reviewed file (the joined summary, never the text) and keep what it returns in `cross`.
            #   Example: two files -> one call with two entries.
            cross = []
        except Exception as error:
            cross_error = str(error)
    return {"files": reviewed, "cross": cross, "failed": failed, "skipped": skipped, "cross_error": cross_error}


def run_adaptive(planner, worker, goal, max_steps=6):
    """Ask the planner what to do next after every step, and stop when it is done, stuck or out of steps."""
    steps = []

    def finish(status, summary="", reason=""):
        return {"status": status, "summary": summary, "steps": steps, "reason": reason}

    while True:
        # TODO 7 of 9 (finish this to pass e4): the question to the planner. Call the planner with the goal and a copy
        #   of the steps done so far (each {subtask, result}). Example: after two steps the planner receives a list of
        #   two.
        plan = planner(goal, [])
        if not isinstance(plan, dict) or not isinstance(plan.get("done"), bool):
            return finish("bad_plan", reason="the planner reply could not be read")
        if plan["done"]:
            return finish("done", str(plan.get("summary") or ""))
        subtask = str(plan.get("next") or "").strip()
        # TODO 8 of 9 (finish this to pass e5): the stuck rules. When the planner gives no next step, finish with status
        #   stuck and the reason "no next step"; when the next step was already done (compare ignoring case), finish with
        #   status stuck and the reason "repeated subtask: NAME". Example: next "Fix A" after "fix a" -> stuck.
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
    # TODO 9 of 9 (finish this to pass e6): the choice. Receives the validated fields. Return adaptive when the steps
    #   are not known; otherwise per_item_then_cross when there are at least 2 items and items_interact is true; otherwise
    #   fixed_chain. Example: steps known, 3 items that interact -> per_item_then_cross.
    return "fixed_chain"
