import harness.Scripted
import harness.Scripted.message
import harness.Scripted.text
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ThinkingTest {
    @Test
    fun theRequestCarriesAdaptiveThinkingAndEffortInOutputConfig() {
        val rig = Scripted.client(message(listOf(text("x"))))
        request(rig.client(), "medium")
        val sent = rig.http().requests[0]
        assertEquals("adaptive", sent.at("/thinking/type").asText())
        assertEquals(1, sent["thinking"].size())
        assertEquals("medium", sent.at("/output_config/effort").asText())
        assertFalse("budget_tokens" in sent.toString())
        assertFalse(sent.has("temperature"))
    }

    @Test
    fun aThinkingBlockCanBeEmptyAndBilledTokensExceedVisibleOnes() {
        val rig = Scripted.client(message(listOf(THINKING_BLOCK, text("A")), "end_turn", MODEL, USAGE, null))
        val reply = request(rig.client(), "high")
        assertTrue(reply.content()[0].isThinking())
        assertEquals("", reply.content()[0].asThinking().thinking())
        assertEquals(1650L, thinkingTokens(reply.usage()))
        assertEquals(1900L, reply.usage().outputTokens())
    }

    @Test
    fun aTurnMayHaveNoThinkingBlockAtAll() {
        val reply = request(Scripted.client(message(listOf(text("B.")))).client(), "low")
        assertEquals(1, reply.content().size)
        assertTrue(reply.content()[0].isText())
    }
}
