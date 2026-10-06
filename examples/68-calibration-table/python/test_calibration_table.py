from calibration_table import accuracy_by_type, calibration_table, make_records, overall_accuracy, precision_above, ratio

SMALL = [("a", 95, True), ("a", 95, True), ("b", 55, False), ("b", 55, True)]


def test_ratio_rounds_half_up_and_survives_an_empty_whole():
    assert [ratio(1, 2), ratio(1, 3), ratio(2, 3), ratio(0, 0)] == [50, 33, 67, 0]


def test_accuracy_is_reported_overall_and_per_type():
    assert overall_accuracy(SMALL) == 75
    assert accuracy_by_type(SMALL) == [("a", 2, 2, 100), ("b", 1, 2, 50)]


def test_the_table_compares_what_a_bucket_claimed_with_how_often_it_was_right():
    assert calibration_table(SMALL) == [("50-59", 2, 55, 50), ("90-100", 2, 95, 100)]


def test_precision_above_counts_what_would_skip_review():
    assert [precision_above(SMALL, 90), precision_above(SMALL, 50), precision_above(SMALL, 99)] == [(2, 100), (4, 75), (0, 0)]


def test_the_generated_records_hide_a_weak_type_behind_the_overall_figure():
    records = make_records()
    by_type = {t: p for t, _, _, p in accuracy_by_type(records)}
    assert len(records) == 200
    assert by_type["handwritten"] < overall_accuracy(records) < by_type["invoice"]
    assert sum(count for _, count, _, _ in calibration_table(records)) == 200
