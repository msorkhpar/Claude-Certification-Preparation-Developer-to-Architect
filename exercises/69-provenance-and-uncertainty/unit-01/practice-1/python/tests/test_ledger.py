import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
import ledger as _solution


def _got(fn):
    def call(*args, **kwargs):
        value = fn(*args, **kwargs)
        assert value is not None, f"{fn.__name__} returned nothing"
        return value

    return call


check_finding, merge, coverage_note, render = (_got(getattr(_solution, n)) for n in ("check_finding", "merge", "coverage_note", "render"))


def refused(fn, *args):
    try:
        fn(*args)
    except ValueError:
        return True
    return False


def f(claim, value, source, date):
    return {"claim": claim, "value": value, "source": source, "date": date}


def entry(claim, status, *values):
    return {"claim": claim, "status": status, "values": [{"value": v, "sources": [{"source": s, "date": d} for s, d in srcs]} for v, srcs in values]}


def test_m1_findings_are_merged_per_claim_with_every_source_kept_once():
    merged = merge([
        f("revenue 2023", "4.1B", "Annual report", "2024-02-01"),
        f("headcount", "910", "Press release", "2024-03-01"),
        f("revenue 2023", "4.1B", "Press release", "2024-02-03"),
        f("revenue 2023", "4.1B", "Annual report", "2024-02-01"),
    ])
    assert merged == [
        entry("revenue 2023", "agreed", ("4.1B", [("Annual report", "2024-02-01"), ("Press release", "2024-02-03")])),
        entry("headcount", "agreed", ("910", [("Press release", "2024-03-01")])),
    ]
    assert merge([]) == []


def test_e1_a_finding_without_a_source_or_a_date_is_refused():
    assert check_finding(f("c", "v", "", " ")) == ["source", "date"]
    assert check_finding({"value": "v", "source": "s", "date": "2024-01-01"}) == ["claim"]
    assert check_finding(f("c", "v", "s", "2024-01-01")) == []
    assert refused(merge, [f("c", "v", "s", "2024-01-01"), f("c", "w", "s", "")])
    assert not refused(merge, [f("c", "v", "s", "2024-01-01")])


def test_e2_two_values_from_the_same_date_are_a_conflict_that_keeps_both():
    merged = merge([f("market size", "12%", "Firm A report", "2024-05-01"), f("market size", "9%", "Firm B survey", "2024-05-01")])
    assert merged == [entry("market size", "conflict", ("12%", [("Firm A report", "2024-05-01")]), ("9%", [("Firm B survey", "2024-05-01")]))]


def test_e3_two_values_from_different_dates_are_a_change_not_a_conflict():
    merged = merge([f("market size", "12%", "Firm A report", "2024-05-01"), f("market size", "9%", "Firm B survey", "2022-05-01")])
    assert merged == [entry("market size", "changed", ("9%", [("Firm B survey", "2022-05-01")]), ("12%", [("Firm A report", "2024-05-01")]))]


def test_e4_the_coverage_note_separates_what_is_well_supported_from_what_is_not():
    merged = merge([
        f("a", "1", "S1", "2024-01-01"), f("a", "1", "S2", "2024-01-02"),
        f("b", "2", "S1", "2024-01-01"), f("b", "2", "S1", "2024-02-01"),
        f("c", "3", "S1", "2024-01-01"), f("c", "4", "S2", "2022-01-01"),
        f("d", "5", "S1", "2024-01-01"), f("d", "6", "S2", "2024-01-01"),
    ])
    note = coverage_note(["a", "b", "c", "d"], merged, {})
    assert (note["well_supported"], note["single_source"], note["changed"], note["contested"], note["gaps"]) == (["a"], ["b"], ["c"], ["d"], [])


def test_e5_a_planned_claim_with_no_finding_is_a_gap_with_its_reason():
    merged = merge([f("a", "1", "S1", "2024-01-01"), f("z", "9", "S1", "2024-01-01")])
    note = coverage_note(["q", "a", "r"], merged, {"q": "the registry timed out"})
    assert note["gaps"] == [{"claim": "q", "reason": "the registry timed out"}, {"claim": "r", "reason": "no source found"}]
    assert note["single_source"] == ["a", "z"]


def test_e6_financial_data_is_rendered_as_a_table():
    e = entry("revenue 2023", "agreed", ("4.1B", [("Annual report", "2024-02-01"), ("Press release", "2024-02-03")]))
    assert render(e, "financial") == "| Source | Date | Value |\n|---|---|---|\n| Annual report | 2024-02-01 | 4.1B |\n| Press release | 2024-02-03 | 4.1B |"


def test_e7_news_is_rendered_as_prose_that_says_when_sources_disagree():
    agreed = entry("launch", "agreed", ("in May", [("Daily", "2024-05-02")]))
    changed = entry("size", "changed", ("9%", [("B", "2022-05-01")]), ("12%", [("A", "2024-05-01")]))
    conflict = entry("size", "conflict", ("12%", [("A", "2024-05-01")]), ("9%", [("B", "2024-05-01")]))
    assert render(agreed, "news") == "launch: in May (Daily, 2024-05-02)."
    assert render(changed, "news") == "size: 9% (B, 2022-05-01); 12% (A, 2024-05-01). The figures are from different dates."
    assert render(conflict, "news") == "size: 12% (A, 2024-05-01); 9% (B, 2024-05-01). The sources disagree."


def test_e8_technical_findings_are_a_list_and_an_unknown_content_type_is_refused():
    e = entry("rate limit", "agreed", ("100 per minute", [("API docs", "2024-06-01"), ("Changelog", "2024-06-05")]))
    assert render(e, "technical") == "rate limit:\n- 100 per minute (API docs, 2024-06-01)\n- 100 per minute (Changelog, 2024-06-05)"
    assert render(e, "technical") != render(e, "financial")
    assert refused(render, e, "poem")
