/** How a subagent's failure reaches the coordinator and the report: local recovery, structured error context, valid empty results, and coverage notes. See ../../statement.md. */

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

fun searchWithRecovery(query: String, maxAttempts: Int = 2, call: (String, Int) -> Reply): Outcome? {
    // TODO: call(query, attempt) until it succeeds, fails for good or the attempts run out; return the outcome.
    return null
}

fun coordinatorPlan(results: Map<String, Outcome>): List<Step>? {
    // TODO: a step for every topic of results, in order; the run is never stopped.
    return null
}

fun coverageNote(results: Map<String, Outcome>, topics: List<String>): String? {
    // TODO: the coverage annotation for the report: which topics are well supported, partial, without findings or gaps.
    return null
}
