/** A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md. */

/** A failure worth retrying: a timeout, a rate limit, a lost response. */
class Transient(message: String) : RuntimeException(message)

/** A failure that retrying cannot fix. */
class Fatal(message: String) : RuntimeException(message)

typealias Agent = (String, Map<String, String>) -> String

@Suppress("UNCHECKED_CAST")
fun runPlan(plan: List<Map<String, Any?>>, agents: Map<String, Agent>, store: MutableMap<String, String>, attempts: Int, breakerThreshold: Int): Map<String, Any?>? {
    val done = linkedMapOf<String, String>()
    val failed = linkedMapOf<String, String>()
    val skipped = linkedMapOf<String, String>()
    val degraded = mutableListOf<String>()
    val resumed = mutableListOf<String>()
    val calls = linkedMapOf<String, Int>()
    val consecutive = mutableMapOf<String, Int>()

    /** The result and null, or null and a reason; every call to an agent is counted for the task. */
    fun attempt(agentName: String, key: String, inputs: Map<String, String>, taskId: String): Pair<String?, String?> {
        for (n in 1..attempts) {
            if ((consecutive[agentName] ?: 0) >= breakerThreshold) return null to "circuit open"
            calls[taskId] = calls.getValue(taskId) + 1
            try {
                val result = agents.getValue(agentName)(key, inputs)
                consecutive[agentName] = 0
                return result to null
            } catch (e: Transient) {
                consecutive[agentName] = (consecutive[agentName] ?: 0) + 1
            } catch (e: Fatal) {
                consecutive[agentName] = (consecutive[agentName] ?: 0) + 1
                return null to "fatal: ${e.message}"
            }
        }
        return null to "retries exhausted"
    }

    for (task in plan) {
        val tid = task["id"] as String
        calls[tid] = 0
        if (tid in store) {
            done[tid] = store.getValue(tid)
            resumed += tid
            continue
        }
        val needs = (task["needs"] as List<String>?) ?: emptyList()
        val missing = needs.firstOrNull { it !in done }
        if (missing != null) {
            skipped[tid] = "dependency failed: $missing"
            continue
        }
        val inputs = needs.associateWith { done.getValue(it) }
        var (result, reason) = attempt(task["agent"] as String, task["key"] as String, inputs, tid)
        if (result == null && task["fallback"] != null) {
            result = attempt(task["fallback"] as String, task["key"].toString() + ":fallback", inputs, tid).first
            if (result != null) {
                done[tid] = result
                degraded += tid
                continue
            }
        }
        if (result == null) {
            failed[tid] = reason!!
            continue
        }
        done[tid] = result
        store[tid] = result
    }
    return linkedMapOf("done" to done, "failed" to failed, "skipped" to skipped, "degraded" to degraded, "attempts" to calls, "resumed" to resumed)
}
