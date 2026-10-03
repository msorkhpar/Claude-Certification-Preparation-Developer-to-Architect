import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Assemble a streamed Messages reply from its events. See ../../statement.md for the contract. */
final class Assemble {
    private Assemble() {}

    @SuppressWarnings("unchecked")
    static Map<String, Object> assemble(List<Map<String, Object>> events) {
        Map<String, Object> message = null;
        TreeMap<Integer, Map<String, Object>> blocks = new TreeMap<>();
        Map<Integer, StringBuilder> fragments = new HashMap<>();
        boolean stopped = false;
        for (Map<String, Object> event : events) {
            String kind = (String) event.get("type");
            if (kind == null) continue;
            switch (kind) {
                case "message_start" -> {
                    Map<String, Object> start = (Map<String, Object>) event.get("message");
                    message = new LinkedHashMap<>(start);
                    message.put("content", new ArrayList<>());
                    Map<String, Object> usage = new LinkedHashMap<>();
                    if (start.get("usage") instanceof Map<?, ?> u) usage.putAll((Map<String, Object>) u);
                    message.put("usage", usage);
                }
                case "content_block_start" -> {
                    int index = ((Number) event.get("index")).intValue();
                    Map<String, Object> block = new LinkedHashMap<>((Map<String, Object>) event.get("content_block"));
                    blocks.put(index, block);
                    if ("tool_use".equals(block.get("type"))) fragments.put(index, new StringBuilder());
                }
                case "content_block_delta" -> {
                    int index = ((Number) event.get("index")).intValue();
                    Map<String, Object> block = blocks.get(index);
                    Map<String, Object> delta = (Map<String, Object>) event.get("delta");
                    switch ((String) delta.get("type")) {
                        case "text_delta" -> block.put("text", delta.get("text"));
                        case "input_json_delta" -> fragments.get(index).append((String) delta.get("partial_json"));
                        case "thinking_delta" -> block.put("thinking", String.valueOf(block.getOrDefault("thinking", "")) + delta.get("thinking"));
                        case "signature_delta" -> block.put("signature", delta.get("signature"));
                        default -> { }
                    }
                }
                case "content_block_stop" -> {
                    int index = ((Number) event.get("index")).intValue();
                    if (fragments.containsKey(index)) {
                        String joined = fragments.get(index).toString();
                        blocks.get(index).put("input", joined.isBlank() ? new LinkedHashMap<String, Object>() : Json.parse(joined));
                    }
                }
                case "message_delta" -> {
                    Map<String, Object> delta = (Map<String, Object>) event.get("delta");
                    message.put("stop_reason", delta.get("stop_reason"));
                    message.put("stop_sequence", delta.get("stop_sequence"));
                    if (event.get("usage") instanceof Map<?, ?> u) ((Map<String, Object>) message.get("usage")).putAll((Map<String, Object>) u);
                }
                case "message_stop" -> stopped = true;
                case "error" -> {
                    Map<String, Object> error = (Map<String, Object>) event.get("error");
                    throw new StreamError(String.valueOf(error.getOrDefault("type", "unknown")), String.valueOf(error.getOrDefault("message", "")));
                }
                default -> { } // ping and event types this client does not know are skipped: new types may be added
            }
        }
        if (message == null || !stopped) throw new StreamError("incomplete_stream", "the stream ended before message_stop");
        message.put("content", new ArrayList<>(blocks.values()));
        return message;
    }
}
