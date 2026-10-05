"""A support agent's whole control surface in one dispatcher: the identity gate, errors the loop can act on, a stall guard and the three escalation triggers.

The model is a script of the calls it asks for, in the shapes of the tool_use blocks of module 26, without the client: this example is about the code that
runs the tools, not about what a model says. Customers, orders and the incidents are made up. The limit, the stall count and the codes are this course's own
design, not an Anthropic interface.
"""
import logging

log = logging.getLogger(__name__)

LIMIT = 10_000  # cents: a refund above it is a person's decision
STALL = 3       # the same call this many times in a row is no progress

CUSTOMERS = {"C1": ("Ana Silva", "ana@example.com"), "C2": ("Ana Silva", "ana.s@example.com"), "C3": ("Ben Ortiz", "ben@example.com")}
ORDERS = {"O1": ("C3", 5000), "O2": ("C3", 30000), "O3": ("C1", 4000)}


class Desk:
    """One conversation's state. A new case gets a new Desk, so nothing of one customer reaches another."""

    def __init__(self):
        self.customer, self.checked, self.refunds, self.refused = None, [], [], []
        self.backend, self.recent, self.escalation, self.last_code, self.retries = [], [], None, None, 0
        self.flaky = {"O2"}  # the first lookup of this order fails with a transient fault

    def fail(self, code, message, retryable=False):
        self.last_code = code
        if code not in ("transient", "not_found"):
            self.refused.append(code)
        return {"ok": False, "code": code, "retryable": retryable, "message": message}

    def escalate(self, trigger, reason):
        """The record a person reads, built from the desk's own state and not from the model's account."""
        self.escalation = {"trigger": trigger, "reason": reason, "customer": self.customer, "verified": self.customer is not None,
                           "orders": sorted(self.checked), "refunds": list(self.refunds), "refused": list(self.refused)}
        return {"ok": True, "content": "handed over"}

    def call(self, tool, args):
        self.last_code = None
        self.recent = (self.recent + [(tool, sorted(args.items()))])[-STALL:]
        if tool == "escalate_to_human":  # the way to a person never waits for a prerequisite
            return self.escalate(args["trigger"], args["reason"])
        if len(self.recent) == STALL and len(set(map(str, self.recent))) == 1:
            return self.escalate("stalled", f"{tool} repeated {STALL} times without progress")
        if tool == "get_customer":
            found = [c for c, (name, email) in CUSTOMERS.items() if args["query"] in (name, email)]
            if len(found) > 1:
                return self.fail("ambiguous_match", f"{len(found)} customers match. Ask for the e-mail address. Do not pick one.")
            if not found:
                return self.fail("not_found", "No customer matches. Ask for the e-mail address.")
            self.customer = found[0]
            return {"ok": True, "content": f"customer_id={found[0]}"}
        if self.customer is None:
            return self.fail("identity_required", "Identify the customer with get_customer before this action.")
        owner, total = ORDERS.get(args.get("order_id"), (None, 0))
        if tool == "lookup_order":
            self.backend.append(tool)
            if args["order_id"] in self.flaky:
                self.flaky.discard(args["order_id"])
                return self.fail("transient", "The order service timed out. Retry.", retryable=True)
            if owner is None:
                return self.fail("not_found", "No such order.")
            if owner != self.customer:
                return self.fail("order_not_owned", "That order does not belong to the verified customer.")
            self.checked.append(args["order_id"])
            return {"ok": True, "content": f"total_cents={total}"}
        if tool == "process_refund":
            if args["order_id"] not in self.checked:
                return self.fail("order_not_checked", "Look up the order before refunding it.")
            if args["amount_cents"] > LIMIT:
                return self.fail("needs_human", "A refund above the limit is decided by a person. Escalate.")
            self.backend.append(tool)
            self.refunds.append(f"{args['order_id']}:{args['amount_cents']}")
            return {"ok": True, "content": f"refund_id=R{len(self.refunds)}"}
        return self.fail("unknown_tool", f"No tool named {tool}.")


def run(script):
    """The loop: each call goes through the desk, a retryable error is retried once, and the outcome is read from the desk."""
    desk = Desk()
    for tool, args in script:
        result = desk.call(tool, args)
        if not result["ok"] and result.get("retryable"):
            desk.retries += 1
            result = desk.call(tool, args)
    outcome = "escalated" if desk.escalation else "asked" if desk.last_code == "ambiguous_match" else "resolved"
    return desk, outcome


def join(items):
    return ",".join(items) or "-"


INCIDENTS = [
    ("skips identity", [("lookup_order", {"order_id": "O1"}), ("get_customer", {"query": "ben@example.com"}), ("lookup_order", {"order_id": "O1"}),
                        ("process_refund", {"order_id": "O1", "amount_cents": 2000})]),
    ("transient fault", [("get_customer", {"query": "ben@example.com"}), ("lookup_order", {"order_id": "O2"}), ("process_refund", {"order_id": "O2", "amount_cents": 8000})]),
    ("over the limit", [("get_customer", {"query": "ben@example.com"}), ("lookup_order", {"order_id": "O2"}), ("process_refund", {"order_id": "O2", "amount_cents": 25000}),
                        ("escalate_to_human", {"trigger": "needs_human", "reason": "refund of 250.00 asked"})]),
    ("someone else's order", [("get_customer", {"query": "ana@example.com"}), ("lookup_order", {"order_id": "O1"})]),
    ("two customers match", [("get_customer", {"query": "Ana Silva"})]),
    ("asks for a person", [("escalate_to_human", {"trigger": "customer_request", "reason": "customer asked for a person"})]),
    ("no progress", [("get_customer", {"query": "ben@example.com"})] + [("lookup_order", {"order_id": "O9"})] * 3),
]


def main():
    for name, script in INCIDENTS:
        desk, outcome = run(script)
        print(f"{name:<21} outcome={outcome:<9} refused={join(desk.refused)} retries={desk.retries} refunds={join(desk.refunds)} backend={join(desk.backend)}")
        if desk.escalation:
            e = desk.escalation
            print(f"  handoff: trigger={e['trigger']} verified={'yes' if e['verified'] else 'no'} customer={e['customer'] or '-'} orders={join(e['orders'])} refunds={join(e['refunds'])} refused={join(e['refused'])}")


if __name__ == "__main__":
    main()
