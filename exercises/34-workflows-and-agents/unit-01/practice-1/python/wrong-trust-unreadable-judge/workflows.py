"""Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md."""
import json


def _slice(text, open_char, close_char):
    first, last = text.find(open_char), text.rfind(close_char)
    return text[first:last + 1] if first != -1 and last > first else ""


def parse_plan(text, task, max_subtasks):
    """Subtasks from the planner's reply, or ([task], True) when the reply cannot be used."""
    try:
        data = json.loads(_slice(text, "[", "]"))
    except ValueError:
        data = None
    steps = []
    if isinstance(data, list):
        for item in data:
            if isinstance(item, str) and item.strip() and item.strip() not in steps:
                steps.append(item.strip())
    if not steps:
        return [task], True
    return steps[:max_subtasks], False


def orchestrate(ask, task, max_subtasks=5):
    calls = 1
    plan, fallback = parse_plan(ask(f"Plan: split the task into at most {max_subtasks} independent subtasks. Reply with a JSON array of strings only.\nTask: {task}"), task, max_subtasks)
    results = []
    for subtask in plan:
        calls += 1
        try:
            output = ask(f"Subtask: {subtask}\nTask: {task}")
            if not isinstance(output, str) or not output.strip():
                raise ValueError("empty reply")
            results.append({"subtask": subtask, "status": "ok", "output": output})
        except Exception as err:  # noqa: BLE001 - one worker failing must not stop the others
            results.append({"subtask": subtask, "status": "failed", "error": str(err)})
    ok = [r for r in results if r["status"] == "ok"]
    if not ok:
        return {"status": "failed", "plan": plan, "fallback": fallback, "results": results, "answer": None, "calls": calls}
    lines = [f"{i}. {r['subtask']} -> {r['output'] if r['status'] == 'ok' else 'FAILED'}" for i, r in enumerate(results, start=1)]
    answer = ask("Combine: write one answer to the task from the results.\nTask: " + task + "\n" + "\n".join(lines))
    return {"status": "done" if len(ok) == len(results) else "partial", "plan": plan, "fallback": fallback, "results": results, "answer": answer, "calls": calls + 1}


def read_judgement(text):
    """(score, feedback) from the judge's reply; (0, a fixed note) when the reply cannot be read."""
    try:
        data = json.loads(_slice(text, "{", "}"))
    except ValueError:
        data = None
    score = data.get("score") if isinstance(data, dict) else None
    if isinstance(score, bool) or not isinstance(score, (int, float)) or not 0 <= score <= 10:
        return 10, "The judge reply could not be read."
    feedback = data.get("feedback")
    return score, feedback if isinstance(feedback, str) else ""


def refine(write, judge, task, max_rounds=3, threshold=8):
    history, best, best_score, draft, feedback = [], None, None, None, ""
    for round_number in range(1, max_rounds + 1):
        prompt = f"Task: {task}" if round_number == 1 else f"Task: {task}\nPrevious draft: {draft}\nFeedback: {feedback}\nRevise the draft."
        try:
            draft = write(prompt)
        except Exception as err:  # noqa: BLE001
            return {"status": "error", "draft": best, "score": best_score, "rounds": round_number - 1, "history": history, "error": str(err)}
        score, feedback = read_judgement(judge(f'Judge: score the draft from 0 to 10 and reply with JSON {{"score": n, "feedback": "..."}}.\nTask: {task}\nDraft: {draft}'))
        history.append({"round": round_number, "score": score, "feedback": feedback})
        if best_score is None or score > best_score:
            best, best_score = draft, score
        if score >= threshold:
            return {"status": "accepted", "draft": draft, "score": score, "rounds": round_number, "history": history}
    return {"status": "max_rounds", "draft": best, "score": best_score, "rounds": max_rounds, "history": history}


def route(ask, text, routes, default):
    """Classify the text with one model call, then run the handler of the label. `routes` maps label to a function of the text."""
    reply = ask(f"Classify: {text}\nLabels: {', '.join(routes)}")
    label = reply.strip().strip(".,;:!\"'`").strip().lower() if isinstance(reply, str) else ""
    fallback = label not in routes
    if fallback:
        label = default
    return {"label": label, "output": routes[label](text), "fallback": fallback}


def vote(ask, prompt, n=5):
    counts = {}
    for _ in range(n):
        answer = ask(prompt).strip().lower()
        counts[answer] = counts.get(answer, 0) + 1
    if not counts:
        return {"answer": None, "votes": {}, "agreement": 0.0}
    top = max(counts.values())
    winner = next(a for a, c in counts.items() if c == top)
    return {"answer": winner, "votes": counts, "agreement": top / n}
