from criteria_lint import GOOD_CRITERION, VAGUE_CRITERION, lint_criterion, lint_examples, trust


def test_a_criterion_that_names_no_pattern_is_linted_and_a_concrete_one_is_not():
    assert lint_criterion(VAGUE_CRITERION) == ["vague-report", "no-skip", "no-high-example", "no-low-example"]
    assert lint_criterion(GOOD_CRITERION) == []
    assert lint_criterion({**GOOD_CRITERION, "skip": "Use your judgment."}) == ["vague-skip"]


def test_a_set_of_examples_needs_two_to_four_both_verdicts_and_a_reason_each():
    report, skip = {"verdict": "report", "reason": "r"}, {"verdict": "skip", "reason": "s"}
    assert lint_examples([report, skip]) == []
    assert lint_examples([report]) == ["two-to-four", "both-verdicts"]
    assert lint_examples([report, skip, report, skip, report]) == ["two-to-four"]
    assert lint_examples([report, {"verdict": "report", "reason": ""}]) == ["both-verdicts", "reason-missing"]


def test_a_category_is_switched_off_only_with_enough_reviews_and_a_low_share_accepted():
    verdicts = [("bug", "accepted")] * 9 + [("bug", "dismissed")] + [("style", "accepted")] * 2 + [("style", "dismissed")] * 6 + [("naming", "dismissed")] * 3
    table = trust(verdicts)
    assert table["bug"] == {"reviewed": 10, "accepted": 9, "precision": 0.9, "off": False}
    assert table["style"] == {"reviewed": 8, "accepted": 2, "precision": 0.25, "off": True}
    assert table["naming"]["off"] is False and table["naming"]["precision"] == 0.0
