/** A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md. */
private val log = System.getLogger("reliable_agents")

/** A failure worth retrying: a timeout, a rate limit, a lost response. */
class Transient(message: String) : RuntimeException(message)

/** A failure that retrying cannot fix. */
class Fatal(message: String) : RuntimeException(message)

typealias Agent = (String, Map<String, String>) -> String

/** One call to an agent: ("ok", result), ("transient", null) or ("fatal", message). Any other exception is a crash and is not caught. */
fun callOnce(agents: Map<String, Agent>, name: String, key: String, inputs: Map<String, String>): Pair<String, String?> =
    try {
        "ok" to agents.getValue(name)(key, inputs)
    } catch (e: Transient) {
        "transient" to null
    } catch (e: Fatal) {
        "fatal" to e.message
    }

/** True when the agent has failed `threshold` calls in a row. */
fun breakerOpen(consecutive: Map<String, Int>, name: String, threshold: Int): Boolean = (consecutive[name] ?: 0) >= threshold

/** A success resets the agent's count to zero; a failure adds one. */
fun recordOutcome(consecutive: MutableMap<String, Int>, name: String, ok: Boolean) {
    consecutive[name] = if (ok) 0 else (consecutive[name] ?: 0) + 1
}

/** The result and null, or null and a reason; every call to an agent is counted for the task. */
fun attempt(
    agents: Map<String, Agent>, name: String, key: String, inputs: Map<String, String>, calls: MutableMap<String, Int>, taskId: String,
    consecutive: MutableMap<String, Int>, attempts: Int, threshold: Int,
): Pair<String?, String?> {
    for (n in 1..attempts) {
        if (breakerOpen(consecutive, name, threshold)) return null to "circuit open"
        calls[taskId] = calls.getValue(taskId) + 1
        val (status, value) = callOnce(agents, name, key, inputs)
        recordOutcome(consecutive, name, status == "ok")
        if (status == "ok") return value to null
        if (status == "fatal") return null to "fatal: $value"
    }
    return null to "retries exhausted"
}

/** The first id in `needs` that is not done, or null. */
fun missingDependency(needs: List<String>, done: Map<String, String>): String? = needs.firstOrNull { it !in done }

/** The key the fallback agent receives. */
fun fallbackKey(key: String): String = key + ":fallback"

/** True when the store already holds this task's result. */
fun shouldResume(tid: String, store: Map<String, String>): Boolean = tid in store

/** Write a final result to the store as soon as the task finishes; a degraded result is not final. */
fun checkpoint(store: MutableMap<String, String>, tid: String, result: String, degraded: Boolean) {
    if (!degraded) store[tid] = result
}

@Suppress("UNCHECKED_CAST")
fun runPlan(plan: List<Map<String, Any?>>, agents: Map<String, Agent>, store: MutableMap<String, String>, attempts: Int, breakerThreshold: Int): Map<String, Any?>? {
    log.log(System.Logger.Level.DEBUG, "runPlan input: {0}", plan)
    val done = linkedMapOf<String, String>()
    val failed = linkedMapOf<String, String>()
    val skipped = linkedMapOf<String, String>()
    val degraded = mutableListOf<String>()
    val resumed = mutableListOf<String>()
    val calls = linkedMapOf<String, Int>()
    val consecutive = mutableMapOf<String, Int>()

    for (task in plan) {
        val tid = task["id"] as String
        calls[tid] = 0
        if (shouldResume(tid, store)) {
            done[tid] = store.getValue(tid)
            resumed += tid
            continue
        }
        val needs = (task["needs"] as List<String>?) ?: emptyList()
        val missing = missingDependency(needs, done)
        if (missing != null) {
            skipped[tid] = "dependency failed: $missing"
            continue
        }
        val inputs = needs.associateWith { done[it] ?: "" }
        var (result, reason) = attempt(agents, task["agent"] as String, task["key"] as String, inputs, calls, tid, consecutive, attempts, breakerThreshold)
        if (result == null && task["fallback"] != null) {
            result = attempt(agents, task["fallback"] as String, fallbackKey(task["key"] as String), inputs, calls, tid, consecutive, attempts, breakerThreshold).first
            if (result != null) {
                done[tid] = result
                degraded += tid
                checkpoint(store, tid, result, true)
                continue
            }
        }
        if (result == null) {
            failed[tid] = reason!!
            continue
        }
        done[tid] = result
        checkpoint(store, tid, result, false)
    }
    return linkedMapOf("done" to done, "failed" to failed, "skipped" to skipped, "degraded" to degraded, "attempts" to calls, "resumed" to resumed)
}
