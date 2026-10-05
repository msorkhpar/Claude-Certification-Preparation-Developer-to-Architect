import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** The agent loop, driven by the stop reason. See ../../statement.md. Messages and replies are JSON-like maps. */
final class AgentLoop {
    private static final System.Logger LOG = System.getLogger(AgentLoop.class.getName());
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

    private static List<Map<String, Object>> callsOf(List<Map<String, Object>> content) {
        List<Map<String, Object>> calls = new ArrayList<>();
        for (Map<String, Object> block : content) if ("tool_use".equals(block.get("type"))) calls.add(block);
        return calls;
    }

    private static Map<String, Object> toolResult(Map<String, Object> block, String content, boolean isError) {
        Map<String, Object> result = map("type", "tool_result", "tool_use_id", block.get("id"), "content", content);
        if (isError) result.put("is_error", true);
        return result;
    }

    private static String statusFor(String reason, List<Map<String, Object>> calls) {
        if (reason.equals("tool_use") && calls.isEmpty()) return "malformed";
        return switch (reason) {
            case "end_turn", "stop_sequence" -> "done";
            case "max_tokens" -> "truncated";
            case "refusal" -> "refused";
            default -> "unexpected";
        };
    }

    private static boolean atLimit(int turns, int maxTurns) {
        return turns >= maxTurns;
    }

    private static List<Map<String, Object>> snapshot(List<Map<String, Object>> messages) {
        return new ArrayList<>(messages);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> runTool(Map<String, Object> block, Map<String, Function<Map<String, Object>, String>> tools) {
        String name = (String) block.get("name");
        Function<Map<String, Object>, String> handler = tools.get(name);
        if (handler == null) return toolResult(block, "Unknown tool: " + name, true);
        try {
            Object input = block.get("input");
            return toolResult(block, handler.apply(input == null ? Map.of() : (Map<String, Object>) input), false);
        } catch (RuntimeException error) { // a failing tool is a result for the model, not a crash of the loop
            return toolResult(block, String.valueOf(error.getMessage()), true);
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
        LOG.log(System.Logger.Level.DEBUG, "runAgent input: {0}", task);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(map("role", "user", "content", task));
        int turns = 0;
        String lastText = "";
        while (true) {
            if (atLimit(turns, maxTurns)) return outcome("max_turns", lastText, turns, messages); // the count is a backstop: it only ends a run the model has not ended itself
            turns++;
            Map<String, Object> reply = model.apply(snapshot(messages));
            List<Map<String, Object>> content = (List<Map<String, Object>>) reply.get("content");
            lastText = textOf(content);
            messages.add(map("role", "assistant", "content", content));
            String reason = (String) reply.get("stop_reason");
            List<Map<String, Object>> calls = callsOf(content);
            if (!"tool_use".equals(reason) || calls.isEmpty()) return outcome(statusFor(reason, calls), lastText, turns, messages);
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> block : calls) results.add(runTool(block, tools));
            messages.add(map("role", "user", "content", results));
        }
    }
}
