import json
import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from harness import compare, grade, meets, run_eval


def case(check, **extra):
    return {"id": "c", "input": "x", "check": check, **extra}


def verdict(check, output, judge=None):
    r = grade(case(check), output, judge) or {}
    return r.get("passed"), r.get("reason")


def test_m1_a_run_grades_every_case_with_its_own_check_and_reports_the_pass_rate():
    cases = [
        {"id": "c1", "input": "I love it", "tags": ["core"], "check": {"type": "exact", "expected": "positive"}},
        {"id": "c2", "input": "awful", "tags": ["core"], "check": {"type": "exact", "expected": "negative"}},
        {"id": "c3", "input": "order 7", "tags": ["extract"], "check": {"type": "regex", "pattern": "ORD-\\d{4}"}},
        {"id": "c4", "input": "meh", "tags": ["core", "edge"], "check": {"type": "exact", "expected": "neutral"}},
    ]
    answers = {"I love it": "positive", "awful": "negative", "order 7": "The order is ORD-0007.", "meh": "positive"}
    report = run_eval(cases, lambda text: answers[text]) or {}
    assert report.get("total") == 4 and report.get("passed") == 3
    assert abs(report.get("pass_rate", -1) - 0.75) < 1e-9
    assert [(r["id"], r["passed"], r["reason"]) for r in report["results"]] == [
        ("c1", True, "ok"), ("c2", True, "ok"), ("c3", True, "ok"), ("c4", False, "mismatch")]
    empty = run_eval([], lambda text: text) or {}
    assert empty.get("total") == 0 and empty.get("pass_rate") == 0.0 and empty.get("results") == []


def test_e1_an_exact_check_ignores_case_and_spacing_but_nothing_else():
    exact = {"type": "exact", "expected": "not  enough info"}
    assert verdict({"type": "exact", "expected": "positive"}, "  Positive \n") == (True, "ok")
    assert verdict(exact, "Not enough\ninfo") == (True, "ok")
    assert verdict({"type": "exact", "expected": "positive"}, "positively") == (False, "mismatch")
    assert verdict({"type": "exact", "expected": "positive"}, "negative") == (False, "mismatch")
    assert verdict({"type": "exact", "expected": "positive"}, "") == (False, "mismatch")
    assert verdict({"type": "regex", "pattern": "^ORD-\\d{4}$"}, "ORD-12345") == (False, "mismatch")


def test_e2_a_json_field_check_needs_a_json_object_with_the_field_and_the_same_typed_value():
    spam = {"type": "json_field", "field": "label", "equals": "spam"}
    assert verdict(spam, '{"label":"spam","score":0.9}') == (True, "ok")
    assert verdict(spam, ' \n{"label": "spam"}\n') == (True, "ok")
    assert verdict(spam, '{"label":"ham"}') == (False, "mismatch")
    assert verdict(spam, '{"score":1}') == (False, "missing field")
    assert verdict(spam, 'Sure! {"label":"spam"}') == (False, "not json")
    assert verdict(spam, '```json\n{"label":"spam"}\n```') == (False, "not json")
    assert verdict(spam, '["spam"]') == (False, "not json")
    count = {"type": "json_field", "field": "count", "equals": 3}
    assert verdict(count, '{"count":3}') == (True, "ok")
    assert verdict(count, '{"count":"3"}') == (False, "mismatch")
    assert verdict({"type": "json_field", "field": "ok", "equals": True}, '{"ok":1}') == (False, "mismatch")


def test_e3_a_judge_check_sends_the_rubric_prompt_and_accepts_only_a_bare_score_at_the_threshold():
    check = {"type": "judge", "criterion": "empathetic", "threshold": 4}
    seen = []

    def judge(reply):
        def call(prompt):
            seen.append(prompt)
            return reply
        return call

    r = grade(case(check), "We are sorry.", judge("5")) or {}
    assert (r.get("passed"), r.get("reason"), r.get("score")) == (True, "ok", 5)
    assert seen == ["Rate this response on a scale of 1-5 for empathetic:\n<response>We are sorry.</response>\n"
                    "1: Not at all empathetic\n5: Perfectly empathetic\nOutput only the number."]
    assert verdict(check, "x", judge(" 4\n")) == (True, "ok")
    assert verdict(check, "x", judge("3")) == (False, "below threshold")
    assert verdict({"type": "judge", "criterion": "calm"}, "x", judge("4")) == (True, "ok")
    assert verdict({"type": "judge", "criterion": "calm"}, "x", judge("3")) == (False, "below threshold")
    for reply in ("Score: 4", "I'd say 4 or 5", "6", "0", "", "4.5"):
        assert verdict(check, "x", judge(reply)) == (False, "ungradable"), reply
    assert verdict(check, "x", None) == (False, "ungradable")

    def broken(prompt):
        raise RuntimeError("judge down")

    assert verdict(check, "x", broken) == (False, "ungradable")
    calls = []
    run_eval([{"id": "a", "input": "q", "check": {"type": "exact", "expected": "y"}}], lambda t: "y", lambda p: calls.append(p) or "5")
    assert calls == []


