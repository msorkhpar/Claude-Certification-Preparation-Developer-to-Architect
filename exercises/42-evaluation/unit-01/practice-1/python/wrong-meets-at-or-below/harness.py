"""An eval harness. See ../../statement.md."""
import json
import re


def _norm(text):
    return " ".join(text.split()).lower()


def _result(passed, reason, **extra):
    return {"passed": passed, "reason": reason, **extra}


def judge_prompt(criterion, output):
    return (f"Rate this response on a scale of 1-5 for {criterion}:\n<response>{output}</response>\n"
            f"1: Not at all {criterion}\n5: Perfectly {criterion}\nOutput only the number.")


def grade(case, output, judge=None):
    """Grade one output against the case's check. Returns {"passed", "reason"} (and "score" for a judge check)."""
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
        if check["field"] not in data:
            return _result(False, "missing field")
        actual, want = data[check["field"]], check["equals"]
        return _result(True, "ok") if type(actual) is type(want) and actual == want else _result(False, "mismatch")
    if kind == "judge":
        if judge is None:
            return _result(False, "ungradable", score=None)
        try:
            reply = judge(judge_prompt(check["criterion"], output))
        except Exception:
            return _result(False, "ungradable", score=None)
        text = reply.strip() if isinstance(reply, str) else ""
        if not re.fullmatch(r"[1-5]", text):
            return _result(False, "ungradable", score=None)
        score = int(text)
        if score >= check.get("threshold", 4):
            return _result(True, "ok", score=score)
        return _result(False, "below threshold", score=score)
    return _result(False, "ungradable")


def run_eval(cases, model, judge=None, repeats=1):
    """Run every case `repeats` times through the model and grade it. A case passes only if every run passes."""
    results, by_tag, flaky = [], {}, []
    for case in cases:
        runs = []
        for _ in range(repeats):
            try:
                output = model(case["input"])
            except Exception:
                runs.append(_result(False, "model error"))
                continue
            runs.append(grade(case, output, judge))
        passed = all(r["passed"] for r in runs)
        mixed = any(r["passed"] for r in runs) and not passed
        reason = "ok" if passed else next(r["reason"] for r in runs if not r["passed"])
        results.append({"id": case["id"], "passed": passed, "reason": reason, "flaky": mixed})
        if mixed:
            flaky.append(case["id"])
        for tag in case.get("tags", []):
            row = by_tag.setdefault(tag, {"passed": 0, "total": 0})
            row["total"] += 1
            row["passed"] += 1 if passed else 0
    total, passed_count = len(results), sum(1 for r in results if r["passed"])
    return {"total": total, "passed": passed_count, "pass_rate": passed_count / total if total else 0.0,
            "results": results, "by_tag": by_tag, "flaky": flaky}


def meets(report, criteria):
    """Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky."""
    failures = []
    if "min_pass_rate" in criteria and report["pass_rate"] <= criteria["min_pass_rate"]:
        failures.append("overall")
    for tag, minimum in criteria.get("tags", {}).items():
        row = report["by_tag"].get(tag)
        if row is None or row["total"] == 0 or row["passed"] / row["total"] < minimum:
            failures.append(f"tag:{tag}")
    if "max_flaky" in criteria and len(report["flaky"]) > criteria["max_flaky"]:
        failures.append("flaky")
    return {"met": not failures, "failures": failures}


def compare(baseline, current):
    """What changed between two reports: regressions, fixes, added and removed cases, the pass-rate change."""
    before = {r["id"]: r["passed"] for r in baseline["results"]}
    now = {r["id"]: r["passed"] for r in current["results"]}
    regressions = [i for i in now if i in before and before[i] and not now[i]]
    fixed = [i for i in now if i in before and not before[i] and now[i]]
    added = [i for i in now if i not in before]
    removed = [i for i in before if i not in now]
    return {"regressions": regressions, "fixed": fixed, "added": added, "removed": removed,
            "pass_rate_delta": current["pass_rate"] - baseline["pass_rate"], "ok": not regressions and not removed}
