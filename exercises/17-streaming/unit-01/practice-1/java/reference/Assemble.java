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

    /** The message a message_start event begins: its fields except content, an empty content list and a copy of its usage. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> startMessage(Map<String, Object> start) {
        Map<String, Object> message = new LinkedHashMap<>(start);
        message.put("content", new ArrayList<>());
        Map<String, Object> usage = new LinkedHashMap<>();
        if (start.get("usage") instanceof Map<?, ?> u) usage.putAll((Map<String, Object>) u);
        message.put("usage", usage);
        return message;
    }

    /** Fold one content_block_delta into its block; `pieces` is the fragment builder of a tool_use block. */
    private static void applyDelta(Map<String, Object> block, StringBuilder pieces, Map<String, Object> delta) {
        switch ((String) delta.get("type")) {
            case "text_delta" -> block.put("text", String.valueOf(block.getOrDefault("text", "")) + delta.get("text"));
            case "input_json_delta" -> pieces.append((String) delta.get("partial_json"));
            case "thinking_delta" -> block.put("thinking", String.valueOf(block.getOrDefault("thinking", "")) + delta.get("thinking"));
            case "signature_delta" -> block.put("signature", delta.get("signature"));
            default -> { }
        }
    }

    /** At content_block_stop: a tool_use block (it has `pieces`) gets its input, the fragments joined and parsed, an empty map when empty. */
    private static void finishBlock(Map<String, Object> block, StringBuilder pieces) {
        if (pieces == null) return;
        String joined = pieces.toString();
        block.put("input", joined.isBlank() ? new LinkedHashMap<String, Object>() : Json.parse(joined));
    }

    /** Fold a message_delta event: the stop reason and sequence, and each usage key replaces the same key (the output count is cumulative). */
    @SuppressWarnings("unchecked")
    private static void applyMessageDelta(Map<String, Object> message, Map<String, Object> event) {
        Map<String, Object> delta = (Map<String, Object>) event.get("delta");
        message.put("stop_reason", delta.get("stop_reason"));
        message.put("stop_sequence", delta.get("stop_sequence"));
        if (event.get("usage") instanceof Map<?, ?> u) ((Map<String, Object>) message.get("usage")).putAll((Map<String, Object>) u);
    }

    /** An error event ends the assembly with a StreamError carrying the error's type and message. */
    @SuppressWarnings("unchecked")
    private static void raiseError(Map<String, Object> event) {
        Map<String, Object> error = (Map<String, Object>) event.get("error");
        throw new StreamError(String.valueOf(error.getOrDefault("type", "unknown")), String.valueOf(error.getOrDefault("message", "")));
    }

    /** A stream that never started or never reached message_stop is an error, not a short message. */
    private static void checkComplete(Map<String, Object> message, boolean stopped) {
        if (message == null || !stopped) throw new StreamError("incomplete_stream", "the stream ended before message_stop");
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
