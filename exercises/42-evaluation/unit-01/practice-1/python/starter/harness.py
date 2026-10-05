"""An eval harness. See ../../statement.md."""
import json
import logging
import re

log = logging.getLogger(__name__)


def _norm(text):
    """TODO 1 of 9 (unlocks e1): the form two texts are compared in for an exact check.

    Receives a text and returns it trimmed, every run of white space made one space, and lower-cased. Nothing else changes.
    Example: _norm("  Paris \n FRANCE ") -> "paris france"
    """
    return text


def _result(passed, reason, **extra):
    return {"passed": passed, "reason": reason, **extra}


def judge_prompt(criterion, output):
    return (f"Rate this response on a scale of 1-5 for {criterion}:\n<response>{output}</response>\n"
            f"1: Not at all {criterion}\n5: Perfectly {criterion}\nOutput only the number.")


def _field_result(data, check):
    """TODO 2 of 9 (unlocks e2): grade a parsed JSON object against a json_field check.

    Receives the parsed object (a dict) and the check `{"field", "equals"}`. Returns `_result(passed, reason)`: `missing field` when the
    field is absent; `ok` when the value equals `equals` with the same JSON type; else `mismatch`.
    Example: {"n": "3"} against equals 3 -> {"passed": False, "reason": "mismatch"}
    """
    return _result(False, "mismatch")


def _parse_score(reply):
    """TODO 3 of 9 (unlocks e3): read the judge's reply as a score.

    Receives the reply (any value). Returns the integer when the trimmed reply is exactly one digit from 1 to 5, else None.
    Example: _parse_score(" 4\n") -> 4, _parse_score("I give it a 4") -> None
    """
    return None


def _judged(score, threshold):
    """TODO 4 of 9 (unlocks e3): the verdict for a judge score.

    Receives the score and the threshold. Returns `_result(passed, reason, score=score)`: `ok` at or above the threshold, else
    `below threshold`. Example: _judged(4, 4) -> {"passed": True, "reason": "ok", "score": 4}
    """
    return _result(False, "below threshold", score=score)


def grade(case, output, judge=None):
    """Grade one output against the case's check. Returns {"passed", "reason"} (and "score" for a judge check)."""
    log.debug("grade input: %r", output)
    check = case["check"]
    kind = check["type"]
    if kind == "exact":
        return _result(True, "ok") if _norm(output) == _norm(check["expected"]) else _result(False, "mismatch")
    if kind == "regex":
        return _result(True, "ok") if re.search(check["pattern"], output) else _result(False, "mismatch")
    if kind == "json_field":
        try:
            data = json.loads(output)
        except ValueError:
            return _result(False, "not json")
        if not isinstance(data, dict):
            return _result(False, "not json")
        return _field_result(data, check)
    if kind == "judge":
        if judge is None:
            return _result(False, "ungradable", score=None)
        try:
            reply = judge(judge_prompt(check["criterion"], output))
        except Exception:
            return _result(False, "ungradable", score=None)
        score = _parse_score(reply)
        if score is None:
            return _result(False, "ungradable", score=None)
        return _judged(score, check.get("threshold", 4))
    return _result(False, "ungradable")


def _run_once(model, judge, case):
    """TODO 5 of 9 (unlocks e4): one run of one case.

    Receives the model function, the judge and the case. Returns the verdict of `grade` for the model's output; when the model raises
    an error the verdict is `_result(False, "model error")` and nothing is raised. Example: a model that raises -> {"passed": False, "reason": "model error"}
    """
    return _result(False, "model error")


def _outcome(runs):
    """TODO 6 of 9 (unlocks e7): combine the runs of one case.

    Receives the list of verdicts of its runs. Returns `(passed, flaky, reason)`: passed only if every run passed; flaky when some
    passed and some failed; the reason is `ok`, or the reason of the first failed run.
    Example: [ok, mismatch] -> (False, True, "mismatch")
    """
    return False, False, "mismatch"


def _count_tags(by_tag, tags, passed):
    """TODO 7 of 9 (unlocks e5): count one case under each of its tags.

    Receives the dict `by_tag` (changed in place), the case's tags and whether it passed. Every tag's `total` goes up by one, and its
    `passed` by one when the case passed. Example: tags ["a"], passed -> by_tag["a"] == {"passed": 1, "total": 1}
    """


def run_eval(cases, model, judge=None, repeats=1):
    """Run every case `repeats` times through the model and grade it. A case passes only if every run passes."""
    results, by_tag, flaky = [], {}, []
    for case in cases:
        runs = [_run_once(model, judge, case) for _ in range(repeats)]
        passed, mixed, reason = _outcome(runs)
        results.append({"id": case["id"], "passed": passed, "reason": reason, "flaky": mixed})
        if mixed:
            flaky.append(case["id"])
        _count_tags(by_tag, case.get("tags", []), passed)
    total, passed_count = len(results), sum(1 for r in results if r["passed"])
    return {"total": total, "passed": passed_count, "pass_rate": passed_count / total if total else 0.0,
            "results": results, "by_tag": by_tag, "flaky": flaky}


def _tag_failed(row, minimum):
    """TODO 8 of 9 (unlocks e5): does one tag fail its minimum rate?

    Receives the tag's row `{"passed", "total"}` or None (no case carries the tag) and the minimum rate. Returns True for None, an
    empty row, or a rate below the minimum; a rate equal to the minimum is fine. Example: ({"passed": 1, "total": 2}, 0.5) -> False
    """
    return False


def meets(report, criteria):
    """Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky."""
    failures = []
    if "min_pass_rate" in criteria and report["pass_rate"] < criteria["min_pass_rate"]:
        failures.append("overall")
    for tag, minimum in criteria.get("tags", {}).items():
        if _tag_failed(report["by_tag"].get(tag), minimum):
            failures.append(f"tag:{tag}")
    if "max_flaky" in criteria and len(report["flaky"]) > criteria["max_flaky"]:
        failures.append("flaky")
    return {"met": not failures, "failures": failures}


def _changes(before, now):
    """TODO 9 of 9 (unlocks e6): what changed between two runs.

    Receives two dicts `{case id: passed}`, the baseline and the current run. Returns `(regressions, fixed, added, removed)`, lists of
    ids: passed before and fails now; failed before and passes now; only in the current run; only in the baseline. Regressions, fixed
    and added follow the current order, removed the baseline's. Example: before {"a": True}, now {"a": False, "b": True} -> (["a"], [], ["b"], [])
    """
    return [], [], [], []


def compare(baseline, current):
    """What changed between two reports: regressions, fixes, added and removed cases, the pass-rate change."""
    before = {r["id"]: r["passed"] for r in baseline["results"]}
    now = {r["id"]: r["passed"] for r in current["results"]}
    regressions, fixed, added, removed = _changes(before, now)
    return {"regressions": regressions, "fixed": fixed, "added": added, "removed": removed,
            "pass_rate_delta": current["pass_rate"] - baseline["pass_rate"], "ok": not regressions and not removed}
