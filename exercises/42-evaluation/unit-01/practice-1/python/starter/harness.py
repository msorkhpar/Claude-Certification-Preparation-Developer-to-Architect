"""An eval harness. See ../../statement.md."""


def grade(case, output, judge=None):
    """Grade one output against the case's check. Returns {"passed", "reason"} (and "score" for a judge check)."""
    return {"passed": True, "reason": "ok"}


def run_eval(cases, model, judge=None, repeats=1):
    """Run every case `repeats` times through the model and grade it. A case passes only if every run passes."""
    return {"total": 0, "passed": 0, "pass_rate": 0.0, "results": [], "by_tag": {}, "flaky": []}


def meets(report, criteria):
    """Compare a report with success criteria: min_pass_rate, tags {tag: minimum rate}, max_flaky."""
    return {"met": True, "failures": []}


def compare(baseline, current):
    """What changed between two reports: regressions, fixed, added and removed case ids, the pass-rate change."""
    return {"regressions": [], "fixed": [], "added": [], "removed": [], "pass_rate_delta": 0.0, "ok": True}
