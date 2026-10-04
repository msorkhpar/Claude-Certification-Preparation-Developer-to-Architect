import com.anthropic.errors.SseException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StreamingTest {
    @Test
    fun theTextStreamYieldsTheTextDeltasOnly() {
        assertEquals(listOf("Let me ", "check."), readText(clientFor(EVENTS).client()).textPieces)
    }

    @Test
    fun theFinalMessageAssemblesTextToolInputAndUsage() {
        val message = readText(clientFor(EVENTS).client()).message
        assertEquals("tool_use", message.stopReason().get().asString())
        assertEquals("Let me check.", message.content()[0].asText().text())
        assertEquals(mapOf("city" to "Paris"), toolInput(message.content()[1]))
        assertEquals(52L, message.usage().inputTokens())
        assertEquals(38L, message.usage().outputTokens())
    }

    @Test
    fun theRequestAsksForAStreamAndTheSdkDropsPingEvents() {
        val rig = clientFor(EVENTS)
        val names = rawEventNames(rig.client())
        assertTrue(rig.http().requests[0]["stream"].asBoolean())
        assertFalse("ping" in names)
        assertEquals(13, names.size)
    }

    @Test
    fun anErrorEventAfterA200Raises() {
        assertThrows(SseException::class.java) { readText(clientFor(FAILING).client()) }
    }
}
