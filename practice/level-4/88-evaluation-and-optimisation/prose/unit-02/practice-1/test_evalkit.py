import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from evalkit import ab_verdict, choose_model, diagnose, percentile, segment_table, shadow_gate

COSTS = {"order status": 1, "refund": 20, "policy": 5, "complaint": 10}


def rows(*groups):
    return [(segment, i < right) for segment, total, right in groups for i in range(total)]


def table(results, costs):
    result = segment_table(results, costs)
    assert isinstance(result, list), "segment_table returned nothing"
    return result


def gate(pairs, protected):
    result = shadow_gate(pairs, protected)
    assert isinstance(result, dict), "shadow_gate returned nothing"
    return result


def test_m1_a_segment_table_reports_accuracy_and_error_cost_with_the_costliest_segment_first():
    results = rows(("order status", 30, 30), ("refund", 8, 5), ("policy", 10, 9), ("complaint", 4, 4))
    assert table(results, COSTS) == [("refund", 8, 5, 63, 60), ("policy", 10, 9, 90, 5), ("complaint", 4, 4, 100, 0), ("order status", 30, 30, 100, 0)]


def test_e1_a_segment_with_no_cost_entry_costs_one_per_error_and_no_results_give_an_empty_table():
    assert table(rows(("odd", 3, 1)), COSTS) == [("odd", 3, 1, 33, 2)]
    assert table([], COSTS) == []
    assert table(rows(("b", 2, 1), ("a", 2, 1)), {"a": 1, "b": 1}) == [("a", 2, 1, 50, 1), ("b", 2, 1, 50, 1)]


def test_e2_a_percentile_uses_the_nearest_rank_and_does_not_need_sorted_input():
    values = [4800, 800, 1000, 900]
    assert percentile(values, 50) == 900
    assert percentile(values, 95) == 4800
    assert percentile(values, 100) == 4800
    assert percentile(values, 1) == 800
    assert percentile([7], 50) == 7
    assert percentile([], 50) == 0


def test_e3_a_test_with_fewer_cases_than_the_minimum_in_either_arm_decides_nothing():
    assert ab_verdict(150, 199, 190, 400) == "too few cases"
    assert ab_verdict(150, 400, 190, 199) == "too few cases"
    assert ab_verdict(100, 200, 160, 200) == "new is better"
    assert ab_verdict(10, 100, 19, 100, 50) == "no clear difference"
    assert ab_verdict(10, 100, 19, 100) == "too few cases"


def test_e4_a_difference_is_called_only_when_it_clears_the_95_percent_bar_and_the_better_side_is_named():
    assert ab_verdict(410, 500, 438, 500) == "new is better"
    assert ab_verdict(438, 500, 410, 500) == "old is better"
    assert ab_verdict(410, 500, 431, 500) == "no clear difference"
    assert ab_verdict(0, 300, 0, 300) == "no clear difference"
    assert ab_verdict(300, 300, 300, 300) == "no clear difference"


def test_e5_a_shadow_run_is_held_for_a_regression_in_a_protected_segment_or_for_more_losses_than_gains():
    pairs = [("refund", False, True)] * 2 + [("policy", True, False), ("complaint", True, False)] + [("policy", True, True)] * 5
    assert gate(pairs, {"refund", "complaint"}) == {"decision": "hold", "lost": 2, "gained": 2, "blocked": ["complaint"]}
    assert gate(pairs, {"refund"}) == {"decision": "ship", "lost": 2, "gained": 2, "blocked": []}
    worse = [("policy", True, False)] * 3 + [("policy", False, True)] * 2
    assert gate(worse, set()) == {"decision": "hold", "lost": 3, "gained": 2, "blocked": []}


def test_e6_diagnosis_checks_the_evidence_then_the_grounding_then_the_format_then_the_stronger_model():
    assert diagnose(False, False, False, False) == "retrieval or data"
    assert diagnose(True, False, False, False) == "ungrounded answer"
    assert diagnose(True, True, False, False) == "format instructions"
    assert diagnose(True, True, True, False) == "prompt or task"
    assert diagnose(True, True, True, True) == "model mismatch"


def test_e7_model_choice_takes_the_cheapest_option_that_meets_the_accuracy_floor_and_the_latency_limit():
    options = [("small", 84, 900, 1), ("medium", 91, 1800, 3), ("large", 95, 4200, 9)]
    assert choose_model(options, 90, 2000) == "medium"
    assert choose_model(options, 94, 2000) == "none"
    assert choose_model(options, 91, 1800) == "medium"
    assert choose_model(options, 80, 5000) == "small"
    assert choose_model([("b", 90, 100, 2), ("a", 90, 100, 2)], 90, 100) == "a"
