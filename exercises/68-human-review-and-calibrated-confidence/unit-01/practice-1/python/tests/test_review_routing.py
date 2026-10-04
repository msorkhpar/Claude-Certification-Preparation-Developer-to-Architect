import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
import review_routing as _solution


def _got(fn):
    def call(*args, **kwargs):
        value = fn(*args, **kwargs)
        assert value is not None, f"{fn.__name__} returned nothing"
        return value

    return call


accuracy_by, can_automate, stratified_sample, route, checkpoint = (_got(getattr(_solution, n)) for n in ("accuracy_by", "can_automate", "stratified_sample", "route", "checkpoint"))
calibrate_threshold = _solution.calibrate_threshold


def recs(doc_type, field, right, wrong):
    return [{"doc_type": doc_type, "field": field, "correct": True}] * right + [{"doc_type": doc_type, "field": field, "correct": False}] * wrong


def seg(rows, name):
    return next(r for r in rows if r["segment"] == name)


def test_m1_accuracy_is_reported_per_document_type_and_field_next_to_the_overall_figure():
    rows = accuracy_by(recs("invoice", "total", 90, 0) + recs("receipt", "date", 8, 2))
    assert [r["segment"] for r in rows] == ["overall", "invoice/total", "receipt/date"]
    assert rows[0] == {"segment": "overall", "correct": 98, "total": 100, "percent": 98}
    assert rows[2] == {"segment": "receipt/date", "correct": 8, "total": 10, "percent": 80}


def test_e1_a_weak_segment_is_hidden_by_a_high_overall_figure_and_found_by_the_breakdown():
    rows = accuracy_by(recs("invoice", "total", 970, 10) + recs("handwritten", "total", 8, 12))
    assert seg(rows, "overall")["percent"] == 98 and seg(rows, "handwritten/total")["percent"] == 40 and seg(rows, "invoice/total")["percent"] == 99
    assert accuracy_by([])[0]["percent"] == 0


def test_e2_automation_needs_every_segment_to_pass_and_enough_samples_in_each():
    records = recs("invoice", "total", 97, 3) + recs("receipt", "date", 50, 0) + recs("handwritten", "total", 10, 0)
    result = can_automate(records, 95, 30)
    assert result == {"automate": False, "failing": [], "undersampled": ["handwritten/total"]}
    assert can_automate(recs("invoice", "total", 97, 3) + recs("receipt", "date", 45, 5), 95, 30) == {"automate": False, "failing": ["receipt/date"], "undersampled": []}
    assert can_automate(recs("invoice", "total", 97, 3) + recs("receipt", "date", 50, 0), 95, 30) == {"automate": True, "failing": [], "undersampled": []}
    assert can_automate([], 95, 30)["automate"] is False
    assert can_automate(recs("invoice", "total", 97, 3) + recs("receipt", "date", 20, 10), 95, 30) == {"automate": False, "failing": ["receipt/date"], "undersampled": []}
    assert can_automate(recs("invoice", "total", 95, 5), 95, 30) == {"automate": True, "failing": [], "undersampled": []}


def test_e3_the_threshold_is_the_lowest_confidence_whose_accepted_items_meet_the_target_precision():
    labeled = [(95, True), (90, True), (85, True), (80, False), (70, False), (60, False)]
    assert calibrate_threshold(labeled, 90) == 85
    assert calibrate_threshold(labeled, 70) == 80
    assert calibrate_threshold(labeled, 50) == 60


def test_e4_no_threshold_exists_when_no_confidence_level_meets_the_target():
    assert calibrate_threshold([(95, False), (90, False)], 90) is None
    assert calibrate_threshold([], 90) is None
    assert calibrate_threshold([(95, True), (90, False)], 100) == 95


def test_e5_the_stratified_sample_takes_the_best_ranked_items_of_every_stratum():
    items = [{"id": "a1", "stratum": "invoice", "rank": 5}, {"id": "a2", "stratum": "invoice", "rank": 1}, {"id": "a3", "stratum": "invoice", "rank": 3},
             {"id": "b1", "stratum": "receipt", "rank": 9}, {"id": "c1", "stratum": "handwritten", "rank": 2}, {"id": "c2", "stratum": "handwritten", "rank": 2}]
    assert stratified_sample(items, 2) == ["a2", "a3", "b1", "c1", "c2"]
    assert stratified_sample(items, 1) == ["a2", "b1", "c1"]
    assert stratified_sample([], 3) == []


def test_e6_low_confidence_and_conflicts_go_to_review_with_the_weakest_first():
    rows = [{"id": "x1", "confidence": 90, "conflict": False}, {"id": "x2", "confidence": 60, "conflict": False}, {"id": "x3", "confidence": 99, "conflict": True}, {"id": "x4", "confidence": 79, "conflict": False}, {"id": "x5", "confidence": 80, "conflict": False}]
    assert route(rows, 80, 10) == {"review": ["x3", "x2", "x4"], "backlog": [], "auto": ["x1", "x5"]}


def test_e7_review_capacity_is_respected_and_the_rest_wait_in_a_backlog():
    rows = [{"id": f"r{i}", "confidence": 50 + i, "conflict": False} for i in range(5)] + [{"id": "ok", "confidence": 99, "conflict": False}]
    assert route(rows, 80, 2) == {"review": ["r0", "r1"], "backlog": ["r2", "r3", "r4"], "auto": ["ok"]}
    assert route(rows, 80, 0)["review"] == []


def test_e8_an_irreversible_action_needs_a_person_whatever_the_confidence():
    assert checkpoint("delete_records", 1) == "human" and checkpoint("send_payment", 5) == "human"
    assert checkpoint("update_label", 50) == "auto" and checkpoint("update_label", 5000) == "human"
    assert checkpoint("update_label", 1000) == "auto" and checkpoint("update_label", 300, limit=200) == "human"
