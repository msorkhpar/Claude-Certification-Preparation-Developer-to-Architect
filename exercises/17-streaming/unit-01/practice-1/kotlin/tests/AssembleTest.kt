import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AssembleTest {
    @Suppress("UNCHECKED_CAST")
    private fun ev(json: String) = Json.parse(json) as Map<String, Any?>

    private fun start(input: Int = 25, output: Int = 1) = ev(
        """{"type":"message_start","message":{"id":"msg_x","type":"message","role":"assistant","model":"claude-sonnet-5-5",
        "content":[],"stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":$input,"output_tokens":$output}}}""",
    )

    private fun blockStart(index: Int, blockJson: String) = ev("""{"type":"content_block_start","index":$index,"content_block":$blockJson}""")

    private fun delta(index: Int, kind: String, key: String, value: String) =
        ev("""{"type":"content_block_delta","index":$index,"delta":{"type":"$kind","$key":${Json.stringify(value)}}}""")

    private fun text(index: Int, value: String) = delta(index, "text_delta", "text", value)

    private fun stop(index: Int) = ev("""{"type":"content_block_stop","index":$index}""")

    private fun finish(reason: String = "end_turn", outputTokens: Int = 15) = listOf(
        ev("""{"type":"message_delta","delta":{"stop_reason":"$reason","stop_sequence":null},"usage":{"output_tokens":$outputTokens}}"""),
        ev("""{"type":"message_stop"}"""),
    )

    @Suppress("UNCHECKED_CAST")
    private fun events(vararg parts: Any): List<Map<String, Any?>> =
        parts.flatMap { if (it is List<*>) it as List<Map<String, Any?>> else listOf(it as Map<String, Any?>) }

    private val textBlock = """{"type":"text","text":""}"""

    private fun errorOf(call: () -> Unit): RuntimeException? =
        try {
            call()
            null
        } catch (e: RuntimeException) {
            e
        }

    @Test
    fun m1_textDeltasAreJoinedAndTheMessageIsComplete() {
        val message = assemble(events(start(), blockStart(0, textBlock), text(0, "Hel"), text(0, "lo, "), text(0, "world."), stop(0), finish()))
        assertEquals(Json.parse("""[{"type":"text","text":"Hello, world."}]"""), message["content"])
        assertEquals(listOf("msg_x", "assistant", "claude-sonnet-5-5"), listOf(message["id"], message["role"], message["model"]))
        assertEquals("end_turn", message["stop_reason"])
        assertNull(message["stop_sequence"])
    }

    @Test
    fun e1_toolInputIsTheFragmentsJoinedThenParsedAndEmptyInputIsAnEmptyObject() {
        val tool = """{"type":"tool_use","id":"toolu_1","name":"get_weather","input":{}}"""
        val tool2 = """{"type":"tool_use","id":"toolu_2","name":"get_weather","input":{}}"""
        val message = assemble(
            events(
                start(), blockStart(0, tool), delta(0, "input_json_delta", "partial_json", ""),
                delta(0, "input_json_delta", "partial_json", "{\"ci"), delta(0, "input_json_delta", "partial_json", "ty\": \"Pa"),
                delta(0, "input_json_delta", "partial_json", "ris\", \"days\": 3}"), stop(0), blockStart(1, tool2), stop(1), finish("tool_use"),
            ),
        )
        assertEquals(
            Json.parse(
                """[{"type":"tool_use","id":"toolu_1","name":"get_weather","input":{"city":"Paris","days":3}},""" +
                    """{"type":"tool_use","id":"toolu_2","name":"get_weather","input":{}}]""",
            ),
            message["content"],
        )
    }

    @Test
    fun e2_pingAndUnknownEventTypesAreIgnored() {
        val message = assemble(
            events(
                start(), ev("""{"type":"ping"}"""), blockStart(0, textBlock), ev("""{"type":"ping"}"""), text(0, "ok"),
                ev("""{"type":"some_future_event","data":{"x":1}}"""), stop(0), finish(),
            ),
        )
        assertEquals(Json.parse("""[{"type":"text","text":"ok"}]"""), message["content"])
    }

    @Test
    fun e3_anErrorEventRaisesWithItsTypeAndMessage() {
        val events = events(start(), blockStart(0, textBlock), text(0, "partial"), ev("""{"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}"""))
        val err = errorOf { assemble(events) }
        assertTrue(err is StreamError, "got $err")
        err as StreamError
        assertEquals(listOf("overloaded_error", "Overloaded"), listOf(err.errorType, err.detail))
    }

    @Test
    fun e4_aStreamThatEndsBeforeMessageStopIsAnErrorNotAShortMessage() {
        val events = events(start(), blockStart(0, textBlock), text(0, "cut off"))
        val err = errorOf { assemble(events) }
        assertTrue(err is StreamError, "got $err")
        err as StreamError
        assertEquals("incomplete_stream", err.errorType)
    }

    @Test
    fun e5_usageTakesInputTokensFromTheStartAndTheCumulativeOutputFromTheEnd() {
        val message = assemble(events(start(52, 1), blockStart(0, textBlock), text(0, "x"), stop(0), finish("end_turn", 38)))
        assertEquals(Json.parse("""{"input_tokens":52,"output_tokens":38}"""), message["usage"])
    }

    @Test
    fun e6_blocksKeepTheirIndexOrderAndThinkingFieldsAreAssembled() {
        val message = assemble(
            events(
                start(), blockStart(0, """{"type":"thinking","thinking":""}"""),
                delta(0, "thinking_delta", "thinking", "Let me "), delta(0, "thinking_delta", "thinking", "think."),
                delta(0, "signature_delta", "signature", "sig-abc"), stop(0), blockStart(1, textBlock), text(1, "Answer."), stop(1), finish(),
            ),
        )
        assertEquals(
            Json.parse("""[{"type":"thinking","thinking":"Let me think.","signature":"sig-abc"},{"type":"text","text":"Answer."}]"""),
            message["content"],
        )
    }
}
