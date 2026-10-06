import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ToolChoice
import com.anthropic.models.messages.ToolChoiceTool
import com.anthropic.models.messages.ToolUseBlock
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Scripted.toolUse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolLoopTest {
    private fun block(id: String, name: String, city: String) = com.anthropic.core.jsonMapper().convertValue(map("type", "tool_use", "id", id, "name", name, "input", map("city", city)), ToolUseBlock::class.java)

    @Test
    fun aFailingCallBecomesAnErrorResultWithAHint() {
        val result = runTool(block("t1", "get_weather", "Atlantis"))
        assertEquals(true, result.isError().get())
        assertTrue("Known cities" in result.content().get().string().get())
    }

    @Test
    fun allResultsGoBackInOneUserMessageInCallOrder() {
        val rig = Scripted.client(*REPLIES.toTypedArray())
        val run = loop(rig.client(), "q")
        assertEquals(listOf("user", "assistant", "user", "assistant"), run.messages.map { it.role().asString() })
        assertEquals(listOf("toolu_01", "toolu_02", "toolu_03"), run.messages[2].content().blockParams().get().map { it.asToolResult().toolUseId() })
        assertEquals("end_turn", run.reply.stopReason().get().asString())
        assertEquals(2, rig.http().requests.size)
    }

    @Test
    fun aForcedChoiceIsSentOnce() {
        val rig = Scripted.client(message(listOf(toolUse("toolu_01", "get_time", map("city", "Oslo"))), "tool_use"), message(listOf(text("09:15"))))
        loop(rig.client(), "time?", ToolChoice.ofTool(ToolChoiceTool.builder().name("get_time").build()))
        assertEquals("tool", rig.http().requests[0].at("/tool_choice/type").asText())
        assertEquals("get_time", rig.http().requests[0].at("/tool_choice/name").asText())
        assertFalse(rig.http().requests[1].has("tool_choice"))
    }
}
