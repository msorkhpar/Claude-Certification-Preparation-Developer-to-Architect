import java.util.*;
import java.util.function.Function;

final class Agent {
    interface Model { Map<String, Object> call(List<Map<String, Object>> messages); }

    @SuppressWarnings("unchecked")
    static String text(Object content) {
        var sb = new StringBuilder();
        for (var b : (List<Map<String, Object>>) content) if ("text".equals(b.get("type"))) sb.append(b.get("text"));
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    static String run(Model model, Map<String, Function<Map<String, Object>, Object>> tools, String userText, int maxTurns) {
        var messages = new ArrayList<Map<String, Object>>();
        messages.add(Map.of("role", "user", "content", userText));
        for (int turn = 0; turn < maxTurns; turn++) {
            var resp = model.call(messages);
            messages.add(Map.of("role", "assistant", "content", resp.get("content")));
            if (!"tool_use".equals(resp.get("stop_reason"))) return text(resp.get("content"));
            var results = new ArrayList<Map<String, Object>>();
            for (var block : (List<Map<String, Object>>) resp.get("content")) {
                if (!"tool_use".equals(block.get("type"))) continue;
                var id = block.get("id");
                try {
                    var tool = tools.get((String) block.get("name"));
                    if (tool == null) throw new IllegalArgumentException("unknown tool " + block.get("name"));
                    results.add(Map.of("type", "tool_result", "tool_use_id", id,
                        "content", String.valueOf(tool.apply((Map<String, Object>) block.get("input")))));
                } catch (RuntimeException e) {
                    results.add(Map.of("type", "tool_result", "tool_use_id", id, "content", String.valueOf(e.getMessage()), "is_error", true));
                }
            }
            for (var r : results) messages.add(Map.of("role", "user", "content", List.of(r)));
        }
        throw new IllegalStateException("max_turns exceeded");
    }
}
