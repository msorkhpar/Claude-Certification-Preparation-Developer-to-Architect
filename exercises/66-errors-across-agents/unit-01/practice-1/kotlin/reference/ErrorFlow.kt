/** How a subagent's failure reaches the coordinator and the report: local recovery, structured error context, valid empty results, and coverage notes. See ../../statement.md. */

private val log = System.getLogger("error_flow")

val ALTERNATIVES = mapOf(
    "timeout" to listOf("retry later", "try a narrower query"),
    "unavailable" to listOf("use a cached source", "try another provider"),
    "permission" to listOf("request access", "use a public source"),
    "invalid_query" to listOf("rewrite the query"),
)
val TRANSIENT = listOf("timeout", "unavailable")

/** What a search tool returns: status ok with items, or status error with a type and the partial items found before it failed. */
data class Reply(val status: String, val type: String? = null, val items: List<String> = emptyList(), val partial: List<String> = emptyList())

data class Outcome(val status: String, val items: List<String> = emptyList(), val attempts: Int, val failureType: String? = null, val attempted: String? = null, val partialResults: List<String> = emptyList(), val alternatives: List<String> = emptyList())

data class Step(val topic: String, val action: String)

fun searchWithRecovery(query: String, maxAttempts: Int = 2, call: (String, Int) -> Reply): Outcome {
    log.log(System.Logger.Level.DEBUG, "searchWithRecovery input: {0}", query)
    var attempts = 0
    while (true) {
        attempts += 1
        val reply = call(query, attempts)
        if (reply.status == "ok") return Outcome(if (reply.items.isEmpty()) "empty" else "success", reply.items, attempts)
        val kind = reply.type!!
        if (kind in TRANSIENT && attempts < maxAttempts) continue
        return Outcome("failed", emptyList(), attempts, kind, query, reply.partial, ALTERNATIVES[kind] ?: emptyList())
    }
}

fun coordinatorPlan(results: Map<String, Outcome>): List<Step> = results.map { (topic, o) ->
    Step(topic, when {
        o.status == "success" -> "use"
        o.status == "empty" -> "no_findings"
        o.partialResults.isNotEmpty() -> "use_partial"
        o.alternatives.isNotEmpty() -> "try_alternative"
        else -> "flag_gap"
    })
}

fun coverageNote(results: Map<String, Outcome>, topics: List<String>): String {
    val groups = linkedMapOf("Well-supported" to mutableListOf<String>(), "Partial" to mutableListOf(), "No findings" to mutableListOf(), "Gaps" to mutableListOf())
    for (topic in topics) {
        val o = results[topic]
        when {
            o == null -> groups.getValue("Gaps") += "$topic (not searched)"
            o.status == "success" -> groups.getValue("Well-supported") += topic
            o.status == "empty" -> groups.getValue("No findings") += topic
            o.partialResults.isNotEmpty() -> groups.getValue("Partial") += "$topic (${o.failureType})"
            else -> groups.getValue("Gaps") += "$topic (${o.failureType}: ${o.attempted})"
        }
    }
    return groups.filter { it.value.isNotEmpty() }.map { "${it.key}: ${it.value.joinToString(", ")}" }.joinToString("\n")
}
