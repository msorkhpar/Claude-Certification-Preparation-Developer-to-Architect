import java.util.*;

/** Hand-written, illustrative scripted model: replies in the Messages API shape (maps and lists). */
final class Scripted {
    static Map<String, Object> text(String t) { return Map.of("type", "text", "text", t); }
    static Map<String, Object> toolUse(String id, String name, Map<String, Object> input) {
        return Map.of("type", "tool_use", "id", id, "name", name, "input", input);
    }
    static Map<String, Object> reply(List<Map<String, Object>> content, String stopReason) {
        return Map.of("type", "message", "role", "assistant", "content", content, "stop_reason", stopReason);
    }

    static final class Model implements Agent.Model {
        final Deque<Map<String, Object>> replies = new ArrayDeque<>();
        final List<List<Map<String, Object>>> seen = new ArrayList<>();
        @SafeVarargs Model(Map<String, Object>... r) { replies.addAll(List.of(r)); }
        public Map<String, Object> call(List<Map<String, Object>> messages) {
            List<Map<String, Object>> copy = new ArrayList<>();
            for (Map<String, Object> m : messages) copy.add(new HashMap<>(m));
            seen.add(copy);
            if (replies.isEmpty()) throw new AssertionError("scripted model ran out of replies");
            return replies.poll();
        }
    }
}