def test_e4_a_model_that_fails_on_one_case_does_not_stop_the_run():
    def model(text):
        if text == "boom":
            raise RuntimeError("503 from upstream")
        return "ok"

    cases = [{"id": n, "input": n, "check": {"type": "exact", "expected": "ok"}} for n in ("a", "boom", "c")]
    report = run_eval(cases, model) or {}
    assert report.get("total") == 3 and report.get("passed") == 2
    assert [(r["id"], r["passed"], r["reason"]) for r in report["results"]] == [("a", True, "ok"), ("boom", False, "model error"), ("c", True, "ok")]


def test_e5_tags_report_their_own_rates_and_success_criteria_judge_each_dimension():
    cases = [
        {"id": "a", "input": "1", "tags": ["core"], "check": {"type": "exact", "expected": "1"}},
        {"id": "b", "input": "2", "tags": ["core", "edge"], "check": {"type": "exact", "expected": "2"}},
        {"id": "c", "input": "3", "tags": ["edge"], "check": {"type": "exact", "expected": "x"}},
    ]
    report = run_eval(cases, lambda t: t) or {}
    assert report.get("by_tag") == {"core": {"passed": 2, "total": 2}, "edge": {"passed": 1, "total": 2}}
    rep = {"total": 10, "passed": 8, "pass_rate": 0.8, "results": [], "flaky": [],
           "by_tag": {"core": {"passed": 6, "total": 6}, "edge": {"passed": 2, "total": 4}}}
    assert meets(rep, {"min_pass_rate": 0.8, "tags": {"edge": 0.5}}) == {"met": True, "failures": []}
    assert meets(rep, {}) == {"met": True, "failures": []}
    assert meets(rep, {"min_pass_rate": 0.85, "tags": {"edge": 0.75, "core": 1.0, "rare": 0.5}}) == {
        "met": False, "failures": ["overall", "tag:edge", "tag:rare"]}
    assert meets({**rep, "flaky": ["c3"]}, {"max_flaky": 0}) == {"met": False, "failures": ["flaky"]}
    assert meets({**rep, "flaky": ["c3"]}, {"max_flaky": 1}) == {"met": True, "failures": []}


def report(rate, rows):
    return {"pass_rate": rate, "results": [{"id": i, "passed": p} for i, p in rows]}


def test_e6_a_regression_run_names_what_broke_what_was_fixed_and_what_went_missing():
    base = report(0.75, [("a", True), ("b", True), ("c", False), ("d", True)])
    now = report(0.75, [("a", True), ("b", False), ("c", True), ("e", True)])
    diff = compare(base, now) or {}
    assert (diff.get("regressions"), diff.get("fixed"), diff.get("added"), diff.get("removed")) == (["b"], ["c"], ["e"], ["d"])
    assert abs(diff.get("pass_rate_delta", 9)) < 1e-9 and diff.get("ok") is False
    better = compare(report(0.5, [("a", True), ("b", True), ("c", False), ("d", False)]),
                     report(0.75, [("a", True), ("b", False), ("c", True), ("d", True)])) or {}
    assert better.get("regressions") == ["b"] and abs(better.get("pass_rate_delta", 9) - 0.25) < 1e-9 and better.get("ok") is False
    same = compare(base, report(1.0, [("a", True), ("b", True), ("c", True), ("d", True)])) or {}
    assert same.get("regressions") == [] and same.get("fixed") == ["c"] and same.get("ok") is True
    dropped = compare(report(0.5, [("a", True), ("d", False)]), report(1.0, [("a", True)])) or {}
    assert dropped.get("regressions") == [] and dropped.get("removed") == ["d"] and dropped.get("ok") is False


def test_e7_repeated_runs_expose_flaky_cases_and_a_case_passes_only_if_every_run_does():
    outputs = {"q": ["yes", "yes", "no"], "r": ["yes"] * 3, "s": ["no"] * 3}
    calls = []
    counts = {}

    def model(text):
        n = counts.get(text, 0)
        counts[text] = n + 1
        calls.append(text)
        return outputs[text][n]

    cases = [{"id": n, "input": n, "check": {"type": "exact", "expected": "yes"}} for n in ("q", "r", "s")]
    report_ = run_eval(cases, model, repeats=3) or {}
    assert len(calls) == 9
    assert [(r["id"], r["passed"], r["reason"], r["flaky"]) for r in report_["results"]] == [
        ("q", False, "mismatch", True), ("r", True, "ok", False), ("s", False, "mismatch", False)]
    assert report_.get("flaky") == ["q"] and report_.get("passed") == 1
    single = run_eval(cases[:1], lambda t: "yes") or {}
    assert single["results"][0]["flaky"] is False and single["flaky"] == []
