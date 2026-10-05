# Planted wrong solutions of module 48-multi-step-workflows-with-guarantees: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/48-multi-step-workflows-with-guarantees/unit-01/practice-1"] = {
    "python": ("desk.py", {
        "wrong-identity-unchecked": [('        if self._customer is None:\n            return self._block(name, "identity_required")\n', '        if self._customer is None and name == "lookup_order":\n            return self._block(name, "identity_required")\n')],
        "wrong-ownership-unchecked": [('            if result.get("customer_id") != self._customer:\n                return self._block(name, "order_not_owned")\n', "")],
        "wrong-over-limit-executes": [('        if amount > self.limit:\n            return self._block(name, "needs_human")\n', "")],
        "wrong-failure-unlocks": [("                self._customer, self._failures = None, self._failures + 1", "                self._failures = self._failures + 1")],
        "wrong-no-lockout": [("                self._locked = self._failures >= 3", "                self._locked = False")],
        "wrong-exceeds-ignored": [('        if amount > order["total_cents"] - order["refunded_cents"]:\n            return self._block(name, "exceeds_order")\n', "")],
        "wrong-zero-amount-ok": [("or amount <= 0:", "or amount < 0:")],
        "wrong-unknown-message": [('f"Unknown tool: {name}"', 'f"Unknown tool {name}"')],
        "wrong-handoff-no-blocks": [('"refunds_done": [dict(r) for r in self._refunds], "blocked": [dict(b) for b in self._blocked], "recommended_action"', '"refunds_done": [dict(r) for r in self._refunds], "blocked": [], "recommended_action"')],
    }),
    "typescript": ("desk.ts", {
        "wrong-identity-unchecked": [('    if (this.customer === null) return this.block(name, "identity_required");\n', '    if (this.customer === null && name === "lookup_order") return this.block(name, "identity_required");\n')],
        "wrong-ownership-unchecked": [('      if (result!.customer_id !== this.customer) return this.block(name, "order_not_owned");\n', "")],
        "wrong-over-limit-executes": [('    if (amount > this.limitCents) return this.block(name, "needs_human");\n', "")],
        "wrong-failure-unlocks": [("        this.customer = null;\n        this.failures += 1;", "        this.failures += 1;")],
        "wrong-no-lockout": [("this.locked = this.failures >= 3;", "this.locked = false;")],
        "wrong-exceeds-ignored": [('    if (amount > order.total_cents - order.refunded_cents) return this.block(name, "exceeds_order");\n', "")],
        "wrong-zero-amount-ok": [("!Number.isInteger(amount) || amount <= 0", "!Number.isInteger(amount) || amount < 0")],
        "wrong-unknown-message": [("`Unknown tool: ${name}`", "`Unknown tool ${name}`")],
        "wrong-handoff-no-blocks": [("      blocked: structuredClone(this.blockedCalls), recommended_action: action };", "      blocked: [], recommended_action: action };")],
    }),
    "java": ("RefundDesk.java", {
        "wrong-identity-unchecked": [('        if (customer == null) return block(name, "identity_required", null);\n', '        if (customer == null && name.equals("lookup_order")) return block(name, "identity_required", null);\n')],
        "wrong-ownership-unchecked": [('            if (!customer.equals(result.get("customer_id"))) return block(name, "order_not_owned", null);\n', "")],
        "wrong-over-limit-executes": [('        if (amount > limitCents) return block(name, "needs_human", null);\n', "")],
        "wrong-failure-unlocks": [("                customer = null;\n                failures++;", "                failures++;")],
        "wrong-no-lockout": [("locked = failures >= 3;", "locked = false;")],
        "wrong-exceeds-ignored": [('        if (amount > (Integer) order.get("total_cents") - refunded) return block(name, "exceeds_order", null);\n', "")],
        "wrong-zero-amount-ok": [("|| amount <= 0) return block", "|| amount < 0) return block")],
        "wrong-unknown-message": [('"Unknown tool: " + name', '"Unknown tool " + name')],
        "wrong-handoff-no-blocks": [('"refunds_done", copies(refunds), "blocked", copies(blocked), "recommended_action", action);', '"refunds_done", copies(refunds), "blocked", new ArrayList<>(), "recommended_action", action);')],
    }),
    "kotlin": ("RefundDesk.kt", {
        "wrong-identity-unchecked": [('val who = customer ?: return block(name, "identity_required")', 'val who = customer ?: if (name == "lookup_order") return block(name, "identity_required") else ""')],
        "wrong-ownership-unchecked": [('if (result!!["customer_id"] != who) return block(name, "order_not_owned")', 'if (result!!["customer_id"] == null) return block(name, "order_not_owned")')],
        "wrong-over-limit-executes": [('        if (amount > limitCents) return block(name, "needs_human")\n', "")],
        "wrong-failure-unlocks": [("                customer = null\n                failures++", "                failures++")],
        "wrong-no-lockout": [("locked = failures >= 3", "locked = false")],
        "wrong-exceeds-ignored": [('        if (amount > (order["total_cents"] as Int) - refunded) return block(name, "exceeds_order")\n', "")],
        "wrong-zero-amount-ok": [("if (amount == null || amount <= 0)", "if (amount == null || amount < 0)")],
        "wrong-unknown-message": [('"Unknown tool: $name"', '"Unknown tool $name"')],
        "wrong-handoff-no-blocks": [('"blocked" to blocked.map { LinkedHashMap(it) }, "recommended_action" to action)', '"blocked" to emptyList<Any?>(), "recommended_action" to action)')],
    }),
}
