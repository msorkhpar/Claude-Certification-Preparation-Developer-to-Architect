"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from desk import RefundDesk

ORDER = {"order_id": "O1", "customer_id": "C1", "total_cents": 5000, "refunded_cents": 0}
refunds = []

# A scripted backend, like the one the tests use: four functions that answer with fixed data.
backend = {
    "verify_identity": lambda args: {"verified": "yes", "customer_id": "C1"} if args.get("code") == "1234" else {"verified": "no"},
    "lookup_order": lambda args: dict(ORDER),
    "process_refund": lambda args: refunds.append(args) or {"refund_id": f"R{len(refunds)}", "amount_cents": args["amount_cents"]},
    "escalate": lambda args: {"ticket_id": "T1"},
}
desk = RefundDesk(backend)

# The model breaks the order of the steps: a refund before identity is verified must be blocked in code.
early = desk.call("process_refund", {"order_id": "O1", "amount_cents": 2000}) or {}
print("refund before verifying:", early.get("content"), "| is_error:", early.get("is_error"))

for name, args in (("verify_identity", {"code": "1234"}), ("lookup_order", {"order_id": "O1"}),
                   ("process_refund", {"order_id": "O1", "amount_cents": 2000})):
    out = desk.call(name, args) or {}
    print(f"{name}:", out.get("content"))
print("refunds that reached the backend:", len(refunds))
