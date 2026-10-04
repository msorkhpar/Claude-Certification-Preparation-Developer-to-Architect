/** A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md. */

/** A failure worth retrying: a timeout, a rate limit, a lost response. */
class Transient(message: String) : RuntimeException(message)

/** A failure that retrying cannot fix. */
class Fatal(message: String) : RuntimeException(message)

typealias Agent = (String, Map<String, String>) -> String

fun runPlan(plan: List<Map<String, Any?>>, agents: Map<String, Agent>, store: MutableMap<String, String>, attempts: Int, breakerThreshold: Int): Map<String, Any?>? {
    // TODO: run the tasks in order and return a map with done, failed, skipped, degraded, attempts and resumed.
    return null
}
