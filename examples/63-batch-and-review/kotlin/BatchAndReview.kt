/**
 * What a batch asks of its caller, and what an independent review is given.
 *
 * Read on 2026-10-04 in the Claude API documentation ("Batch processing"): a batch is processed asynchronously, results are available when every request has finished or after 24 hours, whichever comes first,
 * a request is identified by its `custom_id` (1 to 64 letters, digits, hyphens and underscores), results can come back in any order, and `stream`, `speed` and a `max_tokens` of 0 are refused. The exam guide's wording for
 * task 4.5 (a batch has no latency guarantee and cannot run a tool mid-request) and 4.6 (an independent instance reviews better than the generator) is what the functions below make visible. Nothing here calls a model.
 */
private val CUSTOM_ID = Regex("^[a-zA-Z0-9_-]{1,64}$")

data class Matched(val customId: String, val kind: String)

data class Entry(val customId: String, val params: Map<String, Any>)

data class Pairing(val matched: List<Matched>, val unrequested: List<String>)

/** An item that arrives just after a submission waits one interval for the next batch, then the processing window, then the handling. */
fun worstCaseWait(intervalHours: Int, windowHours: Int = 24, handlingHours: Int = 2): Int = intervalHours + windowHours + handlingHours

/** One entry of a batch request, refused for the same reasons the API refuses it. */
fun batchEntry(customId: String, params: Map<String, Any>): Entry {
    require(CUSTOM_ID.matches(customId)) { "custom_id '$customId' must be 1 to 64 letters, digits, hyphens or underscores" }
    require(params["stream"] != true) { "stream is not supported in a batch" }
    require("speed" !in params) { "speed is not supported in a batch" }
    require(params["max_tokens"] != 0) { "a max_tokens of 0 is not supported in a batch" }
    return Entry(customId, params)
}

/** Results come back in any order: pair them with the requests by custom_id, and report a result nobody asked for. */
fun matchResults(requests: List<String>, results: List<Matched>): Pairing {
    val byId = results.associate { it.customId to it.kind }
    val asked = requests.toSet()
    return Pairing(requests.map { Matched(it, byId[it] ?: "missing") }, results.map { it.customId }.filter { it !in asked })
}

/** What the reviewing instance receives: an independent one gets the code alone, a self-review also gets the reasoning that produced it. */
fun reviewRequest(code: String, reasoning: String, independent: Boolean): String {
    val parts = mutableListOf("Review this code for defects.", "<code>$code</code>")
    if (!independent) parts += "<your_earlier_reasoning>$reasoning</your_earlier_reasoning>"
    return parts.joinToString("\n")
}

fun main() {
    for (interval in listOf(4, 6)) println("worst-case wait with a $interval hour interval: ${worstCaseWait(interval)} hours (SLA 30 hours)")
    for (customId in listOf("invoice-0042", "invoice 0042")) {
        try {
            batchEntry(customId, mapOf("max_tokens" to 1024))
            println("custom_id $customId: accepted")
        } catch (e: IllegalArgumentException) {
            println("custom_id $customId: refused, ${e.message}")
        }
    }
    for (params in listOf(mapOf<String, Any>("stream" to true), mapOf("speed" to "fast"), mapOf("max_tokens" to 0))) {
        try {
            batchEntry("a1", params)
        } catch (e: IllegalArgumentException) {
            println("refused: ${e.message}")
        }
    }
    val p = matchResults(listOf("a1", "a2", "a3"), listOf(Matched("a2", "expired"), Matched("z9", "succeeded"), Matched("a1", "succeeded")))
    println("matched: " + p.matched.joinToString(", ") { "${it.customId}=${it.kind}" } + "; unrequested: " + p.unrequested.joinToString(", "))
    val code = "total = price * qty"
    val reasoning = "qty is always positive, so no check"
    for (independent in listOf(false, true)) {
        val request = reviewRequest(code, reasoning, independent)
        println("independent=${if (independent) "yes" else "no"}: carries the reasoning: ${if (reasoning in request) "yes" else "no"}")
    }
}
