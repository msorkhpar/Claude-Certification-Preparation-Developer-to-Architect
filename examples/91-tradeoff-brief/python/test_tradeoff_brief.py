from tradeoff_brief import *


def test_percent_rounds_halves_up_and_survives_no_cases():
    assert pct(1, 2) == 50 and pct(1, 8) == 13 and pct(1, 3) == 33 and pct(2, 3) == 67
    assert pct(0, 0) == 0


def test_break_even_is_where_the_expected_error_cost_equals_the_check():
    assert break_even(250, 5) == 98
    assert break_even(60, 5) == 91
    assert break_even(12, 5) == 58


def test_a_service_level_is_met_exactly_at_its_edge():
    latency = Sla("p95 latency", 2000, "max", "ms")
    assert sla_line(latency, 2000).endswith("met")
    assert sla_line(latency, 2001) == "p95 latency: 2001 ms against a limit of 2000 ms: missed by 1 ms"
    floor = Sla("availability", 995, "min", "per mille")
    assert sla_line(floor, 995).endswith("met")
    assert sla_line(floor, 994).endswith("missed by 1 per mille")


def test_the_report_lists_the_costliest_segment_first_and_checks_below_the_break_even():
    lines = segment_report([Segment("status", 98, 100, 12), Segment("credit", 63, 100, 250)], 5)
    assert lines[0].startswith("credit: 63 percent right") and lines[0].endswith("reviewed (break-even 98)")
    assert lines[1] == "status: 98 percent right, error cost 12, auto (break-even 58)"


def test_the_two_audiences_get_the_same_facts_in_different_words():
    weakest = Segment("credit", 63, 100, 250)
    sponsor = brief("sponsor", "Routing", 80000, 315000, weakest, "approve the pilot")
    engineer = brief("engineer", "Routing", 80000, 315000, weakest, "approve the pilot")
    assert "a saving of 235,000" in sponsor and "Decision asked: approve the pilot." in sponsor
    assert "saving=235000" in engineer and "decision" not in engineer.lower()
