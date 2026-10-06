import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AssembleTest {
    @SuppressWarnings("unchecked")
    private static Map<String, Object> ev(String json) {
        return (Map<String, Object>) Json.parse(json);
    }

    private static Map<String, Object> start(int in, int out) {
        return ev("{\"type\":\"message_start\",\"message\":{\"id\":\"msg_x\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-sonnet-5-5\","
            + "\"content\":[],\"stop_reason\":null,\"stop_sequence\":null,\"usage\":{\"input_tokens\":" + in + ",\"output_tokens\":" + out + "}}}");
    }

    private static Map<String, Object> start() {
        return start(25, 1);
    }

    private static Map<String, Object> blockStart(int index, String blockJson) {
        return ev("{\"type\":\"content_block_start\",\"index\":" + index + ",\"content_block\":" + blockJson + "}");
    }

    private static Map<String, Object> delta(int index, String kind, String key, String value) {
        return ev("{\"type\":\"content_block_delta\",\"index\":" + index + ",\"delta\":{\"type\":\"" + kind + "\",\"" + key + "\":" + Json.stringify(value) + "}}");
    }

    private static Map<String, Object> text(int index, String value) {
        return delta(index, "text_delta", "text", value);
    }

    private static Map<String, Object> stop(int index) {
        return ev("{\"type\":\"content_block_stop\",\"index\":" + index + "}");
    }

    private static List<Map<String, Object>> finish(String reason, int outputTokens) {
        return List.of(ev("{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"" + reason + "\",\"stop_sequence\":null},\"usage\":{\"output_tokens\":" + outputTokens + "}}"),
            ev("{\"type\":\"message_stop\"}"));
    }

    private static List<Map<String, Object>> finish() {
        return finish("end_turn", 15);
    }

    private static List<Map<String, Object>> events(Object... parts) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object p : parts) {
            if (p instanceof List<?> l) l.forEach(x -> out.add(cast(x)));
            else out.add(cast(p));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object o) {
        return (Map<String, Object>) o;
    }

    private static final String TEXT_BLOCK = "{\"type\":\"text\",\"text\":\"\"}";

    private static RuntimeException errorOf(Runnable call) {
        try {
            call.run();
        } catch (RuntimeException e) {
            return e;
        }
        return null;
    }

    @Test
    void m1_textDeltasAreJoinedAndTheMessageIsComplete() {
        Map<String, Object> message = Assemble.assemble(events(start(), blockStart(0, TEXT_BLOCK), text(0, "Hel"), text(0, "lo, "), text(0, "world."), stop(0), finish()));
        assertEquals(Json.parse("[{\"type\":\"text\",\"text\":\"Hello, world.\"}]"), message.get("content"));
        assertEquals(List.of("msg_x", "assistant", "claude-sonnet-5-5"), List.of(String.valueOf(message.get("id")), String.valueOf(message.get("role")), String.valueOf(message.get("model"))));
        assertEquals("end_turn", message.get("stop_reason"));
        assertNull(message.get("stop_sequence"));
    }

    @Test
    void e1_toolInputIsTheFragmentsJoinedThenParsedAndEmptyInputIsAnEmptyObject() {
        String tool = "{\"type\":\"tool_use\",\"id\":\"toolu_1\",\"name\":\"get_weather\",\"input\":{}}";
        String tool2 = "{\"type\":\"tool_use\",\"id\":\"toolu_2\",\"name\":\"get_weather\",\"input\":{}}";
        Map<String, Object> message = Assemble.assemble(events(start(), blockStart(0, tool), delta(0, "input_json_delta", "partial_json", ""),
            delta(0, "input_json_delta", "partial_json", "{\"ci"), delta(0, "input_json_delta", "partial_json", "ty\": \"Pa"),
            delta(0, "input_json_delta", "partial_json", "ris\", \"days\": 3}"), stop(0), blockStart(1, tool2), stop(1), finish("tool_use", 15)));
        assertEquals(Json.parse("[{\"type\":\"tool_use\",\"id\":\"toolu_1\",\"name\":\"get_weather\",\"input\":{\"city\":\"Paris\",\"days\":3}},"
            + "{\"type\":\"tool_use\",\"id\":\"toolu_2\",\"name\":\"get_weather\",\"input\":{}}]"), message.get("content"));
    }

    @Test
    void e2_pingAndUnknownEventTypesAreIgnored() {
        Map<String, Object> message = Assemble.assemble(events(start(), ev("{\"type\":\"ping\"}"), blockStart(0, TEXT_BLOCK), ev("{\"type\":\"ping\"}"), text(0, "ok"),
            ev("{\"type\":\"some_future_event\",\"data\":{\"x\":1}}"), stop(0), finish()));
        assertEquals(Json.parse("[{\"type\":\"text\",\"text\":\"ok\"}]"), message.get("content"));
    }

    @Test
    void e3_anErrorEventRaisesWithItsTypeAndMessage() {
        List<Map<String, Object>> events = events(start(), blockStart(0, TEXT_BLOCK), text(0, "partial"),
            ev("{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"Overloaded\"}}"));
        StreamError err = assertInstanceOf(StreamError.class, errorOf(() -> Assemble.assemble(events)));
        assertEquals(List.of("overloaded_error", "Overloaded"), List.of(err.errorType(), err.detail()));
    }

    @Test
    void e4_aStreamThatEndsBeforeMessageStopIsAnErrorNotAShortMessage() {
        List<Map<String, Object>> events = events(start(), blockStart(0, TEXT_BLOCK), text(0, "cut off"));
        StreamError err = assertInstanceOf(StreamError.class, errorOf(() -> Assemble.assemble(events)));
        assertEquals("incomplete_stream", err.errorType());
    }

    @Test
    void e5_usageTakesInputTokensFromTheStartAndTheCumulativeOutputFromTheEnd() {
        Map<String, Object> message = Assemble.assemble(events(start(52, 1), blockStart(0, TEXT_BLOCK), text(0, "x"), stop(0), finish("end_turn", 38)));
        assertEquals(Json.parse("{\"input_tokens\":52,\"output_tokens\":38}"), message.get("usage"));
    }

    @Test
    void e6_blocksKeepTheirIndexOrderAndThinkingFieldsAreAssembled() {
        Map<String, Object> message = Assemble.assemble(events(start(), blockStart(0, "{\"type\":\"thinking\",\"thinking\":\"\"}"),
            delta(0, "thinking_delta", "thinking", "Let me "), delta(0, "thinking_delta", "thinking", "think."),
            delta(0, "signature_delta", "signature", "sig-abc"), stop(0), blockStart(1, TEXT_BLOCK), text(1, "Answer."), stop(1), finish()));
        assertEquals(Json.parse("[{\"type\":\"thinking\",\"thinking\":\"Let me think.\",\"signature\":\"sig-abc\"},{\"type\":\"text\",\"text\":\"Answer.\"}]"), message.get("content"));
    }
}
