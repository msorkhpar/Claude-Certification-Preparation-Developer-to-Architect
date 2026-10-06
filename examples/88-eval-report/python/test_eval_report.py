from eval_report import *


def test_the_two_versions_have_the_same_overall_accuracy_and_different_segments():
    rows = build_cases()
    assert len(rows) == 52
    old, new = segment_table(rows, "old"), segment_table(rows, "new")
    assert sum(t[2] for t in old) == sum(t[2] for t in new) == 48
    assert old[0][:5] == ("refund", 8, 5, 63, 60)
    assert new[0][:5] == ("refund", 8, 7, 88, 20)


def test_percentile_uses_the_nearest_rank_and_needs_no_sorted_input():
    values = [4800, 800, 1000, 900]
    assert percentile(values, 50) == 900
    assert percentile(values, 95) == 4800
    assert percentile([], 95) == 0


def test_ab_verdict_names_the_better_side_only_when_it_clears_the_bar():
    assert ab_verdict(410, 500, 438, 500) == "new is better"
    assert ab_verdict(438, 500, 410, 500) == "old is better"
    assert ab_verdict(410, 500, 425, 500) == "no clear difference"
    assert ab_verdict(82, 100, 90, 100) == "too few cases"
    assert ab_verdict(0, 300, 0, 300) == "no clear difference"


def test_shadow_gate_holds_for_a_protected_regression_or_a_net_loss():
    gate = shadow_gate(build_cases(), {"refund", "complaint"})
    assert gate == {"decision": "hold", "lost": 2, "gained": 2, "blocked": ["complaint"]}
    assert shadow_gate(build_cases(), {"refund"})["decision"] == "ship"


def test_diagnose_checks_the_evidence_before_the_prompt_and_the_model_last():
    assert diagnose(False, False, False, False) == "retrieval or data"
    assert diagnose(True, True, False, True) == "format instructions"
    assert diagnose(True, True, True, True) == "model mismatch"


def test_choose_model_takes_the_cheapest_that_meets_both_limits():
    options = [("small", 84, 900, 1), ("medium", 91, 1800, 3), ("large", 95, 4200, 9)]
    assert choose_model(options, 90, 2000) == "medium"
    assert choose_model(options, 94, 2000) == "none"
