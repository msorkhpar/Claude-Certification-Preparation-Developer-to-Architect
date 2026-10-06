"""Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md."""
import json
import logging

log = logging.getLogger(__name__)


def _slice(text, open_char, close_char):
    first, last = text.find(open_char), text.rfind(close_char)
    return text[first:last + 1] if first != -1 and last > first else ""


def _clean_steps(data):
    """TODO 1 of 9 (unlocks e1): the usable subtasks from the parsed plan.

    Receives whatever `json.loads` gave (a list, or anything else) and returns the string items of a list, stripped, with empty
    ones and repeats dropped (the first of a repeat stays), in order; anything that is not a list gives [].
    Example: _clean_steps(["a", " a ", "", 3, "b"]) -> ["a", "b"]
    """
    return []


def parse_plan(text, task, max_subtasks):
    """Subtasks from the planner's reply, or ([task], True) when the reply cannot be used."""
    try:
        data = json.loads(_slice(text, "[", "]"))
    except ValueError:
        data = None
    steps = _clean_steps(data)
    if not steps:
        return [task], True
    return steps[:max_subtasks], False


def _run_worker(ask, subtask, task):
    """TODO 2 of 9 (unlocks m1 and e2): run one worker call for one subtask.

    Receives `ask`, the subtask and the task. Calls `ask` with `Subtask: {subtask}\\nTask: {task}` and returns
    {"subtask", "status": "ok", "output"} for a good reply, or {"subtask", "status": "failed", "error"} when the reply is empty or
    only spaces (error `empty reply`) or the call raises (error: the exception's message). It never raises.
    Example: a worker that replies "  " -> {"subtask": "s", "status": "failed", "error": "empty reply"}
    """
    log.debug("_run_worker input: %r", subtask)
    return {"subtask": subtask, "status": "failed", "error": ""}


def _combine_prompt(task, results):
    """TODO 3 of 9 (unlocks m1): the prompt of the combine call.

    Receives the task and the results in plan order. Returns `Combine: write one answer to the task from the results.\\nTask: {task}`
    followed by one line per result, `\\n{i}. {subtask} -> {output}` or `\\n{i}. {subtask} -> FAILED` (i from 1).
    Example: task "T", one ok result "a" with output "x" -> "Combine: write one answer to the task from the results.\\nTask: T\\n1. a -> x"
    """
    return ""


def orchestrate(ask, task, max_subtasks=5):
    calls = 1
    plan, fallback = parse_plan(ask(f"Plan: split the task into at most {max_subtasks} independent subtasks. Reply with a JSON array of strings only.\nTask: {task}"), task, max_subtasks)
    results = []
    for subtask in plan:
        calls += 1
        results.append(_run_worker(ask, subtask, task))
    ok = [r for r in results if r["status"] == "ok"]
    if not ok:
        return {"status": "failed", "plan": plan, "fallback": fallback, "results": results, "answer": None, "calls": calls}
    answer = ask(_combine_prompt(task, results))
    return {"status": "done" if len(ok) == len(results) else "partial", "plan": plan, "fallback": fallback, "results": results, "answer": answer, "calls": calls + 1}


def _score_ok(score):
    """TODO 4 of 9 (unlocks e4): is this a usable judge score?

    Receives the `score` field of the judge's JSON (any value, or None). Returns True for a number from 0 to 10 and False for
    anything else, a boolean included.
    Example: _score_ok(7.5) -> True, _score_ok(True) -> False, _score_ok(11) -> False
    """
    return isinstance(score, (int, float))  # a number of any size; add the range and the boolean exclusion


def read_judgement(text):
    """(score, feedback) from the judge's reply; (0, a fixed note) when the reply cannot be read."""
    try:
        data = json.loads(_slice(text, "{", "}"))
    except ValueError:
        data = None
    score = data.get("score") if isinstance(data, dict) else None
    if not _score_ok(score):
        return 0, "The judge reply could not be read."
    feedback = data.get("feedback")
    return score, feedback if isinstance(feedback, str) else ""


