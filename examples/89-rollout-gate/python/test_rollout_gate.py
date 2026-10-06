from rollout_gate import *


def test_retirement_status_counts_days_and_ranks_by_urgency():
    models = [("b", "2026-11-30", False), ("a", "2026-10-15", True), ("c", "2026-08-05", False)]
    assert retirement_status(models, "2026-10-04") == ["c: -60 days, retired", "a: 11 days, urgent (tentative)", "b: 57 days, migrate now"]
    assert retirement_status([("x", "2027-01-01", False)], "2026-10-04") == ["x: 89 days, watch"]


def test_migration_removes_what_the_new_model_refuses_and_keeps_the_rest():
    old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, None, "budget", "tool", False, True)
    new, changes = migrate_request(old)
    assert new == Request(TARGET, None, None, None, "adaptive", "auto", True, False)
    assert len(changes) == 6 and changes[0] == "model set to claude-sonnet-5-5"
    clean = Request(TARGET, None, None, None, "adaptive", "auto", True, False)
    assert migrate_request(clean) == (clean, [])


def test_the_gate_is_go_only_when_no_check_fails():
    cases = suite()
    assert gate(cases, set(), 40, 2000) == {"decision": "go", "reasons": []}
    result = gate(cases, {"refund"}, 25, 2000)
    assert result["reasons"] == ["protected segment lost answers: refund", "cost up 35% over the 25% limit"]


def test_a_cost_rise_exactly_at_the_limit_passes():
    assert gate(suite(), set(), 35, 2000)["decision"] == "go"
    assert gate(suite(), set(), 34, 2000)["reasons"] == ["cost up 35% over the 34% limit"]


def test_the_tail_is_the_nearest_rank_95th_percentile():
    assert gate(suite(), set(), 40, 1800)["decision"] == "go"
    assert gate(suite(), set(), 40, 1799)["reasons"] == ["p95 latency 1800 ms over the 1799 ms limit"]


def test_a_must_pass_failure_and_a_net_loss_are_named():
    broken = [c._replace(new_ok=False) if c.id in ("b1", "r1") else c for c in suite()]
    reasons = gate(broken, set(), 40, 2000)["reasons"]
    assert reasons[0] == "must-pass failed: b1, r1"
    assert reasons[1] == "net loss: lost 3, gained 2"


def test_a_rollout_advances_holds_or_rolls_back():
    assert rollout_step(1, 2000, 6, 1000, 5) == "advance to 5"
    assert rollout_step(5, 300, 0, 1000, 5) == "hold at 5"
    assert rollout_step(25, 50000, 400, 1000, 5) == "rollback to 0"
    assert rollout_step(100, 50000, 10, 1000, 5) == "complete"
    assert rollout_step(25, 1000, 5, 1000, 5) == "advance to 100"
