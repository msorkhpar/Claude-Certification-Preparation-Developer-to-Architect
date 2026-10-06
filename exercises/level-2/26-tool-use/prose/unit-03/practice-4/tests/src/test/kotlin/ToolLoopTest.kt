import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolLoopTest {
    private fun text(t: String): Map<String, Any?> = mapOf("type" to "text", "text" to t)
    private fun toolUse(id: String, name: String, vararg input: Pair<String, Any?>): Map<String, Any?> = mapOf("type" to "tool_use", "id" to id, "name" to name, "input" to linkedMapOf(*input))
    private fun reply(content: List<Map<String, Any?>>, stopReason: String = "end_turn"): Map<String, Any?> = mapOf("id" to "msg_illustrative", "type" to "message", "role" to "assistant",
        "model" to "claude-sonnet-5-5", "content" to content, "stop_reason" to stopReason, "usage" to mapOf("input_tokens" to 1, "output_tokens" to 1))

    /** A hand-written, illustrative model: returns the next reply and records a copy of each request. */
    private class Scripted(vararg replies: Map<String, Any?>) : (Map<String, Any?>) -> Map<String, Any?> {
        val replies = replies.toMutableList()
        val seen = mutableListOf<Map<String, Any?>>()
        @Suppress("UNCHECKED_CAST")
        override fun invoke(request: Map<String, Any?>): Map<String, Any?> {
            seen += Json.parse(Json.stringify(request)) as Map<String, Any?>
            return replies.removeAt(0)
        }
    }

    private val calls = mutableListOf<String>()
    private val schema: Map<String, Any?> = mapOf("type" to "object", "properties" to mapOf("city" to mapOf("type" to "string")), "required" to listOf("city"))

    private fun tools(): List<Tool> = listOf(
        Tool("get_weather", "Current weather for a city.", schema) { a -> calls += "get_weather ${Json.stringify(a)}"; "${a["city"]}: 18 C" },
        Tool("get_time", "Local time for a city.", schema) { a -> calls += "get_time ${Json.stringify(a)}"; "${a["city"]}: 14:05" },
        Tool("broken", "Always fails.", schema) { _ -> throw ToolError("the weather service is down") },
        Tool("structured", "Returns an object.", schema) { a -> mapOf("city" to a["city"], "temp_c" to 18L, "tags" to listOf("mild")) })

    /** The RequestError field the call throws, "crash" for another exception, null when it returns. */
    private fun failureOf(call: () -> Any?): String? = try {
        call()
        null
    } catch (e: RequestError) {
        e.field
    } catch (e: RuntimeException) {
        "crash"
    }

    /** The parsed JSON, or a marker string when the text is not JSON. */
    private fun jsonOrText(text: String): Any? = try {
        Json.parse(text)
    } catch (e: IllegalArgumentException) {
        "not JSON: $text"
    }

    @Suppress("UNCHECKED_CAST")
    private fun messagesOf(m: Scripted, call: Int): List<Map<String, Any?>> = if (call < m.seen.size) m.seen[call]["messages"] as List<Map<String, Any?>> else emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun contentOf(messages: List<Map<String, Any?>>, index: Int): List<Map<String, Any?>> = if (index < messages.size) messages[index]["content"] as List<Map<String, Any?>> else emptyList()

    private fun at(result: Map<String, Any?>?, key: String): Any? = if (result == null) "no result" else result[key]
    private fun roles(messages: List<Map<String, Any?>>) = messages.map { it["role"] }
    private fun run(m: Scripted, model: String = "claude-sonnet-5-5", maxTurns: Int = 8, choice: Map<String, Any?>? = null) = runAgent(m, tools(), "go", model, maxTurns, choice)

    @Test
    fun m1_aToolCallIsRunAndItsResultSentBackUntilTheModelEndsItsTurn() {
        calls.clear()
        val first = listOf(text("Let me check."), toolUse("tu_1", "get_weather", "city" to "Oslo"))
        val m = Scripted(reply(first, "tool_use"), reply(listOf(text("It is 18 C in Oslo."))))
        val r = runAgent(m, tools(), "Weather in Oslo?")
        assertEquals(listOf("done", "It is 18 C in Oslo.", 2), listOf(at(r, "status"), at(r, "text"), at(r, "turns")))
        assertEquals(listOf("get_weather {\"city\":\"Oslo\"}"), calls)
        val second = messagesOf(m, 1)
        assertEquals(listOf("user", "assistant", "user"), roles(second))
        assertEquals(first, contentOf(second, 1))
        assertEquals(listOf(mapOf("type" to "tool_result", "tool_use_id" to "tu_1", "content" to "Oslo: 18 C")), contentOf(second, 2))
        val sent = m.seen[0]
        assertEquals("claude-sonnet-5-5", sent["model"])
        @Suppress("UNCHECKED_CAST") val defs = sent["tools"] as List<Map<String, Any?>>
        assertEquals(listOf("get_weather", "get_time", "broken", "structured"), defs.map { it["name"] })
        assertTrue(defs.all { !it.containsKey("handler") } && !sent.containsKey("tool_choice"))
    }

    @Test
    fun e1_parallelCallsGetOneUserMessageWithEveryResultInOrder() {
        calls.clear()
        val content = listOf(text("Checking both."), mapOf("type" to "server_tool_use", "id" to "srvtoolu_1", "name" to "web_search", "input" to mapOf("query" to "x")),
            toolUse("tu_a", "get_time", "city" to "Rome"), toolUse("tu_b", "get_weather", "city" to "Rome"))
        val m = Scripted(reply(content, "tool_use"), reply(listOf(text("done"))))
        runAgent(m, tools(), "Rome?")
        val second = messagesOf(m, 1)
        assertEquals(listOf("user", "assistant", "user"), roles(second))
        assertEquals(content, contentOf(second, 1))
        assertEquals(listOf("tool_result/tu_a/Rome: 14:05", "tool_result/tu_b/Rome: 18 C"), contentOf(second, 2).map { "${it["type"]}/${it["tool_use_id"]}/${it["content"]}" })
        assertEquals(listOf("get_time {\"city\":\"Rome\"}", "get_weather {\"city\":\"Rome\"}"), calls)
    }

    @Test
    fun e2_aFailingUnknownOrMalformedCallBecomesAnErrorResultAndTheLoopGoesOn() {
        calls.clear()
        val batch = listOf(toolUse("t1", "broken", "city" to "Oslo"), toolUse("t2", "teleport", "city" to "Oslo"), toolUse("t3", "get_weather"), toolUse("t4", "get_time", "city" to "Oslo"))
        val m = Scripted(reply(batch, "tool_use"), reply(listOf(text("recovered"))))
        val r = runAgent(m, tools(), "go")
        assertEquals(listOf("done", "recovered"), listOf(at(r, "status"), at(r, "text")))
        val results = contentOf(messagesOf(m, 1), 2)
        assertEquals(listOf("t1/true", "t2/true", "t3/true", "t4/false"), results.map { "${it["tool_use_id"]}/${it["is_error"] ?: false}" })
        assertTrue(results[0]["content"].toString().contains("weather service is down"))
        assertTrue(results[1]["content"].toString().contains("teleport") && results[2]["content"].toString().contains("city"))
        assertEquals(listOf("get_time {\"city\":\"Oslo\"}"), calls)
    }

    @Test
    fun e3_theNumberOfTurnsIsBounded() {
        val m = Scripted(*Array(10) { i -> reply(listOf(toolUse("tu_$i", "get_time", "city" to "Oslo")), "tool_use") })
        val r = run(m, maxTurns = 3)
        assertEquals(listOf("max_turns", 3), listOf(at(r, "status"), at(r, "turns")))
        assertEquals(3, m.seen.size)
    }

    @Test
    fun e4_refusalAndTruncationEndTheLoopAndAPausedTurnContinues() {
        val refused = run(Scripted(reply(listOf(text("I can't help.")), "refusal")))
        assertEquals(listOf("refused", 1), listOf(at(refused, "status"), at(refused, "turns")))
        val cut = run(Scripted(reply(listOf(text("The answer is")), "max_tokens")))
        assertEquals(listOf("truncated", "The answer is"), listOf(at(cut, "status"), at(cut, "text")))
        val paused = listOf(mapOf("type" to "server_tool_use", "id" to "srvtoolu_1", "name" to "web_search", "input" to mapOf("query" to "x")))
        val m = Scripted(reply(paused, "pause_turn"), reply(listOf(text("found it"))))
        val r = run(m)
        assertEquals(listOf("done", "found it", 2), listOf(at(r, "status"), at(r, "text"), at(r, "turns")))
        val again = messagesOf(m, 1)
        assertEquals(listOf("user", "assistant"), roles(again))
        assertEquals(paused, contentOf(again, 1))
    }

    @Test
    fun e5_toolChoiceIsValidatedForTheModelAndAForcedChoiceAppliesToTheFirstRequestOnly() {
        for (choice in listOf(mapOf("type" to "any"), mapOf("type" to "tool", "name" to "get_time"))) {
            for (modelId in listOf("claude-sonnet-5-5", "claude-opus-5-5", "claude-fable-5-1")) {
                assertEquals("tool_choice", failureOf { run(Scripted(reply(listOf(text("x")))), modelId, 8, choice) }, "$choice $modelId")
            }
        }
        assertEquals("tool_choice.type", failureOf { run(Scripted(reply(listOf(text("x")))), choice = mapOf("type" to "maybe")) })
        assertEquals("tool_choice.name", failureOf { run(Scripted(reply(listOf(text("x")))), "claude-opus-5", 8, mapOf("type" to "tool", "name" to "nope")) })
        assertEquals("tool_choice.disable_parallel_tool_use", failureOf { run(Scripted(reply(listOf(text("x")))), choice = mapOf("type" to "auto", "disable_parallel_tool_use" to "yes")) })
        val m = Scripted(reply(listOf(toolUse("tu_1", "get_time", "city" to "Oslo")), "tool_use"), reply(listOf(text("ok"))))
        val r = run(m, "claude-opus-5", 8, mapOf("type" to "tool", "name" to "get_time"))
        assertEquals("done", at(r, "status"))
        assertEquals(listOf(mapOf("type" to "tool", "name" to "get_time"), mapOf("type" to "auto")), m.seen.map { it["tool_choice"] })
        val auto = Scripted(reply(listOf(toolUse("tu_1", "get_time", "city" to "Oslo")), "tool_use"), reply(listOf(text("ok"))))
        run(auto, choice = mapOf("type" to "auto", "disable_parallel_tool_use" to true))
        val kept = mapOf("type" to "auto", "disable_parallel_tool_use" to true)
        assertEquals(listOf(kept, kept), auto.seen.map { it["tool_choice"] })
        val none = Scripted(reply(listOf(text("ok"))))
        run(none, choice = mapOf("type" to "none"))
        assertEquals(mapOf("type" to "none"), none.seen[0]["tool_choice"])
    }

    @Test
    fun e6_resultsThatAreNotTextAreSentAsJsonText() {
        val m = Scripted(reply(listOf(toolUse("tu_1", "structured", "city" to "Oslo")), "tool_use"), reply(listOf(text("ok"))))
        runAgent(m, tools(), "go")
        val results = contentOf(messagesOf(m, 1), 2)
        val content = results.firstOrNull()?.get("content")
        assertTrue(content is String)
        assertEquals(mapOf("city" to "Oslo", "temp_c" to 18L, "tags" to listOf("mild")), jsonOrText(content as String))
    }
}
