"""A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md."""

MESSAGES = {
    "identity_required": "Verify the customer's identity before this action.",
    "order_not_owned": "That order does not belong to the verified customer.",
    "order_not_checked": "Look up the order before refunding it.",
    "bad_amount": "The amount must be a positive whole number of cents.",
    "exceeds_order": "The amount is more than what is left to refund on the order.",
    "needs_human": "Refunds over the limit need a person.",
    "locked": "Too many failed identity checks; escalate to a person.",
}
TOOLS = ("verify_identity", "lookup_order", "process_refund", "escalate")


def _render(result):
    return "; ".join(f"{key}={value}" for key, value in result.items())


class RefundDesk:
    def __init__(self, backend, limit_cents=10000):
        self.backend, self.limit = backend, limit_cents
        self._customer, self._locked, self._failures = None, False, 0
        self._orders, self._refunds, self._blocked, self._calls = {}, [], [], []

    def state(self):
        return {"customer": self._customer, "locked": self._locked, "failures": self._failures, "orders": {k: dict(v) for k, v in self._orders.items()},
                "refunds": [dict(r) for r in self._refunds], "blocked": [dict(b) for b in self._blocked], "backend_calls": list(self._calls)}

    def _block(self, tool, code, message=None):
        self._blocked.append({"tool": tool, "code": code})
        return {"content": f"BLOCKED {code}: {message or MESSAGES[code]}", "is_error": True, "blocked": code}

    def _backend(self, name, args):
        self._calls.append(name)
        try:
            return self.backend[name](args), None
        except Exception as error:  # a backend failure is a result for the model and changes nothing here
            return None, {"content": str(error), "is_error": True, "blocked": None}

    @staticmethod
    def _ok(result):
        return {"content": _render(result), "is_error": False, "blocked": None}

    def call(self, name, args):
        if name not in TOOLS:
            return self._block(name, "unknown_tool", f"Unknown tool: {name}")
        if name == "escalate":
            result, error = self._backend(name, args)
            return error or self._ok(result)
        if self._locked:
            return self._block(name, "locked")
        if name == "verify_identity":
            result, error = self._backend(name, args)
            if error:
                return error
            if result.get("verified") == "yes" and result.get("customer_id"):
                self._customer, self._failures = result["customer_id"], 0
            else:
                self._customer, self._failures = None, self._failures + 1
                self._locked = self._failures >= 3
            return self._ok(result)
        if name == "lookup_order":
            result, error = self._backend(name, args)
            if error:
                return error
            if result.get("customer_id") != self._customer:
                return self._block(name, "order_not_owned")
            self._orders[result["order_id"]] = dict(result)
            return self._ok(result)
        order = self._orders.get(args.get("order_id"))
        if order is None:
            return self._block(name, "order_not_checked")
        amount = args.get("amount_cents")
        if not isinstance(amount, int) or isinstance(amount, bool) or amount <= 0:
            return self._block(name, "bad_amount")
        if amount > order["total_cents"] - order["refunded_cents"]:
            return self._block(name, "exceeds_order")
        if amount > self.limit:
            return self._block(name, "needs_human")
        result, error = self._backend(name, args)
        if error:
            return error
        order["refunded_cents"] += amount
        self._refunds.append({"order_id": order["order_id"], "amount_cents": amount, "refund_id": result.get("refund_id")})
        return self._ok(result)

    def handoff(self, reason):
        last = self._blocked[-1]["code"] if self._blocked else None
        action = "verify_identity_manually" if self._locked else "review_refund" if last == "needs_human" else "review_case"
        return {"customer_id": self._customer, "identity_verified": self._customer is not None, "reason": reason, "orders_checked": list(self._orders),
                "refunds_done": [dict(r) for r in self._refunds], "blocked": [dict(b) for b in self._blocked], "recommended_action": action}
