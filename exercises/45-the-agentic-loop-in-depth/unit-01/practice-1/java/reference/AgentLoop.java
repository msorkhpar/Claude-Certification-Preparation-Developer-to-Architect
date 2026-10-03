import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** The agent loop, driven by the stop reason. See ../../statement.md. Messages and replies are JSON-like maps. */
final class AgentLoop {
    private AgentLoop() {}

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static String textOf(List<Map<String, Object>> content) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> block : content) if ("text".equals(block.get("type"))) sb.append(block.getOrDefault("text", ""));
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> runTool(Map<String, Object> block, Map<String, Function<Map<String, Object>, String>> tools) {
        String name = (String) block.get("name");
        Function<Map<String, Object>, String> handler = tools.get(name);
        if (handler == null) return map("type", "tool_result", "tool_use_id", block.get("id"), "content", "Unknown tool: " + name, "is_error", true);
        try {
            Object input = block.get("input");
            return map("type", "tool_result", "tool_use_id", block.get("id"), "content", handler.apply(input == null ? Map.of() : (Map<String, Object>) input));
        } catch (RuntimeException error) { // a failing tool is a result for the model, not a crash of the loop
            return map("type", "tool_result", "tool_use_id", block.get("id"), "content", String.valueOf(error.getMessage()), "is_error", true);
        }
    }

    private static Map<String, Object> outcome(String status, String text, int turns, List<Map<String, Object>> messages) {
        return map("status", status, "text", text, "turns", turns, "messages", messages);
    }

    static Map<String, Object> runAgent(Function<List<Map<String, Object>>, Map<String, Object>> model, Map<String, Function<Map<String, Object>, String>> tools, String task) {
        return runAgent(model, tools, task, 8);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> runAgent(Function<List<Map<String, Object>>, Map<String, Object>> model, Map<String, Function<Map<String, Object>, String>> tools, String task, int maxTurns) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(map("role", "user", "content", task));
        int turns = 0;
        String lastText = "";
        while (true) {
            if (turns >= maxTurns) return outcome("max_turns", lastText, turns, messages); // the count is a backstop: it only ends a run the model has not ended itself
            turns++;
            Map<String, Object> reply = model.apply(new ArrayList<>(messages));
            List<Map<String, Object>> content = (List<Map<String, Object>>) reply.get("content");
            lastText = textOf(content);
            messages.add(map("role", "assistant", "content", content));
            String reason = (String) reply.get("stop_reason");
            switch (reason) {
                case "tool_use" -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (Map<String, Object> block : content) if ("tool_use".equals(block.get("type"))) results.add(runTool(block, tools));
                    if (results.isEmpty()) return outcome("malformed", lastText, turns, messages);
                    messages.add(map("role", "user", "content", results));
                }
                case "end_turn", "stop_sequence" -> { return outcome("done", lastText, turns, messages); }
                case "max_tokens" -> { return outcome("truncated", lastText, turns, messages); }
                case "refusal" -> { return outcome("refused", lastText, turns, messages); }
                default -> { return outcome("unexpected", lastText, turns, messages); }
            }
        }
    }
}
