import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit import audit


def step(tool, ok=True, right=None):
    return {"tool": tool, "ok": ok, **({"right_tool": right} if right else {})}


def session(steps, outcome="resolved", needs_human=False, refund=0, limit=10000):
    return {"id": "s", "steps": steps, "outcome": outcome, "needs_human": needs_human, "refund_cents": refund, "limit_cents": limit}


CLEAN = [step("get_customer"), step("lookup_order"), step("process_refund")]


def test_m1_a_mixed_set_of_sessions_gets_every_rate_and_the_first_fix():
    sessions = [
        session(CLEAN, refund=2000),
        session([step("lookup_order"), step("get_customer"), step("process_refund")]),
        session([step("get_customer")], outcome="escalated", needs_human=True),
        session([step("get_customer")], outcome="escalated"),
        session(CLEAN, needs_human=True),
        session([step("get_customer", right="lookup_order"), step("lookup_order")]),
    ]
    assert audit(sessions) == {"sessions": 6, "resolved": 4, "fcr": 0.667, "meets_target": False, "over_escalated": 1, "under_escalated": 1,
                               "skipped_prerequisite": 1, "wrong_tool": 1, "over_limit_refunds": 0, "diagnosis": "enforce_in_code"}


def test_e1_no_sessions_give_zero_rates_and_no_diagnosis():
    assert audit([]) == {"sessions": 0, "resolved": 0, "fcr": 0.0, "meets_target": False, "over_escalated": 0, "under_escalated": 0,
                         "skipped_prerequisite": 0, "wrong_tool": 0, "over_limit_refunds": 0, "diagnosis": "none"}


def test_e2_a_protected_call_before_a_successful_identity_check_is_a_skipped_prerequisite():
    assert audit([session([step("get_customer"), step("lookup_order")])])["skipped_prerequisite"] == 0
    assert audit([session([step("lookup_order"), step("get_customer")])])["skipped_prerequisite"] == 1
    assert audit([session([step("get_customer", ok=False), step("process_refund")])])["skipped_prerequisite"] == 1
    assert audit([session([step("process_refund")]), session([step("lookup_order")]), session(CLEAN)])["skipped_prerequisite"] == 2


def test_e3_a_refund_over_the_limit_counts_only_when_it_was_made():
    made = audit([session(CLEAN, refund=10001)])
    assert made["over_limit_refunds"] == 1 and made["diagnosis"] == "enforce_in_code"
    assert audit([session(CLEAN, refund=10000)])["over_limit_refunds"] == 0
    refused = audit([session(CLEAN, outcome="escalated", needs_human=True, refund=50000)])
    assert refused["over_limit_refunds"] == 0 and refused["diagnosis"] == "none"


def test_e4_money_first_then_tool_descriptions_then_escalation_criteria():
    wrong_tool = session([step("get_customer", right="lookup_order")])
    over = session(CLEAN, outcome="escalated")
    assert audit([session([step("process_refund"), step("get_customer")]), wrong_tool, over])["diagnosis"] == "enforce_in_code"
    assert audit([wrong_tool, wrong_tool])["diagnosis"] == "rewrite_tool_descriptions"
    assert audit([wrong_tool, over])["diagnosis"] == "rewrite_tool_descriptions"
    assert audit([wrong_tool, over, over])["diagnosis"] == "write_escalation_criteria"
    assert audit([session(CLEAN, needs_human=True)])["diagnosis"] == "write_escalation_criteria"
    assert audit([session(CLEAN)])["diagnosis"] == "none"


def test_e5_the_target_boundary_and_rounding_of_the_first_contact_rate():
    four_of_five = [session(CLEAN)] * 4 + [session(CLEAN, outcome="escalated", needs_human=True)]
    assert audit(four_of_five)["fcr"] == 0.8 and audit(four_of_five)["meets_target"] is True
    three_of_five = [session(CLEAN)] * 3 + [session(CLEAN, outcome="escalated", needs_human=True)] * 2
    assert audit(three_of_five)["fcr"] == 0.6 and audit(three_of_five)["meets_target"] is False
    two_of_three = audit([session(CLEAN), session(CLEAN), session(CLEAN, outcome="escalated", needs_human=True)])
    assert two_of_three["fcr"] == 0.667
    mixed = audit([session(CLEAN, outcome="escalated"), session(CLEAN, outcome="escalated"), session(CLEAN, needs_human=True)])
    assert (mixed["over_escalated"], mixed["under_escalated"]) == (2, 1)


def test_e6_a_wrong_tool_counts_sessions_and_ignores_steps_with_no_known_right_tool():
    twice = session([step("get_customer", right="lookup_order"), step("lookup_order", right="process_refund")])
    unknown = session([step("get_customer"), step("lookup_order", right="lookup_order")])
    assert audit([twice, unknown, session(CLEAN)])["wrong_tool"] == 1
    assert audit([twice, twice])["wrong_tool"] == 2
