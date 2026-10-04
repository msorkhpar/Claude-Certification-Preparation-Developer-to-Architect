from support_desk import LIMIT, STALL, Desk, run

BEN = ("get_customer", {"query": "ben@example.com"})


def test_nothing_runs_before_the_customer_is_identified_and_the_backend_sees_no_call():
    desk = Desk()
    result = desk.call("lookup_order", {"order_id": "O1"})
    assert result["code"] == "identity_required" and "get_customer" in result["message"] and desk.backend == []


def test_two_matching_customers_are_a_question_and_never_a_guess():
    desk, outcome = run([("get_customer", {"query": "Ana Silva"})])
    assert desk.customer is None and outcome == "asked" and desk.refused == ["ambiguous_match"]


def test_an_order_of_another_customer_is_refused_without_naming_its_owner():
    desk = Desk()
    desk.call(*("get_customer", {"query": "ana@example.com"}))
    result = desk.call("lookup_order", {"order_id": "O1"})
    assert result["code"] == "order_not_owned" and "C3" not in result["message"] and desk.checked == []


def test_a_refund_at_the_limit_runs_and_one_cent_above_needs_a_person():
    desk = Desk()
    desk.call(*BEN)
    desk.flaky.clear()
    desk.call("lookup_order", {"order_id": "O2"})
    assert desk.call("process_refund", {"order_id": "O2", "amount_cents": LIMIT})["ok"]
    over = desk.call("process_refund", {"order_id": "O2", "amount_cents": LIMIT + 1})
    assert over["code"] == "needs_human" and desk.refunds == ["O2:10000"]


def test_a_transient_fault_is_retried_once_and_a_permanent_one_is_not():
    desk, outcome = run([BEN, ("lookup_order", {"order_id": "O2"})])
    assert desk.retries == 1 and desk.checked == ["O2"] and outcome == "resolved"
    desk, _ = run([BEN, ("lookup_order", {"order_id": "O9"})])
    assert desk.retries == 0


def test_the_same_call_three_times_in_a_row_escalates_and_different_calls_do_not():
    desk, outcome = run([BEN] + [("lookup_order", {"order_id": "O9"})] * STALL)
    assert outcome == "escalated" and desk.escalation["trigger"] == "stalled"
    desk, outcome = run([BEN, ("lookup_order", {"order_id": "O9"}), ("lookup_order", {"order_id": "O8"}), ("lookup_order", {"order_id": "O9"})])
    assert outcome == "resolved"


def test_a_person_can_always_be_reached_and_the_record_comes_from_the_desks_state():
    desk, outcome = run([("escalate_to_human", {"trigger": "customer_request", "reason": "wants a person"})])
    assert outcome == "escalated" and desk.escalation["verified"] is False
    desk, _ = run([BEN, ("lookup_order", {"order_id": "O2"}), ("process_refund", {"order_id": "O2", "amount_cents": 25000}),
                   ("escalate_to_human", {"trigger": "needs_human", "reason": "x"})])
    e = desk.escalation
    assert (e["customer"], e["orders"], e["refunds"], e["refused"]) == ("C3", ["O2"], [], ["needs_human"])
