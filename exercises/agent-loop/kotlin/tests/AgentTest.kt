import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AgentTest {
    private val tools: Map<String, (Map<String, Any?>) -> Any?> =
        mapOf("add" to { i -> ((i["a"] as Int) + (i["b"] as Int)).toString() })

    @Suppress("UNCHECKED_CAST")
    private fun content(m: Msg) = m["content"] as List<Msg>

    @Test fun twoTurnsReturnsFinalText() {
        val m = ScriptedModel(
            reply(listOf(text("Adding."), toolUse("tu_1", "add", mapOf("a" to 2, "b" to 3))), "tool_use"),
            reply(listOf(text("The sum is 5.")), "end_turn"))
        assertEquals("The sum is 5.", runAgent(m, tools, "2+3?"))
        assertEquals(2, m.seen.size)
    }

    @Test fun toolResultsAreOneUserTurn() {
        val m = ScriptedModel(
            reply(listOf(toolUse("tu_1", "add", mapOf("a" to 2, "b" to 3)), toolUse("tu_2", "add", mapOf("a" to 1, "b" to 1))), "tool_use"),
            reply(listOf(text("done")), "end_turn"))
        runAgent(m, tools, "go")
        val second = m.seen[1]
        assertEquals(listOf("user", "assistant", "user"), second.map { it["role"] })
        assertEquals(listOf("tool_result:tu_1:5", "tool_result:tu_2:2"),
            content(second[2]).map { "${it["type"]}:${it["tool_use_id"]}:${it["content"]}" })
    }

    @Test fun failingAndUnknownToolsBecomeErrorResults() {
        val boom: Map<String, (Map<String, Any?>) -> Any?> = mapOf("boom" to { _ -> throw IllegalStateException("bad input") })
        val m = ScriptedModel(
            reply(listOf(toolUse("tu_1", "boom"), toolUse("tu_2", "nope")), "tool_use"),
            reply(listOf(text("recovered")), "end_turn"))
        assertEquals("recovered", runAgent(m, boom, "go"))
        val res = content(m.seen[1][2])
        assertEquals(true, res[0]["is_error"])
        assertTrue(res[0]["content"].toString().contains("bad input"))
        assertEquals(true, res[1]["is_error"])
        assertTrue(res[1]["content"].toString().contains("nope"))
    }

    @Test fun noToolUseReturnsImmediately() {
        val m = ScriptedModel(reply(listOf(text("hi")), "end_turn"))
        assertEquals("hi", runAgent(m, tools, "hello"))
        assertEquals(1, m.seen.size)
    }

    @Test fun turnCapThrows() {
        val loop = (0 until 10).map { reply(listOf(toolUse("tu_$it", "add", mapOf("a" to 1, "b" to 1))), "tool_use") }
        assertThrows(IllegalStateException::class.java) { runAgent(ScriptedModel(*loop.toTypedArray()), tools, "go", 3) }
    }

    @Test fun textInToolUseTurnDoesNotEndTheLoop() {
        val m = ScriptedModel(
            reply(listOf(text("I will call a tool."), toolUse("tu_1", "add", mapOf("a" to 1, "b" to 2))), "tool_use"),
            reply(listOf(text("3")), "end_turn"))
        assertEquals("3", runAgent(m, tools, "go"))
    }
}
