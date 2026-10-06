import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from desk import RefundDesk

ORDERS = {"O1": {"order_id": "O1", "customer_id": "C1", "total_cents": 5000, "refunded_cents": 0},
          "O2": {"order_id": "O2", "customer_id": "C2", "total_cents": 3000, "refunded_cents": 0},
          "O9": {"order_id": "O9", "customer_id": "C1", "total_cents": 50000, "refunded_cents": 0}}


class Backend(dict):
    """A scripted backend that keeps a log of every call it receives."""

    def __init__(self, break_refund=False):
        self.log, self.refunds = [], 0
        super().__init__(verify_identity=self._verify, lookup_order=self._lookup, process_refund=self._refund, escalate=self._escalate)
        self.break_refund = break_refund

    def _verify(self, args):
        self.log.append("verify_identity")
        return {"verified": "yes", "customer_id": "C1"} if args.get("code") == "1234" else {"verified": "no"}

    def _lookup(self, args):
        self.log.append("lookup_order")
        return dict(ORDERS[args["order_id"]])

    def _refund(self, args):
        self.log.append("process_refund")
        if self.break_refund:
            raise RuntimeError("payment service offline")
        self.refunds += 1
        return {"refund_id": f"R{self.refunds}", "amount_cents": args["amount_cents"]}

    def _escalate(self, args):
        self.log.append("escalate")
        return {"ticket_id": "T1"}


def desk(**kw):
    backend = Backend(kw.pop("break_refund", False))
    return RefundDesk(backend, **kw), backend


def res(desk, name, **args):
    out = desk.call(name, args)
    assert out is not None, "call returned None"
    return out


def verified(**kw):
    d, b = desk(**kw)
    res(d, "verify_identity", code="1234")
    return d, b


def test_m1_a_verified_customer_can_look_up_an_order_and_be_refunded_within_the_limit():
    d, b = desk()
    assert res(d, "verify_identity", code="1234") == {"content": "verified=yes; customer_id=C1", "is_error": False, "blocked": None}
    assert res(d, "lookup_order", order_id="O1")["content"] == "order_id=O1; customer_id=C1; total_cents=5000; refunded_cents=0"
    assert res(d, "process_refund", order_id="O1", amount_cents=2000) == {"content": "refund_id=R1; amount_cents=2000", "is_error": False, "blocked": None}
    state = d.state() or {}
    assert (state.get("customer"), state.get("locked"), state.get("failures"), state.get("blocked")) == ("C1", False, 0, [])
    assert state.get("refunds") == [{"order_id": "O1", "amount_cents": 2000, "refund_id": "R1"}] and state["orders"]["O1"]["refunded_cents"] == 2000
    assert b.log == ["verify_identity", "lookup_order", "process_refund"]


def test_e1_a_refund_before_identity_is_verified_is_blocked_in_code_and_never_reaches_the_backend():
    d, b = desk()
    out = res(d, "process_refund", order_id="O1", amount_cents=500)
    assert out == {"content": "BLOCKED identity_required: Verify the customer's identity before this action.", "is_error": True, "blocked": "identity_required"}
    assert b.log == [] and (d.state() or {}).get("blocked") == [{"tool": "process_refund", "code": "identity_required"}]
    res(d, "verify_identity", code="1234")
    res(d, "lookup_order", order_id="O1")
    assert res(d, "process_refund", order_id="O1", amount_cents=500)["blocked"] is None and b.log[-1] == "process_refund"


def test_e2_an_order_that_belongs_to_someone_else_is_neither_shown_nor_refundable():
    d, b = desk()
    assert res(d, "lookup_order", order_id="O1")["blocked"] == "identity_required" and b.log == []
    res(d, "verify_identity", code="1234")
    other = res(d, "lookup_order", order_id="O2")
    assert other == {"content": "BLOCKED order_not_owned: That order does not belong to the verified customer.", "is_error": True, "blocked": "order_not_owned"}
    assert "C2" not in other["content"] and "O2" not in (d.state() or {}).get("orders", {"O2": 1})
    assert res(d, "process_refund", order_id="O2", amount_cents=100)["blocked"] == "order_not_checked" and "process_refund" not in b.log


