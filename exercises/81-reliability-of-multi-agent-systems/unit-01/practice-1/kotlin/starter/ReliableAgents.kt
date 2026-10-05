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

/**
 * TODO 1 of 7 (unlocks e4): is the breaker of this agent open?
 * Receives the map of consecutive failures per agent, the agent name and the threshold. Returns true when the agent's count has reached the threshold
 * (a name that is not in the map has a count of 0). Example: breakerOpen(mapOf("w" to 3), "w", 3) -> true, breakerOpen(emptyMap(), "w", 3) -> false
 */
fun breakerOpen(consecutive: Map<String, Int>, name: String, threshold: Int): Boolean = false

/**
 * TODO 2 of 7 (unlocks e4): update the consecutive-failure count of an agent after a call.
 * Receives the map of consecutive failures, the agent name and whether the call succeeded. Sets the count to 0 on a success and adds one on a failure.
 * Example: after a failure then a success then a failure, the count is 1
 */
fun recordOutcome(consecutive: MutableMap<String, Int>, name: String, ok: Boolean) {}

/**
 * TODO 3 of 7 (unlocks m1, e1, e2 and e4): call an agent for a task, retrying a transient failure.
 * Receives the agents, the agent name, the task's key and inputs, the per-task call counts, the task id, the breaker counts, the attempt limit and the breaker
 * threshold. Up to `attempts` times: if the breaker is open return null to "circuit open" without calling; otherwise add one to `calls[taskId]`, make the
 * call with `callOnce` (always the same key), record the outcome and return result to null on "ok"; return null to "fatal: <message>" on "fatal" without
 * retrying; retry on "transient". After the last attempt return null to "retries exhausted".
 * Example: an agent that fails once with Transient and then answers "ok" -> "ok" to null with two calls counted
 */
fun attempt(
    agents: Map<String, Agent>, name: String, key: String, inputs: Map<String, String>, calls: MutableMap<String, Int>, taskId: String,
    consecutive: MutableMap<String, Int>, attempts: Int, threshold: Int,
): Pair<String?, String?> = null to "retries exhausted"

/**
 * TODO 4 of 7 (unlocks e3): the first dependency that did not finish.
 * Receives the ids a task needs and the map of finished results. Returns the first id in `needs` (in order) that is not in `done`, or null when all are.
 * Example: missingDependency(listOf("a", "b"), mapOf("a" to "x")) -> "b"
 */
fun missingDependency(needs: List<String>, done: Map<String, String>): String? = null

/**
 * TODO 5 of 7 (unlocks e5): the idempotency key for the fallback agent.
 * Receives the task's key. Returns it followed by `:fallback`, so that a repeat of the fallback is recognised without colliding with the primary.
 * Example: fallbackKey("key-s") -> "key-s:fallback"
 */
fun fallbackKey(key: String): String = key

/**
 * TODO 6 of 7 (unlocks e6): is this task already checkpointed?
 * Receives a task id and the store of checkpointed results. Returns true when the store holds the task, so that no agent is called for it.
 * Example: shouldResume("a", mapOf("a" to "ok")) -> true
 */
fun shouldResume(tid: String, store: Map<String, String>): Boolean = false

/**
 * TODO 7 of 7 (unlocks m1, e5, e6 and e7): write a finished task's result to the store.
 * Receives the store, the task id, the result and whether the result is degraded (it came from the fallback). Writes the result to the store unless it is
 * degraded, so that a later run tries the primary again. It is called as soon as each task finishes, so a crash later in the run loses nothing finished.
 * Example: checkpoint(store, "a", "ok", false) -> store is {a=ok}; with degraded true the store is unchanged
 */
fun checkpoint(store: MutableMap<String, String>, tid: String, result: String, degraded: Boolean) {}

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
