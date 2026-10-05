import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Assemble a streamed Messages reply from its events. See ../../statement.md for the contract. */
final class Assemble {
    private static final System.Logger LOG = System.getLogger(Assemble.class.getName());
    private Assemble() {}

    private static Map<String, Object> startMessage(Map<String, Object> start) {
        // TODO 1 of 6 (finish this to pass m1 and e5): the message a message_start event begins.
        // Receives the `message` object of the event. Returns a new map with its fields except "content", then "content" as an empty list and "usage" as a COPY of its usage.
        // Example: startMessage({id: m, content: [], usage: {input_tokens: 5}}) -> {id: m, content: [], usage: {input_tokens: 5}}
        return new LinkedHashMap<>();
    }

    private static void applyDelta(Map<String, Object> block, StringBuilder pieces, Map<String, Object> delta) {
        // TODO 2 of 6 (finish this to pass m1 and e6): fold one content_block_delta into its block.
        // Receives the block, `pieces` (the fragment builder of a tool_use block, else null) and the delta. "text_delta" appends "text" to the block's text,
        // "thinking_delta" appends "thinking", "signature_delta" sets "signature", "input_json_delta" appends "partial_json" to `pieces`. Returns nothing.
        // Example: block {text: He}, delta {type: text_delta, text: llo} -> block {text: Hello}
    }

    private static void finishBlock(Map<String, Object> block, StringBuilder pieces) {
        // TODO 3 of 6 (finish this to pass e1): at content_block_stop, give a tool_use block its input.
        // Receives the block and its fragment builder `pieces` (null for a block that is not tool_use). When `pieces` is not null, puts "input" into the block: the
        // fragments joined and parsed with Json.parse, or an empty map when the joined text is empty or only white space. Returns nothing.
        // Example: pieces {"ci + ty": "Pa + ris"} -> block "input" is {city=Paris}; pieces "" -> {}
    }

    private static void applyMessageDelta(Map<String, Object> message, Map<String, Object> event) {
        // TODO 4 of 6 (finish this to pass m1 and e5): fold a message_delta event into the message.
        // Receives the message and the event. Puts "stop_reason" and "stop_sequence" from event "delta", and each key of event "usage" replaces the same key in
        // the message's "usage" map (the output count is cumulative: replace, do not add). Returns nothing.
        // Example: usage {input_tokens: 52, output_tokens: 1} and event usage {output_tokens: 38} -> {input_tokens: 52, output_tokens: 38}
    }

    private static void raiseError(Map<String, Object> event) {
        // TODO 5 of 6 (finish this to pass e3): an error event ends the assembly.
        // Receives the event, whose "error" map holds a "type" and a "message". Throws new StreamError(type, message).
        // Example: {error: {type: overloaded_error, message: Overloaded}} throws StreamError("overloaded_error", "Overloaded")
    }

    private static void checkComplete(Map<String, Object> message, boolean stopped) {
        // TODO 6 of 6 (finish this to pass e4): a stream that never started or never reached message_stop is an error.
        // Receives the message (null before message_start) and whether message_stop was seen. Throws new StreamError("incomplete_stream", ...) unless both hold.
        // Example: checkComplete(message, false) throws a StreamError with errorType "incomplete_stream"
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> assemble(List<Map<String, Object>> events) {
        LOG.log(System.Logger.Level.DEBUG, "assemble input: {0}", events);
        Map<String, Object> message = null;
        TreeMap<Integer, Map<String, Object>> blocks = new TreeMap<>();
        Map<Integer, StringBuilder> fragments = new HashMap<>();
        boolean stopped = false;
        for (Map<String, Object> event : events) {
            String kind = (String) event.get("type");
            if (kind == null) continue;
            switch (kind) {
                case "message_start" -> message = startMessage((Map<String, Object>) event.get("message"));
                case "content_block_start" -> {
                    int index = ((Number) event.get("index")).intValue();
                    Map<String, Object> block = new LinkedHashMap<>((Map<String, Object>) event.get("content_block"));
                    blocks.put(index, block);
                    if ("tool_use".equals(block.get("type"))) fragments.put(index, new StringBuilder());
                }
                case "content_block_delta" -> {
                    int index = ((Number) event.get("index")).intValue();
                    applyDelta(blocks.get(index), fragments.get(index), (Map<String, Object>) event.get("delta"));
                }
                case "content_block_stop" -> {
                    int index = ((Number) event.get("index")).intValue();
                    finishBlock(blocks.get(index), fragments.get(index));
                }
                case "message_delta" -> applyMessageDelta(message, event);
                case "message_stop" -> stopped = true;
                case "error" -> raiseError(event);
                default -> { } // ping and event types this client does not know are skipped: new types may be added
            }
        }
        checkComplete(message, stopped);
        message.put("content", new ArrayList<>(blocks.values()));
        return message;
    }
}
