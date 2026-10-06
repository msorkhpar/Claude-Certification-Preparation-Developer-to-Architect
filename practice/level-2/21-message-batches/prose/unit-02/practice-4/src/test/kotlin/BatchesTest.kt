import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BatchesTest {
    private val model = "claude-haiku-4-5-20251001"

    private fun item(id: String, vararg extra: Pair<String, Any?>): Map<String, Any?> {
        val params = linkedMapOf<String, Any?>("model" to model, "max_tokens" to 200, "messages" to listOf(mapOf("role" to "user", "content" to "Classify ticket $id")))
        extra.forEach { params[it.first] = it.second }
        return mapOf("id" to id, "params" to params)
    }

    private fun line(customId: String, result: Map<String, Any?>) = Json.stringify(mapOf("custom_id" to customId, "result" to result))

    private fun message(text: String, input: Int = 50, output: Int = 7, vararg extraUsage: Pair<String, Any?>): Map<String, Any?> {
        val usage = linkedMapOf<String, Any?>("input_tokens" to input, "output_tokens" to output)
        extraUsage.forEach { usage[it.first] = it.second }
        return mapOf("type" to "succeeded", "message" to mapOf("id" to "msg_illustrative", "type" to "message", "role" to "assistant", "model" to model,
            "stop_reason" to "end_turn", "content" to listOf(mapOf("type" to "text", "text" to text)), "usage" to usage))
    }

    private fun errored(kind: String): Map<String, Any?> = mapOf("type" to "errored", "error" to mapOf("type" to "error", "error" to mapOf("type" to kind, "message" to "x")))

    /** The BatchError field the call throws, "crash" for another exception, null when it returns. */
    private fun failureOf(call: () -> Any?): String? = try {
        call()
        null
    } catch (e: BatchError) {
        e.field
    } catch (e: RuntimeException) {
        "crash"
    }

    private fun built(vararg items: Map<String, Any?>) = buildRequests(items.toList()) ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun field(done: Map<String, Any?>?, key: String): List<Any?> = if (done == null) listOf("no result") else done[key] as List<Any?>

    private fun statuses(done: Map<String, Any?>?): List<Any?> = if (done == null) emptyList() else field(done, "outcomes").map { (it as Map<*, *>)["status"] }

    @Test
    fun m1_resultsAreMatchedToRequestsByCustomIdNotByPosition() {
        val requests = built(item("t-1"), item("t-2"), item("t-3"))
        assertEquals(listOf("t-1", "t-2", "t-3"), requests.map { it["custom_id"] })
        assertEquals(200, ((requests[0]["params"]) as Map<*, *>)["max_tokens"])
        val done = collect(requests, listOf(line("t-3", message("billing")), line("t-1", message("refund")), line("t-2", message("shipping"))))
        val got = field(done, "outcomes").map { val m = it as Map<*, *>; "${m["custom_id"]}/${m["status"]}/${m["text"]}" }
        assertEquals(listOf("t-1/succeeded/refund", "t-2/succeeded/shipping", "t-3/succeeded/billing"), got)
        assertEquals(listOf(emptyList<Any?>(), emptyList<Any?>(), emptyList<Any?>()), listOf(field(done, "retry"), field(done, "fix"), field(done, "unknown")))
    }

    @Test
    fun e1_aCustomIdIs1To64SafeCharactersAndUnique() {
        for (bad in listOf("", "has space", "dot.dot", "x".repeat(65), "naïve")) {
            assertEquals("custom_id", failureOf { buildRequests(listOf(item(bad))) }, bad)
        }
        assertEquals("custom_id", failureOf { buildRequests(listOf(item("a"), item("a"))) })
        assertNull(failureOf { buildRequests(listOf(item("x".repeat(64)), item("A_b-9"))) })
    }

    @Test
    fun e2_parametersABatchCannotTakeAreRefused() {
        assertEquals("params.stream", failureOf { buildRequests(listOf(item("a", "stream" to true))) })
        assertEquals("params.speed", failureOf { buildRequests(listOf(item("a", "speed" to "fast"))) })
        assertEquals("params.max_tokens", failureOf { buildRequests(listOf(mapOf("id" to "a", "params" to mapOf("model" to model, "max_tokens" to 0, "messages" to emptyList<Any?>())))) })
        assertNull(failureOf { buildRequests(listOf(item("a", "stream" to false))) })
    }

    @Test
    fun e3_aBigJobIsCutInOrderByRequestCountAndBySize() {
        val requests = built(*(0 until 7).map { item("r$it") }.toTypedArray())
        val byCount = splitBatches(requests, 3, 1L shl 40)
        assertEquals(listOf(listOf("r0", "r1", "r2"), listOf("r3", "r4", "r5"), listOf("r6")), byCount?.map { b -> b.map { it["custom_id"] } } ?: emptyList<List<Any?>>())
        val size = if (requests.isEmpty()) 0L else Json.stringify(requests[0]).toByteArray(Charsets.UTF_8).size.toLong()
        val bySize = splitBatches(requests, 100_000, size * 2 + 5)
        assertEquals(listOf(2, 2, 2, 1), bySize?.map { it.size } ?: emptyList<Int>())
        assertEquals((0 until 7).map { "r$it" }, bySize?.flatten()?.map { it["custom_id"] } ?: emptyList<Any?>())
        assertEquals(0, splitBatches(emptyList())?.size ?: -1)
        assertEquals("size", failureOf { splitBatches(requests, 100_000, size - 1) })
    }

    @Test
    fun e4_invalidRequestsAreFixedAndTheRestAreRetried() {
        val requests = built(item("ok"), item("bad"), item("busy"), item("late"), item("stopped"))
        val done = collect(requests, listOf(line("ok", message("fine")), line("bad", errored("invalid_request_error")), line("busy", errored("overloaded_error")),
            line("late", mapOf("type" to "expired")), line("stopped", mapOf("type" to "canceled"))))
        assertEquals(listOf("bad"), field(done, "fix"))
        assertEquals(listOf("busy", "late", "stopped"), field(done, "retry"))
        assertEquals(listOf("succeeded", "errored", "errored", "expired", "canceled"), statuses(done))
        assertEquals("invalid_request_error", if (done == null) null else (field(done, "outcomes")[1] as Map<*, *>)["error_type"])
    }

    @Test
    fun e5_aRequestWithNoResultIsMissingAndAStrangerIsReported() {
        val requests = built(item("a"), item("b"), item("c"))
        val done = collect(requests, listOf(line("c", message("three")), "", line("zzz", message("who")), line("a", message("one"))))
        assertEquals(listOf("succeeded", "missing", "succeeded"), statuses(done))
        assertEquals(listOf("b"), field(done, "retry"))
        assertEquals(listOf("zzz"), field(done, "unknown"))
        assertEquals(3, statuses(done).size)
    }

    @Test
    fun e6_onlyRequestsThatSucceededCountTowardUsage() {
        val requests = built(item("a"), item("b"), item("c"))
        val done = collect(requests, listOf(line("a", message("x", 100, 10, "cache_read_input_tokens" to 400)), line("b", errored("api_error")),
            line("c", message("y", 30, 5, "cache_creation_input_tokens" to 200))))
        assertEquals(mapOf("input_tokens" to 130L, "output_tokens" to 15L, "cache_creation_input_tokens" to 200L, "cache_read_input_tokens" to 400L), done?.get("usage"))
    }
}
