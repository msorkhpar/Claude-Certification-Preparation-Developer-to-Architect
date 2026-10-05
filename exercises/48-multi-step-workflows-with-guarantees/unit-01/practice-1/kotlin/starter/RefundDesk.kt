/** A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md. Results are JSON-like maps. */

private val log = System.getLogger("desk")

typealias Backend = Map<String, (Map<String, Any?>) -> Map<String, Any?>>

private val MESSAGES = mapOf(
    "identity_required" to "Verify the customer's identity before this action.",
    "order_not_owned" to "That order does not belong to the verified customer.",
    "order_not_checked" to "Look up the order before refunding it.",
    "bad_amount" to "The amount must be a positive whole number of cents.",
    "exceeds_order" to "The amount is more than what is left to refund on the order.",
    "needs_human" to "Refunds over the limit need a person.",
    "locked" to "Too many failed identity checks; escalate to a person.",
)
private val TOOLS = listOf("verify_identity", "lookup_order", "process_refund", "escalate")

class RefundDesk(private val backend: Backend, private val limitCents: Int = 10000) {
    private var customer: String? = null
    private var locked = false
    private var failures = 0
    private val orders = linkedMapOf<String, MutableMap<String, Any?>>()
    private val refunds = mutableListOf<Map<String, Any?>>()
    private val blocked = mutableListOf<Map<String, Any?>>()
    private val calls = mutableListOf<String>()

    fun state(): Map<String, Any?> = linkedMapOf(
        "customer" to customer, "locked" to locked, "failures" to failures, "orders" to orders.mapValues { LinkedHashMap(it.value) },
        "refunds" to refunds.map { LinkedHashMap(it) }, "blocked" to blocked.map { LinkedHashMap(it) }, "backend_calls" to calls.toList())

    private fun block(tool: String, code: String, message: String? = null): Map<String, Any?> {
        blocked.add(linkedMapOf("tool" to tool, "code" to code))
        return linkedMapOf("content" to "BLOCKED $code: ${message ?: MESSAGES[code]}", "is_error" to true, "blocked" to code)
    }

    private fun ok(result: Map<String, Any?>): Map<String, Any?> =
        linkedMapOf("content" to result.entries.joinToString("; ") { "${it.key}=${it.value}" }, "is_error" to false, "blocked" to null)

    private fun run(name: String, args: Map<String, Any?>): Pair<Map<String, Any?>?, Map<String, Any?>?> {
        calls.add(name)
        return try {
            backend.getValue(name)(args) to null
        } catch (error: RuntimeException) { // a backend failure is a result for the model and changes nothing here
            null to linkedMapOf("content" to error.message.toString(), "is_error" to true, "blocked" to null)
        }
    }

    fun call(name: String, args: Map<String, Any?>): Map<String, Any?> {
        log.log(System.Logger.Level.DEBUG, "call input: {0}", args)
        // TODO 7 of 8 (finish this to pass e6): the first check of call. When the tool name is not one of TOOLS, return
        //   the refusal `unknown_tool` with the message "Unknown tool: NAME". Example: call("delete_everything", {}) ->
        //   BLOCKED unknown_tool: Unknown tool: delete_everything.
        if (name == "escalate") {
            val (result, error) = run(name, args)
            return error ?: ok(result!!)
        }
        if (locked) return block(name, "locked")
        if (name == "verify_identity") {
            val (result, error) = run(name, args)
            if (error != null) return error
            // TODO 1 of 8 (finish this to pass m1, e5): what a `verify_identity` result does to the desk. Receives the
            //   backend's result map. A result with `verified` equal to `yes` and a `customer_id` sets the verified
            //   customer and resets the failure count to 0; any other result clears the verified customer, adds one
            //   failure and locks the desk at three. Example: three results of {verified: no} lock the desk.
            requireNotNull(result)
            return ok(result)
        }
        // TODO 2 of 8 (finish this to pass e1): the prerequisite check. When no customer is verified, return the refusal
        //   `identity_required` for this call: block(name, "identity_required") builds and records it. Example:
        //   process_refund before verify_identity -> BLOCKED identity_required, and the backend is never called.
        val who = customer ?: ""
        if (name == "lookup_order") {
            val (result, error) = run(name, args)
            if (error != null) return error
            // TODO 3 of 8 (finish this to pass e2): the ownership check on a looked-up order. When the order's
            //   customer_id is not the verified customer, return the refusal `order_not_owned` before the order is
            //   remembered. Example: customer C1 looks up an order of C2 -> BLOCKED order_not_owned, and state()["orders"]
            //   stays empty.
            requireNotNull(result)
            orders[result["order_id"] as String] = LinkedHashMap(result)
            return ok(result)
        }
        val order = orders[args["order_id"]] ?: return block(name, "order_not_checked")
        val amount = args["amount_cents"] as? Int
        // TODO 4 of 8 (finish this to pass e3): the amount check. Receives the amount from the call. When it is not a
        //   whole number above zero (a boolean, a decimal, a string, a missing value or 0), return the refusal
        //   `bad_amount`. Example: amount_cents 0 or 12.5 or True -> BLOCKED bad_amount.
        if (amount == null) return block(name, "bad_amount")
        val refunded = order["refunded_cents"] as Int
        // TODO 5 of 8 (finish this to pass e3): the check against the order. When the amount is more than the order's
        //   total_cents minus its refunded_cents, return the refusal `exceeds_order`. Example: 3000 refunded on a 5000
        //   order, then 2001 -> BLOCKED exceeds_order.
        // TODO 6 of 8 (finish this to pass e4): the authority limit. When the amount is above the desk's limit, return
        //   the refusal `needs_human` and do not call the backend. Example: limit 10000, amount 12000 on a 50000 order ->
        //   BLOCKED needs_human.
        val (result, error) = run(name, args)
        if (error != null) return error
        order["refunded_cents"] = refunded + amount
        refunds.add(linkedMapOf("order_id" to order["order_id"], "amount_cents" to amount, "refund_id" to result!!["refund_id"]))
        return ok(result)
    }

    fun handoff(reason: String): Map<String, Any?> {
        val last = blocked.lastOrNull()?.get("code")
        // TODO 8 of 8 (finish this to pass e4, e5): the recommended action of the hand-off. `last` is the code of the
        //   most recent refusal or none. Choose verify_identity_manually when the desk is locked, otherwise review_refund
        //   when last is needs_human, otherwise review_case. Example: locked desk -> verify_identity_manually.
        val action = "review_case"
        return linkedMapOf("customer_id" to customer, "identity_verified" to (customer != null), "reason" to reason, "orders_checked" to orders.keys.toList(),
            "refunds_done" to refunds.map { LinkedHashMap(it) }, "blocked" to blocked.map { LinkedHashMap(it) }, "recommended_action" to action)
    }
}
