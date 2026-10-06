import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from run_audit import audit

POLICY = {"target": 90, "min_n": 3, "gap": 5}


def run(kind="typed", status="valid", correct=True, invented=False, wasted=False, sum_ok=True):
    return {"id": "d", "kind": kind, "status": status, "correct": correct, "invented": invented, "retried_absent": wasted, "sum_ok": sum_ok}


def many(n, **fields):
    return [run(**fields) for _ in range(n)]


def seg(kind, n, correct, percent, automate):
    return {"kind": kind, "n": n, "correct": correct, "percent": percent, "automate": automate}


def test_m1_a_mixed_run_gets_every_count_both_accuracies_the_segments_and_the_first_fix():
    runs = many(3, kind="typed") + [run("scanned"), run("scanned", "needs_review", False),
                                    run("handwritten", "failed", False, invented=True), run("handwritten", "needs_review", False, wasted=True)]
    assert audit(runs, POLICY) == {
        "n": 7, "valid": 4, "needs_review": 2, "failed": 1, "accuracy_all": 57, "accuracy_validated": 100, "meets_target": False,
        "segments": [seg("handwritten", 2, 0, 0, False), seg("scanned", 2, 1, 50, False), seg("typed", 3, 3, 100, True)],
        "invented": 1, "wasted_retries": 1, "unchecked_totals": 0, "overstated": True, "first_fix": "make_fields_nullable"}


def test_e1_an_empty_run_has_zero_figures_no_segments_and_never_meets_the_target():
    assert audit([], POLICY) == {
        "n": 0, "valid": 0, "needs_review": 0, "failed": 0, "accuracy_all": 0, "accuracy_validated": 0, "meets_target": False,
        "segments": [], "invented": 0, "wasted_retries": 0, "unchecked_totals": 0, "overstated": False, "first_fix": "none"}


def test_e2_the_run_meets_the_target_at_exactly_the_target_and_not_below_it():
    at = many(9) + many(1, correct=False)
    below = many(8) + many(2, correct=False)
    assert audit(at, POLICY)["meets_target"] is True and audit(at, POLICY)["accuracy_all"] == 90
    assert audit(below, POLICY)["meets_target"] is False and audit(below, POLICY)["accuracy_all"] == 80


def test_e3_a_kind_needs_at_least_the_minimum_number_of_documents_to_be_automated():
    report = audit(many(3, kind="typed") + many(2, kind="scanned"), POLICY)
    assert report["segments"] == [seg("scanned", 2, 2, 100, False), seg("typed", 3, 3, 100, True)]


def test_e4_a_kind_is_automated_at_exactly_the_target_accuracy_and_not_below_it():
    report = audit(many(9, kind="typed") + many(1, kind="typed", correct=False) + many(17, kind="scanned") + many(2, kind="scanned", correct=False), POLICY)
    assert report["segments"] == [seg("scanned", 19, 17, 89, False), seg("typed", 10, 9, 90, True)]


def test_e5_the_figure_is_overstated_only_when_the_validated_accuracy_exceeds_the_all_document_accuracy_by_more_than_the_gap():
    at_gap = audit(many(19) + many(1, status="failed", correct=False), POLICY)
    assert (at_gap["accuracy_all"], at_gap["accuracy_validated"], at_gap["overstated"], at_gap["first_fix"]) == (95, 100, False, "none")
    over = audit(many(16) + many(1, status="failed", correct=False), POLICY)
    assert (over["accuracy_all"], over["accuracy_validated"], over["overstated"], over["first_fix"]) == (94, 100, True, "measure_all_documents")


def test_e6_each_shape_counts_documents_once_and_an_unchecked_total_counts_only_documents_accepted_as_valid():
    runs = [run(sum_ok=False), run(status="needs_review", correct=False, sum_ok=False), run(status="failed", correct=False, sum_ok=False),
            run(invented=True, wasted=True)]
    report = audit(runs, POLICY)
    assert (report["invented"], report["wasted_retries"], report["unchecked_totals"]) == (1, 1, 1)


def test_e7_the_first_fix_follows_the_order_of_what_costs_most():
    assert audit([run(invented=True, wasted=True, sum_ok=False)], POLICY)["first_fix"] == "make_fields_nullable"
    assert audit([run(wasted=True, sum_ok=False)], POLICY)["first_fix"] == "stop_retrying_absent"
    assert audit([run(sum_ok=False)], POLICY)["first_fix"] == "add_semantic_checks"
    assert audit(many(16) + many(1, status="failed", correct=False), POLICY)["first_fix"] == "measure_all_documents"
    assert audit(many(8) + many(2, correct=False), POLICY)["first_fix"] == "improve_weak_segments"
    assert audit(many(5), POLICY)["first_fix"] == "none"


def test_e8_percentages_are_whole_numbers_rounded_half_up():
    report = audit(many(1) + many(7, correct=False), POLICY)
    assert (report["accuracy_all"], report["accuracy_validated"], report["segments"][0]["percent"]) == (13, 13, 13)
    assert audit(many(2) + many(1, correct=False), POLICY)["accuracy_all"] == 67
    assert audit(many(1) + many(2, correct=False), POLICY)["accuracy_all"] == 33
