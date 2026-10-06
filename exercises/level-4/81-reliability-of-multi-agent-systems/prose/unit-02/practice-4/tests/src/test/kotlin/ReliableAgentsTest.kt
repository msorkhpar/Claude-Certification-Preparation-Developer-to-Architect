import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReliableAgentsTest {
    private fun task(id: String, agent: String = "w", needs: List<String> = emptyList(), fallback: String? = null): Map<String, Any?> =
        linkedMapOf("id" to id, "agent" to agent, "key" to "key-$id", "needs" to needs, "fallback" to fallback)

    private val echo: Agent = { key, inputs -> key + "|" + inputs.toSortedMap().values.joinToString(",") }

    private fun run(plan: List<Map<String, Any?>>, agents: Map<String, Agent>, store: MutableMap<String, String>, attempts: Int = 3, threshold: Int = 3): Map<String, Any?> {
        val result = runPlan(plan, agents, store, attempts, threshold)
        assertNotNull(result, "runPlan returned nothing")
        return result!!
    }

    @Test
    fun m1_tasksRunInOrderAndReceiveTheResultsOfTheTasksTheyNeed() {
        val store = linkedMapOf<String, String>()
        val result = run(listOf(task("a"), task("b", needs = listOf("a")), task("c", needs = listOf("a", "b"))), mapOf("w" to echo), store)
        val expected = mapOf("a" to "key-a|", "b" to "key-b|key-a|", "c" to "key-c|key-a|,key-b|key-a|")
        assertEquals(expected, result["done"])
        assertEquals(mapOf("a" to 1, "b" to 1, "c" to 1), result["attempts"])
        assertEquals(emptyMap<String, String>(), result["failed"])
        assertEquals(emptyMap<String, String>(), result["skipped"])
        assertEquals(emptyList<String>(), result["degraded"])
        assertEquals(emptyList<String>(), result["resumed"])
        assertEquals(expected, store)
    }

    @Test
    fun e1_everyRetryOfATaskCarriesTheSameIdempotencyKey() {
        val seen = mutableListOf<String>()
        val flaky: Agent = { key, _ ->
            seen += key
            if (seen.size < 3) throw Transient("lost")
            "ok"
        }
        val result = run(listOf(task("t")), mapOf("w" to flaky), linkedMapOf())
        assertEquals(listOf("key-t", "key-t", "key-t"), seen)
        assertEquals(mapOf("t" to "ok"), result["done"])
        assertEquals(mapOf("t" to 3), result["attempts"])
    }

    @Test
    fun e2_retriesStopAtTheLimitAndAFatalFailureIsNotRetried() {
        val down: Agent = { _, _ -> throw Transient("down") }
        val bad: Agent = { _, _ -> throw Fatal("bad input") }
        val result = run(listOf(task("a"), task("b", agent = "x")), mapOf("w" to down, "x" to bad), linkedMapOf(), 3, 99)
        assertEquals(mapOf("a" to "retries exhausted", "b" to "fatal: bad input"), result["failed"])
        assertEquals(mapOf("a" to 3, "b" to 1), result["attempts"])
    }

    @Test
    fun e3_aFailureStaysInsideItsBranchAndDependentsAreSkipped() {
        val agent: Agent = { key, _ -> if (key == "key-a") throw Fatal("boom") else "ok" }
        val result = run(listOf(task("a"), task("b", needs = listOf("a")), task("c"), task("d", needs = listOf("b"))), mapOf("w" to agent), linkedMapOf())
        assertEquals(mapOf("c" to "ok"), result["done"])
        assertEquals(mapOf("a" to "fatal: boom"), result["failed"])
        assertEquals(mapOf("b" to "dependency failed: a", "d" to "dependency failed: b"), result["skipped"])
        assertEquals(mapOf("a" to 1, "b" to 0, "c" to 1, "d" to 0), result["attempts"])
    }

    @Test
    fun e4_aBreakerStopsCallsToAnAgentThatKeepsFailingAndASuccessResetsIt() {
        val calls = mutableListOf<String>()
        val flaky: Agent = { key, _ ->
            calls += key
            throw Transient("down")
        }
        val good: Agent = { _, _ -> "fine" }
        val plan = listOf(task("t1", agent = "flaky"), task("t2", agent = "flaky"), task("t3", agent = "flaky"), task("t4", agent = "good"))
        val result = run(plan, mapOf("flaky" to flaky, "good" to good), linkedMapOf(), 2, 3)
        assertEquals(mapOf("t1" to "retries exhausted", "t2" to "circuit open", "t3" to "circuit open"), result["failed"])
        assertEquals(mapOf("t4" to "fine"), result["done"])
        assertEquals(mapOf("t1" to 2, "t2" to 1, "t3" to 0, "t4" to 1), result["attempts"])
        assertEquals(3, calls.size)

        val count = mutableListOf<String>()
        val everyOther: Agent = { key, _ ->
            count += key
            if (count.size % 2 == 1) throw Transient("blip")
            "ok"
        }
        val healthy = run(listOf(task("a", agent = "x"), task("b", agent = "x")), mapOf("x" to everyOther), linkedMapOf(), 2, 2)
        assertEquals(mapOf("a" to "ok", "b" to "ok"), healthy["done"])
        assertEquals(emptyMap<String, String>(), healthy["failed"])
    }

    @Test
    fun e5_aFallbackDegradesOneTaskWithItsOwnKeyAndIsNotCheckpointed() {
        val seen = mutableListOf<String>()
        val primary: Agent = { _, _ -> throw Fatal("down") }
        val backup: Agent = { key, _ ->
            seen += key
            "from backup"
        }
        val use: Agent = { _, inputs -> "got " + inputs["s"] }
        val store = linkedMapOf<String, String>()
        val plan = listOf(task("s", agent = "primary", fallback = "backup"), task("t", agent = "use", needs = listOf("s")))
        val result = run(plan, mapOf("primary" to primary, "backup" to backup, "use" to use), store)
        assertEquals(mapOf("s" to "from backup", "t" to "got from backup"), result["done"])
        assertEquals(listOf("s"), result["degraded"])
        assertEquals(emptyMap<String, String>(), result["failed"])
        assertEquals(listOf("key-s:fallback"), seen)
        assertEquals(setOf("t"), store.keys)
        val bothDown = run(listOf(task("s", agent = "primary", fallback = "primary")), mapOf("primary" to primary), linkedMapOf())
        assertEquals(mapOf("s" to "fatal: down"), bothDown["failed"])
        assertEquals(emptyList<String>(), bothDown["degraded"])
    }

    @Test
    fun e6_aSecondRunResumesFromTheCheckpointAndRetriesOnlyWhatFailed() {
        val calls = mutableListOf<String>()
        var healthy = false
        val agent: Agent = { key, _ ->
            calls += key
            if (key == "key-b" && !healthy) throw Transient("down")
            "ok-$key"
        }
        val store = linkedMapOf<String, String>()
        val plan = listOf(task("a"), task("b", needs = listOf("a")))
        val first = run(plan, mapOf("w" to agent), store, 2, 99)
        assertEquals(mapOf("b" to "retries exhausted"), first["failed"])
        assertEquals(setOf("a"), store.keys)
        healthy = true
        val second = run(plan, mapOf("w" to agent), store, 2, 99)
        assertEquals(listOf("a"), second["resumed"])
        assertEquals(mapOf("a" to "ok-key-a", "b" to "ok-key-b"), second["done"])
        assertEquals(mapOf("a" to 0, "b" to 1), second["attempts"])
        assertEquals(1, calls.count { it == "key-a" })
    }

    @Test
    fun e7_anUnexpectedCrashIsNotSwallowedAndKeepsTheWorkAlreadyCheckpointed() {
        val agent: Agent = { key, _ -> if (key == "key-b") throw IllegalStateException("process died") else "ok" }
        val store = linkedMapOf<String, String>()
        assertThrows(IllegalStateException::class.java) { run(listOf(task("a"), task("b"), task("c")), mapOf("w" to agent), store) }
        assertEquals(mapOf("a" to "ok"), store)
    }
}
