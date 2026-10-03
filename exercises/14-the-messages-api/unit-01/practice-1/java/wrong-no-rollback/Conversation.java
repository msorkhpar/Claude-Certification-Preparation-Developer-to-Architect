import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A conversation client that keeps the state the API does not. See ../../statement.md for the contract. */
final class Conversation {
    private final Send send;
    private final String model;
    private final int maxTokens;
    private final String system;
    private final List<String> stopSequences;
    private List<Map<String, Object>> history = new ArrayList<>();
    private long inputTokens;
    private long outputTokens;

    Conversation(Send send, String model, int maxTokens, String system, List<String> stopSequences) {
        this.send = send;
        this.model = model;
        this.maxTokens = maxTokens;
        this.system = system;
        this.stopSequences = stopSequences;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> copy(List<Map<String, Object>> turns) {
        return (List<Map<String, Object>>) Json.parse(Json.stringify(turns));
    }

    Reply say(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("a turn needs text");
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", text);
        history.add(user);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", (long) maxTokens);
        body.put("messages", copy(history));
        if (system != null && !system.isBlank()) body.put("system", system);
        if (stopSequences != null && !stopSequences.isEmpty()) body.put("stop_sequences", new ArrayList<>(stopSequences));
        Map<String, Object> response;
        try {
            response = send.send(body);
        } catch (RuntimeException e) {
            throw e;
        }
        Map<String, Object> assistant = new LinkedHashMap<>();
        assistant.put("role", "assistant");
        assistant.put("content", response.get("content"));
        history.add(assistant);
        if (response.get("usage") instanceof Map<?, ?> usage) {
            if (usage.get("input_tokens") instanceof Number n) inputTokens += n.longValue();
            if (usage.get("output_tokens") instanceof Number n) outputTokens += n.longValue();
        }
        StringBuilder replyText = new StringBuilder();
        for (Object b : (List<?>) response.get("content")) {
            if (b instanceof Map<?, ?> block && "text".equals(block.get("type"))) replyText.append(block.get("text"));
        }
        String stop = (String) response.get("stop_reason");
        return new Reply(replyText.toString(), stop, "max_tokens".equals(stop));
    }

    List<Map<String, Object>> history() {
        return copy(history);
    }

    Map<String, Long> totals() {
        Map<String, Long> totals = new LinkedHashMap<>();
        totals.put("input_tokens", inputTokens);
        totals.put("output_tokens", outputTokens);
        return totals;
    }

    void reset() {
        history = new ArrayList<>();
        inputTokens = 0;
        outputTokens = 0;
    }
}
