private val log = System.getLogger("support_desk")

/**
 * A support agent's whole control surface in one dispatcher: the identity gate, errors the loop can act on, a stall guard and the three escalation triggers.
 *
 * The model is a script of the calls it asks for, in the shapes of the tool_use blocks of module 26, without the client: this example is about the code that
 * runs the tools, not about what a model says. Customers, orders and the incidents are made up. The limit, the stall count and the codes are this course's own
 * design, not an Anthropic interface.
 */
const val LIMIT = 10_000 // cents: a refund above it is a person's decision
const val STALL = 3 // the same call this many times in a row is no progress

val CUSTOMERS = mapOf("C1" to ("Ana Silva" to "ana@example.com"), "C2" to ("Ana Silva" to "ana.s@example.com"), "C3" to ("Ben Ortiz" to "ben@example.com"))
val ORDERS = mapOf("O1" to ("C3" to 5000), "O2" to ("C3" to 30000), "O3" to ("C1" to 4000))

data class Call(val tool: String, val args: Map<String, Any>)

data class Result(val ok: Boolean, val code: String? = null, val retryable: Boolean = false, val message: String? = null, val content: String? = null)

data class Escalation(val trigger: String, val reason: String, val customer: String?, val verified: Boolean, val orders: List<String>, val refunds: List<String>, val refused: List<String>)

/** One conversation's state. A new case gets a new Desk, so nothing of one customer reaches another. */
class Desk {
    var customer: String? = null
    val checked = mutableListOf<String>()
    val refunds = mutableListOf<String>()
    val refused = mutableListOf<String>()
    val backend = mutableListOf<String>()
    val recent = mutableListOf<String>()
    var escalation: Escalation? = null
    var lastCode: String? = null
    var retries = 0
    val flaky = mutableSetOf("O2") // the first lookup of this order fails with a transient fault

    fun fail(code: String, message: String, retryable: Boolean = false): Result {
        lastCode = code
        if (code != "transient" && code != "not_found") refused += code
        return Result(false, code, retryable, message)
    }

    /** The record a person reads, built from the desk's own state and not from the model's account. */
    fun escalate(trigger: String, reason: String): Result {
        escalation = Escalation(trigger, reason, customer, customer != null, checked.sorted(), refunds.toList(), refused.toList())
        return Result(true, content = "handed over")
    }

    fun call(tool: String, args: Map<String, Any>): Result {
        lastCode = null
        recent += tool + args.toSortedMap()
        if (recent.size > STALL) recent.removeAt(0)
        if (tool == "escalate_to_human") return escalate(args["trigger"] as String, args["reason"] as String) // the way to a person never waits for a prerequisite
        if (recent.size == STALL && recent.distinct().size == 1) return escalate("stalled", "$tool repeated $STALL times without progress")
        if (tool == "get_customer") {
            val found = CUSTOMERS.filter { (_, c) -> args["query"] == c.first || args["query"] == c.second }.keys.toList()
            if (found.size > 1) return fail("ambiguous_match", "${found.size} customers match. Ask for the e-mail address. Do not pick one.")
            if (found.isEmpty()) return fail("not_found", "No customer matches. Ask for the e-mail address.")
            customer = found[0]
            return Result(true, content = "customer_id=${found[0]}")
        }
        if (customer == null) return fail("identity_required", "Identify the customer with get_customer before this action.")
        val id = args["order_id"] as String? ?: ""
        val order = ORDERS[id]
        if (tool == "lookup_order") {
            backend += tool
            if (flaky.remove(id)) return fail("transient", "The order service timed out. Retry.", true)
            if (order == null) return fail("not_found", "No such order.")
            if (order.first != customer) return fail("order_not_owned", "That order does not belong to the verified customer.")
            checked += id
            return Result(true, content = "total_cents=${order.second}")
        }
        if (tool == "process_refund") {
            if (id !in checked) return fail("order_not_checked", "Look up the order before refunding it.")
            val amount = args["amount_cents"] as Int
            if (amount > LIMIT) return fail("needs_human", "A refund above the limit is decided by a person. Escalate.")
            backend += tool
            refunds += "$id:$amount"
            return Result(true, content = "refund_id=R${refunds.size}")
        }
        return fail("unknown_tool", "No tool named $tool.")
    }
}

/** The loop: each call goes through the desk, a retryable error is retried once, and the outcome is read from the desk. */
fun run(script: List<Call>): Pair<Desk, String> {
    val desk = Desk()
    for ((tool, args) in script) {
        val result = desk.call(tool, args)
        if (!result.ok && result.retryable) {
            desk.retries += 1
            desk.call(tool, args)
        }
    }
    val outcome = if (desk.escalation != null) "escalated" else if (desk.lastCode == "ambiguous_match") "asked" else "resolved"
    return desk to outcome
}

fun join(items: List<String>) = items.joinToString(",").ifEmpty { "-" }

fun call(tool: String, vararg kv: Pair<String, Any>) = Call(tool, mapOf(*kv))

val BEN = call("get_customer", "query" to "ben@example.com")

val INCIDENTS = listOf(
    "skips identity" to listOf(call("lookup_order", "order_id" to "O1"), BEN, call("lookup_order", "order_id" to "O1"), call("process_refund", "order_id" to "O1", "amount_cents" to 2000)),
    "transient fault" to listOf(BEN, call("lookup_order", "order_id" to "O2"), call("process_refund", "order_id" to "O2", "amount_cents" to 8000)),
    "over the limit" to listOf(BEN, call("lookup_order", "order_id" to "O2"), call("process_refund", "order_id" to "O2", "amount_cents" to 25000),
        call("escalate_to_human", "trigger" to "needs_human", "reason" to "refund of 250.00 asked")),
    "someone else's order" to listOf(call("get_customer", "query" to "ana@example.com"), call("lookup_order", "order_id" to "O1")),
    "two customers match" to listOf(call("get_customer", "query" to "Ana Silva")),
    "asks for a person" to listOf(call("escalate_to_human", "trigger" to "customer_request", "reason" to "customer asked for a person")),
    "no progress" to listOf(BEN, call("lookup_order", "order_id" to "O9"), call("lookup_order", "order_id" to "O9"), call("lookup_order", "order_id" to "O9")),
)

fun main() {
    for ((name, script) in INCIDENTS) {
        val (desk, outcome) = run(script)
        println("${name.padEnd(21)} outcome=${outcome.padEnd(9)} refused=${join(desk.refused)} retries=${desk.retries} refunds=${join(desk.refunds)} backend=${join(desk.backend)}")
        desk.escalation?.let { e ->
            println("  handoff: trigger=${e.trigger} verified=${if (e.verified) "yes" else "no"} customer=${e.customer ?: "-"} orders=${join(e.orders)} refunds=${join(e.refunds)} refused=${join(e.refused)}")
        }
    }
}