def test_e3_a_refund_is_checked_against_the_order_its_amount_and_what_is_left():
    d, b = verified()
    assert res(d, "process_refund", order_id="O1", amount_cents=100)["blocked"] == "order_not_checked"
    res(d, "lookup_order", order_id="O1")
    for bad in (0, -5, 12.5, "100", True, None):
        assert res(d, "process_refund", order_id="O1", amount_cents=bad)["blocked"] == "bad_amount", bad
    assert res(d, "process_refund", order_id="O1", amount_cents=5001)["blocked"] == "exceeds_order"
    assert res(d, "process_refund", order_id="O1", amount_cents=3000)["blocked"] is None
    out = res(d, "process_refund", order_id="O1", amount_cents=2500)
    assert (out["blocked"], out["content"]) == ("exceeds_order", "BLOCKED exceeds_order: The amount is more than what is left to refund on the order.")
    assert res(d, "process_refund", order_id="O1", amount_cents=2000)["blocked"] is None and b.log.count("process_refund") == 2


def test_e4_a_refund_over_the_limit_is_not_executed_and_becomes_a_structured_hand_off():
    d, b = verified(limit_cents=10000)
    res(d, "lookup_order", order_id="O9")
    out = res(d, "process_refund", order_id="O9", amount_cents=25000)
    assert (out["blocked"], out["is_error"], out["content"]) == ("needs_human", True, "BLOCKED needs_human: Refunds over the limit need a person.")
    assert "process_refund" not in b.log
    assert res(d, "process_refund", order_id="O9", amount_cents=10000)["blocked"] is None
    assert res(d, "process_refund", order_id="O9", amount_cents=10001)["blocked"] == "needs_human"
    handoff = d.handoff("Customer asks for a 250.00 refund") or {}
    assert handoff == {"customer_id": "C1", "identity_verified": True, "reason": "Customer asks for a 250.00 refund", "orders_checked": ["O9"],
                       "refunds_done": [{"order_id": "O9", "amount_cents": 10000, "refund_id": "R1"}],
                       "blocked": [{"tool": "process_refund", "code": "needs_human"}, {"tool": "process_refund", "code": "needs_human"}], "recommended_action": "review_refund"}


def test_e5_a_failed_check_does_not_unlock_anything_and_three_in_a_row_lock_the_desk():
    d, b = desk()
    assert res(d, "verify_identity", code="0000")["content"] == "verified=no"
    assert res(d, "lookup_order", order_id="O1")["blocked"] == "identity_required"
    res(d, "verify_identity", code="1234")
    res(d, "verify_identity", code="0000")
    assert (d.state() or {}).get("customer") is None and res(d, "lookup_order", order_id="O1")["blocked"] == "identity_required"
    res(d, "verify_identity", code="0000")
    res(d, "verify_identity", code="0000")
    state = d.state() or {}
    assert (state.get("locked"), state.get("failures")) == (True, 3)
    assert res(d, "verify_identity", code="1234") == {"content": "BLOCKED locked: Too many failed identity checks; escalate to a person.", "is_error": True, "blocked": "locked"}
    assert res(d, "escalate", reason="locked out")["content"] == "ticket_id=T1"
    assert (d.handoff("identity could not be verified") or {}).get("recommended_action") == "verify_identity_manually"


def test_e6_unknown_tools_and_backend_errors_are_reported_and_the_hand_off_lists_every_block():
    d, b = verified(break_refund=True)
    res(d, "lookup_order", order_id="O1")
    assert res(d, "delete_everything") == {"content": "BLOCKED unknown_tool: Unknown tool: delete_everything", "is_error": True, "blocked": "unknown_tool"}
    failed = res(d, "process_refund", order_id="O1", amount_cents=500)
    assert failed == {"content": "payment service offline", "is_error": True, "blocked": None}
    state = d.state() or {}
    assert state.get("refunds") == [] and state["orders"]["O1"]["refunded_cents"] == 0
    handoff = d.handoff("the payment service is down") or {}
    assert handoff.get("blocked") == [{"tool": "delete_everything", "code": "unknown_tool"}] and handoff.get("refunds_done") == []
    assert handoff.get("recommended_action") == "review_case" and handoff.get("orders_checked") == ["O1"]
