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
        // TODO 2 of 6 (finish this to pass e1): the status of an ok reply. Return success when the reply holds items and
        //   empty when it holds none (a search that found nothing is a valid answer, not a failure). Example: ok with []
        //   -> empty.
        if (reply.status == "ok") return Outcome("success", reply.items, attempts)
        val kind = reply.type!!
        // TODO 1 of 6 (finish this to pass m1): the local retry. When the failure type is one of TRANSIENT and the
        //   attempts so far are below max_attempts, try again; otherwise give up. Example: timeout then ok, limit 2 ->
        //   success with attempts 2.
        // TODO 3 of 6 (finish this to pass e2, e3): the failed outcome. Return status failed with the failure type, the
        //   query that was attempted, the attempts, the partial results the reply carried (none when absent) and the
        //   alternatives listed for that failure type (none when unknown). Example: permission error -> alternatives
        //   [request access, use a public source].
        return Outcome("failed", emptyList(), attempts, kind, query, emptyList(), emptyList())
    }
}

fun coordinatorPlan(results: Map<String, Outcome>): List<Step> = results.map { (topic, o) ->
    Step(topic, when {
        o.status == "success" -> "use"
        o.status == "empty" -> "no_findings"
        // TODO 4 of 6 (finish this to pass e4): the rows for a failed topic. When the outcome has partial results, the
        //   action is use_partial; otherwise try_alternative when it has alternatives; otherwise flag_gap. Example:
        //   failed, no partial, alternatives [rewrite the query] -> try_alternative.
        else -> "flag_gap"
    })
}

fun coverageNote(results: Map<String, Outcome>, topics: List<String>): String {
    val groups = linkedMapOf("Well-supported" to mutableListOf<String>(), "Partial" to mutableListOf(), "No findings" to mutableListOf(), "Gaps" to mutableListOf())
    for (topic in topics) {
        val o = results[topic]
        when {
            // TODO 6 of 6 (finish this to pass e6): the topic with no result. When a requested topic has no outcome at
            //   all, list it under Gaps as "topic (not searched)". Example: topics [a, b], results only for a -> Gaps: b
            //   (not searched).
            o == null -> continue
            o.status == "success" -> groups.getValue("Well-supported") += topic
            o.status == "empty" -> groups.getValue("No findings") += topic
            // TODO 5 of 6 (finish this to pass e5): the groups of a failed topic in the coverage note. With partial
            //   results, list the topic under Partial as "topic (failure type)"; without, under Gaps as "topic (failure
            //   type: query attempted)". Example: partial, timeout -> Partial: topic (timeout).
            else -> groups.getValue("Gaps") += topic
        }
    }
    return groups.filter { it.value.isNotEmpty() }.map { "${it.key}: ${it.value.joinToString(", ")}" }.joinToString("\n")
}
