import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from rollout import TARGET, Case, Request, gate, migrate_request, retirement_status, rollout_step


def base(**fail):
    """Ten cases: billing a1, a2 (must pass), refund b1 (must pass) b2 b3, faq c1 to c5. By default b3 is lost and c4 is gained."""
    rows = [Case("a1", "billing", True, True, True, 4, 5, 900), Case("a2", "billing", True, True, True, 4, 5, 1000),
            Case("b1", "refund", True, True, True, 6, 7, 1500), Case("b2", "refund", False, True, True, 6, 7, 1600), Case("b3", "refund", False, True, False, 6, 7, 1700)]
    rows += [Case(f"c{i}", "faq", False, i != 4 and i != 5, i != 5, 2, 2, 550 + 50 * i) for i in range(1, 6)]
    return [c._replace(new_ok=fail.get(c.id, c.new_ok)) for c in rows]


def fixed():
    return base(b3=True)


def verdict(cases, protected, max_cost_up=20, max_p95=2000):
    result = gate(cases, protected, max_cost_up, max_p95)
    assert isinstance(result, dict), "gate returned nothing"
    return result


def lines(result):
    assert isinstance(result, list), "a list was expected"
    return result


def test_m1_a_change_that_regresses_nothing_and_stays_inside_its_limits_gets_a_go_with_no_reasons():
    assert verdict(fixed(), {"refund"}) == {"decision": "go", "reasons": []}


def test_e1_a_must_pass_case_that_fails_blocks_the_change_and_the_ids_are_listed_in_order():
    assert verdict(base(b3=True, a1=False), set())["reasons"] == ["must-pass failed: a1"]
    assert verdict(base(b3=True, a2=False, b1=False), set())["reasons"][0] == "must-pass failed: a2, b1"


def test_e2_a_protected_segment_that_lost_answers_blocks_the_change_even_when_gains_elsewhere_match_the_losses():
    assert verdict(base(), {"refund"})["reasons"] == ["protected segment lost answers: refund"]
    assert verdict(base(), set()) == {"decision": "go", "reasons": []}


def test_e3_more_losses_than_gains_blocks_the_change_and_both_counts_are_named():
    assert verdict(base(b3=True, c1=False, c2=False), set()) == {"decision": "no-go", "reasons": ["net loss: lost 2, gained 1"]}


def test_e4_a_cost_rise_over_the_limit_blocks_the_change_and_a_rise_exactly_at_the_limit_does_not():
    assert verdict(fixed(), set(), 13)["decision"] == "go"
    assert verdict(fixed(), set(), 12)["reasons"] == ["cost up 13% over the 12% limit"]
    cheaper = [c._replace(new_cost=1) for c in fixed()]
    assert verdict(cheaper, set(), 0)["decision"] == "go"


def test_e5_the_tail_is_the_nearest_rank_95th_percentile_and_a_single_slow_case_does_not_block():
    rows = [Case(f"c{i}", "faq", False, True, True, 1, 1, 9000 if i == 40 else 1000) for i in range(1, 41)]
    assert verdict(rows, set(), 20, 2000) == {"decision": "go", "reasons": []}
    slow = [r._replace(new_ms=3000) if i < 3 else r for i, r in enumerate(rows)]
    assert verdict(slow, set(), 20, 2000)["reasons"] == ["p95 latency 3000 ms over the 2000 ms limit"]


def test_e6_a_roll_out_advances_when_healthy_holds_with_too_few_requests_and_rolls_back_to_zero_when_errors_pass_the_limit():
    assert rollout_step(1, 2000, 6, 1000, 5) == "advance to 5"
    assert rollout_step(5, 300, 0, 1000, 5) == "hold at 5"
    assert rollout_step(5, 300, 300, 1000, 5) == "hold at 5"
    assert rollout_step(25, 50000, 400, 1000, 5) == "rollback to 0"
    assert rollout_step(25, 1000, 5, 1000, 5) == "advance to 100"
    assert rollout_step(100, 50000, 10, 1000, 5) == "complete"


def test_e7_the_retirement_calendar_counts_days_ranks_the_nearest_first_and_names_the_level():
    models = [("b", "2026-11-30", False), ("a", "2026-10-18", True), ("c", "2026-08-05", False), ("d", "2027-01-01", False), ("e", "2026-10-19", False)]
    assert lines(retirement_status(models, "2026-10-04")) == ["c: -60 days, retired", "a: 14 days, urgent (tentative)", "e: 15 days, migrate now", "b: 57 days, migrate now", "d: 89 days, watch"]
    assert lines(retirement_status([("z", "2026-12-03", False)], "2026-10-04")) == ["z: 60 days, migrate now"]
    assert lines(retirement_status([("z", "2026-12-04", False)], "2026-10-04")) == ["z: 61 days, watch"]


def test_e8_migration_removes_the_settings_the_new_model_refuses_and_names_each_change():
    old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, 40, "disabled", "any", False, True)
    result = migrate_request(old)
    assert isinstance(result, tuple), "migrate_request returned nothing"
    new, changes = result
    assert new == Request(TARGET, None, None, None, "between_tools", "auto", True, False)
    assert changes == ["model set to claude-sonnet-5-5", "removed temperature", "removed top_p", "removed top_k", "thinking disabled replaced by between_tools",
                       "forced tool choice replaced by auto with strict tools", "assistant prefill removed; state the format in the instructions"]
    clean = Request(TARGET, None, None, None, "adaptive", "auto", False, False)
    assert migrate_request(clean) == (clean, [])
    assert migrate_request(clean._replace(thinking="budget", tool_choice="tool"))[0].thinking == "adaptive"