def _writer_prompt(task, draft, feedback, round_number):
    """TODO 5 of 9 (unlocks e3): the prompt for the writer in one round.

    Receives the task, the latest draft, the judge's latest feedback and the round number (from 1). Returns `Task: {task}` in round 1
    and afterwards `Task: {task}\\nPrevious draft: {draft}\\nFeedback: {feedback}\\nRevise the draft.`
    Example: _writer_prompt("T", "d", "f", 2) -> "Task: T\\nPrevious draft: d\\nFeedback: f\\nRevise the draft."
    """
    return f"Task: {task}"


def _is_better(score, best_score):
    """TODO 6 of 9 (unlocks e4): does this draft replace the best one so far?

    Receives the new score and the best score so far (None before any draft was judged). Returns True when there is no best yet or
    the new score is strictly higher (so the earliest wins a tie).
    Example: _is_better(5, None) -> True, _is_better(5, 5) -> False
    """
    return False


def _error_result(best, best_score, rounds, history, err):
    """TODO 7 of 9 (unlocks e7): the result when the writer raised.

    Receives the best draft and score so far (None, None before any), the rounds completed, the history and the exception. Returns the
    dict {"status": "error", "draft", "score", "rounds", "history", "error": <the exception's message>}.
    Example: _error_result("d", 4, 1, [], RuntimeError("boom"))["error"] -> "boom"
    """
    return {}


def refine(write, judge, task, max_rounds=3, threshold=8):
    history, best, best_score, draft, feedback = [], None, None, None, ""
    for round_number in range(1, max_rounds + 1):
        prompt = _writer_prompt(task, draft, feedback, round_number)
        try:
            draft = write(prompt)
        except Exception as err:  # noqa: BLE001
            return _error_result(best, best_score, round_number - 1, history, err)
        score, feedback = read_judgement(judge(f'Judge: score the draft from 0 to 10 and reply with JSON {{"score": n, "feedback": "..."}}.\nTask: {task}\nDraft: {draft}'))
        history.append({"round": round_number, "score": score, "feedback": feedback})
        if _is_better(score, best_score):
            best, best_score = draft, score
        if score >= threshold:
            return {"status": "accepted", "draft": draft, "score": score, "rounds": round_number, "history": history}
    return {"status": "max_rounds", "draft": best, "score": best_score, "rounds": max_rounds, "history": history}


def _normalise_label(reply):
    """TODO 8 of 9 (unlocks e5): the label in a classifier's reply.

    Receives the reply (a string, or anything else). Returns it stripped of spaces, then of the punctuation `.,;:!"'` and the backtick
    at both ends, then of spaces again, in lower case; "" when the reply is not a string.
    Example: _normalise_label('  "Billing." ') -> "billing"
    """
    return ""


def route(ask, text, routes, default):
    """Classify the text with one model call, then run the handler of the label. `routes` maps label to a function of the text."""
    reply = ask(f"Classify: {text}\nLabels: {', '.join(routes)}")
    label = _normalise_label(reply)
    fallback = label not in routes
    if fallback:
        label = default
    return {"label": label, "output": routes[label](text), "fallback": fallback}


def _pick_winner(counts, top):
    """TODO 9 of 9 (unlocks e6): the answer that wins the vote.

    Receives the counts (answer -> count, in order of first appearance) and the highest count. Returns the first answer whose count
    is that highest count, so a tie goes to the one seen first.
    Example: _pick_winner({"a": 2, "b": 2}, 2) -> "a"
    """
    return ""


def vote(ask, prompt, n=5):
    counts = {}
    for _ in range(n):
        answer = ask(prompt).strip().lower()
        counts[answer] = counts.get(answer, 0) + 1
    if not counts:
        return {"answer": None, "votes": {}, "agreement": 0.0}
    top = max(counts.values())
    winner = _pick_winner(counts, top)
    return {"answer": winner, "votes": counts, "agreement": top / n}
