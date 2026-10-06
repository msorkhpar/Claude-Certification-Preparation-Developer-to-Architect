import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AgentLoopTest {
    private fun text(t: String): Map<String, Any?> = linkedMapOf("type" to "text", "text" to t)
    private fun call(id: String, name: String, vararg input: Pair<String, Any?>): Map<String, Any?> =
        linkedMapOf("type" to "tool_use", "id" to id, "name" to name, "input" to linkedMapOf(*input))
    private fun reply(stopReason: String, vararg content: Map<String, Any?>): Map<String, Any?> = linkedMapOf("stop_reason" to stopReason, "content" to content.toList())
    private fun result(id: String, content: String): Map<String, Any?> = linkedMapOf("type" to "tool_result", "tool_use_id" to id, "content" to content)
    private fun errorResult(id: String, content: String): Map<String, Any?> = linkedMapOf("type" to "tool_result", "tool_use_id" to id, "content" to content, "is_error" to true)

    /** A scripted model: replies in order, or the last one again when it repeats; it keeps a snapshot of every request. */
    private class ScriptedModel(private val repeat: Boolean, vararg replies: Map<String, Any?>) : (List<Map<String, Any?>>) -> Map<String, Any?> {
        private val queue = replies.toMutableList()
        val seen = mutableListOf<List<Map<String, Any?>>>()

        override fun invoke(messages: List<Map<String, Any?>>): Map<String, Any?> {
            seen.add(messages.toList())
            if (queue.size > 1 || !repeat) {
                if (queue.isEmpty()) fail<Unit>("the loop called the model again after the script ended")
                return queue.removeAt(0)
            }
            return queue[0]
        }
    }

    /** Handlers that record every call. */
    private class RecordingTools {
        val handlers = linkedMapOf<String, (Map<String, Any?>) -> String>()
        val calls = mutableListOf<String>()

        fun with(name: String, fn: (Map<String, Any?>) -> String): RecordingTools {
            handlers[name] = { input -> calls.add("$name $input"); fn(input) }
            return this
        }
    }

    private val lookup: (Map<String, Any?>) -> String = { "record ${it["n"]}" }

    @Suppress("UNCHECKED_CAST")
    private fun messages(r: Map<String, Any?>?): List<Map<String, Any?>> {
        assertNotNull(r, "runAgent returned null")
        return r!!["messages"] as List<Map<String, Any?>>
    }

    private fun roles(r: Map<String, Any?>?) = messages(r).map { it["role"] }

    @Test
    fun m1_aRunAlternatesModelAndToolsUntilTheModelEndsItsTurn() {
        val model = ScriptedModel(false, reply("tool_use", text("Looking."), call("t1", "lookup", "n" to 1)), reply("end_turn", text("Record 1 found.")))
        val tools = RecordingTools().with("lookup", lookup)
        val r = runAgent(model, tools.handlers, "find record 1")
        assertNotNull(r, "runAgent returned null")
        assertEquals(listOf<Any?>("done", "Record 1 found.", 2), listOf(r!!["status"], r["text"], r["turns"]))
        assertEquals(listOf("user", "assistant", "user", "assistant"), roles(r))
        assertEquals(mapOf("role" to "user", "content" to "find record 1"), messages(r)[0])
        assertEquals(listOf(result("t1", "record 1")), messages(r)[2]["content"])
        assertEquals(listOf(1, 3), model.seen.map { it.size })
        assertEquals(listOf("lookup {n=1}"), tools.calls)
    }

    @Test
    fun e1_theStopReasonDecidesAndTheWordsOfTheTextDoNot() {
        val model = ScriptedModel(false, reply("tool_use", text("All done. Saving now."), call("t1", "lookup", "n" to 7)), reply("end_turn", text("Saved.")))
        val tools = RecordingTools().with("lookup", lookup)
        val r = runAgent(model, tools.handlers, "go")
        assertNotNull(r, "runAgent returned null")
        assertEquals(listOf<Any?>("done", 2), listOf(r!!["status"], r["turns"]))
        assertEquals(listOf("lookup {n=7}"), tools.calls)
        val announce = ScriptedModel(false, reply("end_turn", text("Next I will call the lookup tool.")))
        val again = RecordingTools().with("lookup", lookup)
        val ended = runAgent(announce, again.handlers, "go")
        assertNotNull(ended, "runAgent returned null")
        assertEquals(listOf<Any?>("done", 1, 0, 1), listOf(ended!!["status"], ended["turns"], again.calls.size, announce.seen.size))
    }

    @Test
    fun e2_everyCallOfATurnIsAnsweredInOneUserMessageInOrder() {
        val first = reply("tool_use", text("Three at once."), call("a", "lookup", "n" to 1), call("b", "lookup", "n" to 2), call("c", "lookup", "n" to 3))
        val r = runAgent(ScriptedModel(false, first, reply("end_turn", text("ok"))), RecordingTools().with("lookup", lookup).handlers, "go")
        assertEquals(listOf("user", "assistant", "user", "assistant"), roles(r))
        assertEquals(first["content"], messages(r)[1]["content"])
        assertEquals(listOf(result("a", "record 1"), result("b", "record 2"), result("c", "record 3")), messages(r)[2]["content"])
    }

    @Test
    fun e3_aFailingOrUnknownToolBecomesAnErrorResultAndTheRunGoesOn() {
        val model = ScriptedModel(false, reply("tool_use", call("a", "broken"), call("b", "missing"), call("c", "lookup", "n" to 5)), reply("end_turn", text("Partly done.")))
        val tools = RecordingTools().with("broken") { throw IllegalStateException("database offline") }.with("lookup", lookup)
        val r = runAgent(model, tools.handlers, "go")
        assertNotNull(r, "runAgent returned null")
        assertEquals("done", r!!["status"])
        assertEquals(listOf(errorResult("a", "database offline"), errorResult("b", "Unknown tool: missing"), result("c", "record 5")), messages(r)[2]["content"])
        assertEquals(listOf("broken {}", "lookup {n=5}"), tools.calls)
    }

    @Test
    fun e4_theTurnLimitIsABackstopThatEndsOnlyARunTheModelHasNotEnded() {
        val endless = ScriptedModel(true, reply("tool_use", call("t", "lookup", "n" to 1)))
        val tools = RecordingTools().with("lookup", lookup)
        val r = runAgent(endless, tools.handlers, "go", 3)
        assertNotNull(r, "runAgent returned null")
        assertEquals(listOf<Any?>("max_turns", 3, 3, 3), listOf(r!!["status"], r["turns"], endless.seen.size, tools.calls.size))
        assertEquals("user", messages(r).last()["role"])
        assertEquals(7, messages(r).size)
        val last = ScriptedModel(false, reply("tool_use", call("a", "lookup", "n" to 1)), reply("tool_use", call("b", "lookup", "n" to 2)), reply("end_turn", text("Finished on the last turn.")))
        val done = runAgent(last, RecordingTools().with("lookup", lookup).handlers, "go", 3)
        assertNotNull(done, "runAgent returned null")
        assertEquals(listOf<Any?>("done", 3, "Finished on the last turn."), listOf(done!!["status"], done["turns"], done["text"]))
    }

    @Test
    fun e5_aCutOffOrRefusedReplyEndsTheRunWithItsOwnStatus() {
        val expected = linkedMapOf("max_tokens" to "truncated", "refusal" to "refused", "stop_sequence" to "done", "some_new_reason" to "unexpected")
        for ((stopReason, status) in expected) {
            val model = ScriptedModel(false, reply(stopReason, text("partial words")))
            val r = runAgent(model, RecordingTools().with("lookup", lookup).handlers, "go")
            assertNotNull(r, "runAgent returned null")
            assertEquals(listOf<Any?>(status, "partial words", 1, 1), listOf(r!!["status"], r["text"], r["turns"], model.seen.size), stopReason)
        }
    }

    @Test
    fun e6_aToolUseReplyWithoutAToolCallIsMalformedAndSendsNothingMore() {
        val model = ScriptedModel(false, reply("tool_use", text("I will call a tool.")))
        val r = runAgent(model, RecordingTools().with("lookup", lookup).handlers, "go")
        assertNotNull(r, "runAgent returned null")
        assertEquals(listOf<Any?>("malformed", 1, 1, 2), listOf(r!!["status"], r["turns"], model.seen.size, messages(r).size))
    }
}
