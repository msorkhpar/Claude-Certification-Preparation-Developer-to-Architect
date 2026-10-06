import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from review_spec import build_review_prompt, next_step
from review_spec import category_report as _category_report


def category_report(*args, **kw):
    result = _category_report(*args, **kw)
    assert isinstance(result, dict), "category_report returned nothing"
    return result


def crit(**over):
    c = {"id": "bug", "report": "A comment whose claimed behaviour contradicts what the code does.", "skip": "Minor style, naming and patterns the codebase already uses.",
         "severity": {"high": "A null dereference on a request path, such as user.profile.name when user may be None.", "low": "A misleading variable name."}}
    c.update(over)
    return c


REPORT = {"verdict": "report", "category": "bug", "code": "total = price * qty  # sum of the line items", "reason": "The comment says the line sums items, the code multiplies one price by a quantity."}
SKIP = {"verdict": "skip", "code": "for i in range(n):  # loop", "reason": "The comment is terse and accurate; at most a style matter."}
SPEC = {"criteria": [crit()], "examples": [REPORT, SKIP]}


def refused(spec, diff="+ x = 1"):
    try:
        build_review_prompt(spec, diff)
    except ValueError as error:
        return str(error)
    raise AssertionError(f"the specification was accepted: {spec}")


def built(spec):
    result = build_review_prompt(spec, "+ x = 1")
    assert isinstance(result, str), "build_review_prompt returned nothing"
    return result


def test_m1_the_prompt_puts_criteria_first_then_examples_then_the_diff_last():
    expected = "\n".join([
        "<criteria>",
        '<criterion id="bug">',
        "Report: A comment whose claimed behaviour contradicts what the code does.",
        "Skip: Minor style, naming and patterns the codebase already uses.",
        "Severity high: A null dereference on a request path, such as user.profile.name when user may be None.",
        "Severity low: A misleading variable name.",
        "</criterion>",
        "</criteria>",
        "<examples>",
        '<example verdict="report" category="bug">',
        "<code>total = price * qty  # sum of the line items</code>",
        "<reason>The comment says the line sums items, the code multiplies one price by a quantity.</reason>",
        "</example>",
        '<example verdict="skip">',
        "<code>for i in range(n):  # loop</code>",
        "<reason>The comment is terse and accurate; at most a style matter.</reason>",
        "</example>",
        "</examples>",
        "<diff>",
        "+ x = 1",
        "</diff>",
    ])
    assert built(SPEC) == expected


def test_e1_vague_criteria_are_refused_in_both_the_report_and_the_skip_text():
    for phrase in ("Be conservative and flag only what matters.", "Only report high-confidence findings.", "Report it when you are sure.", "Flag only important problems.", "Use your judgment about what matters."):
        for key in ("report", "skip"):
            message = refused({**SPEC, "criteria": [crit(**{key: phrase})]})
            assert "vague" in message, f"{key}: {phrase!r} was refused for another reason: {message}"


def test_e2_a_criterion_needs_report_skip_and_a_concrete_severity_example_for_high_and_low():
    refused({**SPEC, "criteria": []})
    for over in ({"report": ""}, {"skip": "  "}, {"skip": None}, {"severity": {"high": "A null dereference."}}, {"severity": {"low": "A misleading name."}}, {"severity": {"high": "x", "low": " "}}, {"severity": None}):
        refused({**SPEC, "criteria": [crit(**over)]})
    assert "<criterion id=\"bug\">" in built({**SPEC, "criteria": [crit()]})


def test_e3_two_to_four_examples_with_a_report_and_a_skip_each_carrying_a_reason():
    refused({**SPEC, "examples": [REPORT]})
    refused({**SPEC, "examples": [REPORT, SKIP, REPORT, SKIP, REPORT]})
    refused({**SPEC, "examples": [REPORT, dict(REPORT)]})
    refused({**SPEC, "examples": [SKIP, dict(SKIP)]})
    refused({**SPEC, "examples": [REPORT, {**SKIP, "verdict": "maybe"}]})
    refused({**SPEC, "examples": [REPORT, {**SKIP, "reason": " "}]})
    refused({**SPEC, "examples": [{**REPORT, "category": "performance"}, SKIP]})
    for count in (2, 3, 4):
        assert built({**SPEC, "examples": ([REPORT, SKIP, REPORT, SKIP])[:count]}).count("<example ") == count


def findings(category, accepted, dismissed, pattern="p"):
    return [{"category": category, "verdict": "accepted", "detected_pattern": pattern}] * accepted + [{"category": category, "verdict": "dismissed", "detected_pattern": pattern}] * dismissed


def test_e4_a_category_with_enough_reviews_and_low_precision_is_disabled():
    data = findings("bug", 8, 2) + findings("style", 2, 4) + findings("naming", 0, 4) + findings("docs", 3, 3)
    report = category_report(data)
    assert report["disable"] == ["style"], report["disable"]
    assert {k: (v["reviewed"], v["precision"], v["disable"]) for k, v in report["categories"].items()} == {
        "bug": (10, 0.8, False), "style": (6, 0.33, True), "naming": (4, 0.0, False), "docs": (6, 0.5, False)}
    assert category_report(data, min_reviewed=4)["disable"] == ["naming", "style"]
    assert category_report(data, min_precision=0.9)["disable"] == ["bug", "docs", "style"]
    assert category_report([]) == {"categories": {}, "disable": []}


def test_e5_the_most_dismissed_patterns_are_listed_by_count_then_name_and_capped_at_three():
    data = (findings("style", 0, 3, "line-length") + findings("style", 0, 2, "import-order") + findings("style", 0, 1, "quote-style") + findings("style", 0, 1, "brace-style")
            + findings("style", 5, 0, "accepted-only"))
    assert category_report(data)["categories"]["style"]["top_dismissed"] == [["line-length", 3], ["import-order", 2], ["brace-style", 1]]
    assert category_report(findings("bug", 3, 0))["categories"]["bug"]["top_dismissed"] == []


REQUEST = {"repo": "api", "branch": "", "reviewer": None}
REQUIRED = ["repo", "branch", "reviewer"]


def step(request, defaults, attended):
    result = next_step(request, REQUIRED, defaults, attended)
    assert isinstance(result, dict), "next_step returned nothing"
    return result


def test_e6_an_attended_run_asks_only_what_it_cannot_assume_and_states_its_assumptions():
    assert step(REQUEST, {"branch": "main"}, True) == {"action": "ask", "ask": ["reviewer"], "assumptions": {"branch": "main"}}
    assert step({"repo": "api", "branch": " ", "reviewer": "ana"}, {"branch": "main"}, True) == {"action": "proceed", "ask": [], "assumptions": {"branch": "main"}}
    assert step({"repo": "api", "branch": "dev", "reviewer": "ana"}, {"branch": "main"}, True) == {"action": "proceed", "ask": [], "assumptions": {}}
    assert step({}, {}, True) == {"action": "ask", "ask": ["repo", "branch", "reviewer"], "assumptions": {}}


def test_e7_an_unattended_run_never_asks_it_states_assumptions_or_stops():
    assert step(REQUEST, {"branch": "main"}, False) == {"action": "stop", "ask": [], "assumptions": {"branch": "main"}}
    assert step({"repo": "api", "branch": "", "reviewer": "ana"}, {"branch": "main"}, False) == {"action": "proceed", "ask": [], "assumptions": {"branch": "main"}}
    assert step({"repo": "api", "branch": "dev", "reviewer": "ana"}, {}, False) == {"action": "proceed", "ask": [], "assumptions": {}}
