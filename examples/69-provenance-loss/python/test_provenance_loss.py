from provenance_loss import FINDINGS, ledger_lines, plain_summary, sources_named, status


def rows(*items):
    return [("c", value, source, date) for value, source, date in items]


def test_the_status_tells_agreement_from_conflict_and_change():
    assert status(rows(("1", "A", "2024-01-01"), ("1", "B", "2024-02-01"))) == "agreed"
    assert status(rows(("1", "A", "2024-01-01"), ("2", "B", "2024-01-01"))) == "conflict"
    assert status(rows(("1", "A", "2022-01-01"), ("2", "B", "2024-01-01"))) == "changed"


def test_the_plain_summary_keeps_one_value_and_no_sources():
    summary = plain_summary(FINDINGS)
    assert "market growth 2024: 12%" in summary and "9%" not in summary.split("growth forecast")[0]
    assert sources_named(summary, FINDINGS) == 0


def test_the_ledger_keeps_both_sides_of_a_conflict_with_their_sources():
    line = [l for l in ledger_lines(FINDINGS).splitlines() if l.startswith("market growth 2024")][0]
    assert line == "market growth 2024 [conflict]: 12% (Firm A report, 2024-05-01); 9% (Firm B survey, 2024-05-01)"


def test_a_change_over_time_lists_the_older_value_first():
    line = [l for l in ledger_lines(FINDINGS).splitlines() if l.startswith("growth forecast")][0]
    assert line == "growth forecast [changed]: 7% (Firm C yearbook, 2022-04-01); 9% (Firm B survey, 2024-05-01)"


def test_the_ledger_names_every_source():
    assert sources_named(ledger_lines(FINDINGS), FINDINGS) == 5
