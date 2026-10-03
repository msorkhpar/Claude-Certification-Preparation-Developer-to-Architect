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

    Conversation(Send send, String model, int maxTokens, String system, List<String> stopSequences) {
        this.send = send;
        this.model = model;
        this.maxTokens = maxTokens;
        this.system = system;
        this.stopSequences = stopSequences;
    }

    Reply say(String text) {
        // TODO: add the user turn, send the whole history, keep the assistant turn, count usage.
        return new Reply("", "", false);
    }

    List<Map<String, Object>> history() {
        return List.of();
    }

    Map<String, Long> totals() {
        Map<String, Long> totals = new LinkedHashMap<>();
        totals.put("input_tokens", 0L);
        totals.put("output_tokens", 0L);
        return totals;
    }

    void reset() {}
}
