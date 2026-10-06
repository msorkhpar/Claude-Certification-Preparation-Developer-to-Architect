import static harness.Scripted.map;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.SseException;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.Tool;
import com.anthropic.core.JsonValue;
import harness.Reply;
import harness.Scripted;
import harness.ScriptedHttp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A streamed reply read three ways, from a scripted server-sent-event body.
 *
 * <p>The stream is an illustrative, hand-written sequence of events in the API's framing
 * (claude-sonnet-5-5): one text block, then one tool_use block whose input arrives in fragments.
 * The Java SDK reads the events itself and drops `ping` events, so a raw event list has no `ping` in it.
 */
public final class Streaming {
    private static final System.Logger LOG = System.getLogger(Streaming.class.getName());
    static final String MODEL = "claude-sonnet-5-5";

    static MessageCreateParams params() {
        Tool weather = Tool.builder().name("get_weather").description("Weather for a city.")
            .inputSchema(Tool.InputSchema.builder().properties(JsonValue.from(map("city", map("type", "string")))).required(List.of("city")).build()).build();
        return MessageCreateParams.builder().model(Model.of(MODEL)).maxTokens(128).addUserMessage("Weather in Paris?").addTool(weather).build();
    }

    static Map<String, Object> delta(int index, String kind, String key, String value) {
        return map("type", "content_block_delta", "index", index, "delta", map("type", kind, key, value));
    }

    static final List<Map<String, Object>> EVENTS = List.of(
        map("type", "message_start", "message", map("id", "msg_illustrative", "type", "message", "role", "assistant", "model", MODEL, "content", List.of(),
            "stop_reason", null, "stop_sequence", null, "usage", map("input_tokens", 52, "output_tokens", 1))),
        map("type", "content_block_start", "index", 0, "content_block", map("type", "text", "text", "")),
        map("type", "ping"),
        delta(0, "text_delta", "text", "Let me "),
        delta(0, "text_delta", "text", "check."),
        map("type", "content_block_stop", "index", 0),
        map("type", "content_block_start", "index", 1, "content_block", map("type", "tool_use", "id", "toolu_illustrative_1", "name", "get_weather", "input", map())),
        delta(1, "input_json_delta", "partial_json", ""),
        delta(1, "input_json_delta", "partial_json", "{\"ci"),
        delta(1, "input_json_delta", "partial_json", "ty\": \"Par"),
        delta(1, "input_json_delta", "partial_json", "is\"}"),
        map("type", "content_block_stop", "index", 1),
        map("type", "message_delta", "delta", map("stop_reason", "tool_use", "stop_sequence", null), "usage", map("output_tokens", 38)),
        map("type", "message_stop"));

    static final List<Map<String, Object>> FAILING = failing();

    private static List<Map<String, Object>> failing() {
        List<Map<String, Object>> events = new ArrayList<>(EVENTS.subList(0, 5));
        events.add(map("type", "error", "error", map("type", "overloaded_error", "message", "Overloaded")));
        return events;
    }

    /** The pieces of a streamed reply: the text deltas, and the message the SDK's accumulator assembled from all the events. */
    record Streamed(List<String> textPieces, Message message) {}

    /** The name of a raw stream event, as it is on the wire. */
    static String typeName(RawMessageStreamEvent e) {
        if (e.isMessageStart()) return "message_start";
        if (e.isMessageDelta()) return "message_delta";
        if (e.isMessageStop()) return "message_stop";
        if (e.isContentBlockStart()) return "content_block_start";
        if (e.isContentBlockDelta()) return "content_block_delta";
        return "content_block_stop";
    }

    static Streamed readText(AnthropicClient client) {
        MessageAccumulator accumulator = MessageAccumulator.create();
        List<String> pieces = new ArrayList<>();
        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(params())) {
            stream.stream().forEach(event -> {
                accumulator.accumulate(event);
                event.contentBlockDelta().flatMap(d -> d.delta().text()).ifPresent(t -> pieces.add(t.text()));
            });
        }
        return new Streamed(pieces, accumulator.message());
    }

    static List<String> rawEventNames(AnthropicClient client) {
        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(params())) {
            return stream.stream().map(Streaming::typeName).toList();
        }
    }

    /** ['a', 'b', 'b'] becomes "a, b x2". */
    static String runLength(List<String> names) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < names.size(); ) {
            int j = i;
            while (j < names.size() && names.get(j).equals(names.get(i))) j++;
            parts.add(j - i == 1 ? names.get(i) : names.get(i) + " x" + (j - i));
            i = j;
        }
        return String.join(", ", parts);
    }

    static Scripted.Rig clientFor(List<Map<String, Object>> events) {
        return Scripted.client(Reply.sse(events));
    }

    /** The tool input the stream assembled, as a map. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> toolInput(ContentBlock block) {
        return ObjectMappers.jsonMapper().convertValue(block.asToolUse()._input(), Map.class);
    }

    private static String py(Object v) {
        if (v instanceof String s) return "'" + s + "'";
        if (v instanceof Map<?, ?> m) return m.entrySet().stream().map(e -> py(e.getKey()) + ": " + py(e.getValue())).collect(Collectors.joining(", ", "{", "}"));
        if (v instanceof List<?> l) return l.stream().map(Streaming::py).collect(Collectors.joining(", ", "[", "]"));
        return String.valueOf(v);
    }

    public static void main(String[] args) {
        Scripted.Rig rig = clientFor(EVENTS);
        Streamed streamed = readText(rig.client());
        System.out.println("request sets stream: " + (rig.http().requests.get(0).get("stream").asBoolean() ? "True" : "False"));
        System.out.println("text pieces: " + py(streamed.textPieces()));
        Message finalMessage = streamed.message();
        System.out.println("final stop_reason: " + finalMessage.stopReason().get().asString() + " | usage: " + finalMessage.usage().inputTokens() + " in, " + finalMessage.usage().outputTokens() + " out");
        System.out.println("blocks: " + py(finalMessage.content().stream().map(b -> b.isText() ? "text" : "tool_use").toList()) + " | tool input: " + py(toolInput(finalMessage.content().get(1))));

        System.out.println("raw events: " + runLength(rawEventNames(clientFor(EVENTS).client())));

        try {
            readText(clientFor(FAILING).client());
        } catch (SseException err) {
            System.out.println("mid-stream error: " + err.getClass().getSimpleName() + " " + err.errorType().map(t -> t.asString()).orElse("?"));
        }
    }
}
