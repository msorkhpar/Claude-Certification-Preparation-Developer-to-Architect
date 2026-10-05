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
        if (name !in TOOLS) return block(name, "unknown_tool", "Unknown tool: $name")
        if (name == "escalate") {
            val (result, error) = run(name, args)
            return error ?: ok(result!!)
        }
        if (locked) return block(name, "locked")
        if (name == "verify_identity") {
            val (result, error) = run(name, args)
            if (error != null) return error
            if (result!!["verified"] == "yes" && result["customer_id"] != null) {
                customer = result["customer_id"] as String
                failures = 0
            } else {
                customer = null
                failures++
                locked = failures >= 3
            }
            return ok(result)
        }
        val who = customer ?: return block(name, "identity_required")
        if (name == "lookup_order") {
            val (result, error) = run(name, args)
            if (error != null) return error
            if (result!!["customer_id"] != who) return block(name, "order_not_owned")
            orders[result["order_id"] as String] = LinkedHashMap(result)
            return ok(result)
        }
        val order = orders[args["order_id"]] ?: return block(name, "order_not_checked")
        val amount = args["amount_cents"] as? Int
        if (amount == null || amount <= 0) return block(name, "bad_amount")
        val refunded = order["refunded_cents"] as Int
        if (amount > (order["total_cents"] as Int) - refunded) return block(name, "exceeds_order")
        if (amount > limitCents) return block(name, "needs_human")
        val (result, error) = run(name, args)
        if (error != null) return error
        order["refunded_cents"] = refunded + amount
        refunds.add(linkedMapOf("order_id" to order["order_id"], "amount_cents" to amount, "refund_id" to result!!["refund_id"]))
        return ok(result)
    }

    fun handoff(reason: String): Map<String, Any?> {
        val last = blocked.lastOrNull()?.get("code")
        val action = if (locked) "verify_identity_manually" else if (last == "needs_human") "review_refund" else "review_case"
        return linkedMapOf("customer_id" to customer, "identity_verified" to (customer != null), "reason" to reason, "orders_checked" to orders.keys.toList(),
            "refunds_done" to refunds.map { LinkedHashMap(it) }, "blocked" to blocked.map { LinkedHashMap(it) }, "recommended_action" to action)
    }
}
