"""A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)

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
        log.debug("call input: %r", args)
        # TODO 7 of 8 (finish this to pass e6): the first check of call. When the tool name is not one of TOOLS, return
        #   the refusal `unknown_tool` with the message "Unknown tool: NAME". Example: call("delete_everything", {}) ->
        #   BLOCKED unknown_tool: Unknown tool: delete_everything.
        if name == "escalate":
            result, error = self._backend(name, args)
            return error or self._ok(result)
        if self._locked:
            return self._block(name, "locked")
        if name == "verify_identity":
            result, error = self._backend(name, args)
            if error:
                return error
            # TODO 1 of 8 (finish this to pass m1, e5): what a `verify_identity` result does to the desk. Receives the
            #   backend's result map. A result with `verified` equal to `yes` and a `customer_id` sets the verified
            #   customer and resets the failure count to 0; any other result clears the verified customer, adds one
            #   failure and locks the desk at three. Example: three results of {verified: no} lock the desk.
            return self._ok(result)
        # TODO 2 of 8 (finish this to pass e1): the prerequisite check. When no customer is verified, return the refusal
        #   `identity_required` for this call: block(name, "identity_required") builds and records it. Example:
        #   process_refund before verify_identity -> BLOCKED identity_required, and the backend is never called.
        if name == "lookup_order":
            result, error = self._backend(name, args)
            if error:
                return error
            # TODO 3 of 8 (finish this to pass e2): the ownership check on a looked-up order. When the order's
            #   customer_id is not the verified customer, return the refusal `order_not_owned` before the order is
            #   remembered. Example: customer C1 looks up an order of C2 -> BLOCKED order_not_owned, and state()["orders"]
            #   stays empty.
            self._orders[result["order_id"]] = dict(result)
            return self._ok(result)
        order = self._orders.get(args.get("order_id"))
        if order is None:
            return self._block(name, "order_not_checked")
        amount = args.get("amount_cents")
        # TODO 4 of 8 (finish this to pass e3): the amount check. Receives the amount from the call. When it is not a
        #   whole number above zero (a boolean, a decimal, a string, a missing value or 0), return the refusal
        #   `bad_amount`. Example: amount_cents 0 or 12.5 or True -> BLOCKED bad_amount.
        amount = amount if isinstance(amount, int) else 0
        # TODO 5 of 8 (finish this to pass e3): the check against the order. When the amount is more than the order's
        #   total_cents minus its refunded_cents, return the refusal `exceeds_order`. Example: 3000 refunded on a 5000
        #   order, then 2001 -> BLOCKED exceeds_order.
        # TODO 6 of 8 (finish this to pass e4): the authority limit. When the amount is above the desk's limit, return
        #   the refusal `needs_human` and do not call the backend. Example: limit 10000, amount 12000 on a 50000 order ->
        #   BLOCKED needs_human.
        result, error = self._backend(name, args)
        if error:
            return error
        order["refunded_cents"] += amount
        self._refunds.append({"order_id": order["order_id"], "amount_cents": amount, "refund_id": result.get("refund_id")})
        return self._ok(result)

    def handoff(self, reason):
        last = self._blocked[-1]["code"] if self._blocked else None
        # TODO 8 of 8 (finish this to pass e4, e5): the recommended action of the hand-off. `last` is the code of the
        #   most recent refusal or none. Choose verify_identity_manually when the desk is locked, otherwise review_refund
        #   when last is needs_human, otherwise review_case. Example: locked desk -> verify_identity_manually.
        action = "review_case"
        return {"customer_id": self._customer, "identity_verified": self._customer is not None, "reason": reason, "orders_checked": list(self._orders),
                "refunds_done": [dict(r) for r in self._refunds], "blocked": [dict(b) for b in self._blocked], "recommended_action": action}
