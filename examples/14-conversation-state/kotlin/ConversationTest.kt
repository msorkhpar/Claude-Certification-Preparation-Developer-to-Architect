import harness.Scripted
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConversationTest {
    private fun drive(): Pair<harness.ScriptedHttp, List<Turn>> {
        val transport = Scripted.http(*REPLIES.toTypedArray())
        return transport to run(Scripted.clientOn(transport, 0), QUESTIONS)
    }

    @Test
    fun everyRequestCarriesTheWholeHistory() {
        val (transport, _) = drive()
        assertEquals(listOf(1, 3, 5), transport.requests.map { it["messages"].size() })
        assertEquals(listOf("user", "assistant", "user", "assistant", "user"), transport.requests[2]["messages"].map { it["role"].asText() })
    }

    @Test
    fun assistantTurnsAreSentBackAsReceived() {
        val assistant = drive().first.requests[1]["messages"][1]
        assertEquals("assistant", assistant["role"].asText())
        assertEquals(1, assistant["content"].size())
        assertEquals("text", assistant["content"][0]["type"].asText())
        assertEquals("Paris.", assistant["content"][0]["text"].asText())
    }

    @Test
    fun usageAddsUpAndStopReasonsAreRead() {
        val turns = drive().second
        assertEquals(89L, turns.last().input)
        assertEquals(12L, turns.last().output)
        assertEquals(listOf("end_turn", "max_tokens", "stop_sequence"), turns.map { it.reply.stopReason().get().asString() })
    }

    @Test
    fun systemIsTopLevelNeverAMessageRole() {
        val transport = drive().first
        assertTrue(transport.requests.all { it.path("system").asText().isNotEmpty() })
        assertTrue(transport.requests.all { r -> r["messages"].all { it["role"].asText() != "system" } })
    }
}
