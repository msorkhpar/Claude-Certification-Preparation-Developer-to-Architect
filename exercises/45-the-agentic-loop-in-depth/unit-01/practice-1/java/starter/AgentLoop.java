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

    /**
     * TODO 1 of 6 (unlocks m1 and e5): the text of a reply.
     * Receives a reply's content list of blocks. Returns the "text" of every block whose type is "text", joined with nothing between.
     * Example: [text "Hi ", tool_use, text "there"] -> "Hi there"
     */
    private static String textOf(List<Map<String, Object>> content) {
        return "";
    }

    /**
     * TODO 2 of 6 (unlocks e2 and e6): the tool calls of a reply.
     * Receives a reply's content list. Returns its tool_use blocks, in order; an empty list when there are none.
     * Example: [text "x", tool_use a] -> [tool_use a]
     */
    private static List<Map<String, Object>> callsOf(List<Map<String, Object>> content) {
        return new ArrayList<>();
    }

    /**
     * TODO 3 of 6 (unlocks m1, e2 and e3): one tool_result block.
     * Receives the tool_use block, the result text and a flag. Returns {type: tool_result, tool_use_id: the block's id, content: the text},
     * with is_error true added only when the flag is set (a good result has no is_error key). Use map(...) to build it.
     * Example: (block with id t1, "boom", true) -> {type: tool_result, tool_use_id: t1, content: boom, is_error: true}
     */
    private static Map<String, Object> toolResult(Map<String, Object> block, String content, boolean isError) {
        return map();
    }

    /**
     * TODO 4 of 6 (unlocks m1 and e5): the status a stop reason other than tool_use ends the run with.
     * Receives the stop reason. Returns "done" for end_turn and stop_sequence, "truncated" for max_tokens, "refused" for refusal and
     * "unexpected" for anything else. Example: "max_tokens" -> "truncated", "brand_new" -> "unexpected"
     */
    private static String statusFor(String reason) {
        return "";
    }

    /**
     * TODO 5 of 6 (unlocks e4): has the turn limit been reached before the next model call?
     * Receives the model calls made so far and the limit. Returns true when no call is left. Example: (3, 3) -> true, (2, 3) -> false
     */
    private static boolean atLimit(int turns, int maxTurns) {
        return false;
    }

    /**
     * TODO 6 of 6 (unlocks m1): the copy of the messages that the model is handed.
     * Receives the list of messages so far. Returns a new list with the same items, so later changes do not rewrite what the model saw.
     * Example: [a, b] -> a new list [a, b]
     */
    private static List<Map<String, Object>> snapshot(List<Map<String, Object>> messages) {
        return messages;
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
            switch (reason) {
                case "tool_use" -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (Map<String, Object> block : callsOf(content)) results.add(runTool(block, tools));
                    if (results.isEmpty()) return outcome("malformed", lastText, turns, messages);
                    messages.add(map("role", "user", "content", results));
                }
                default -> { return outcome(statusFor(reason), lastText, turns, messages); }
            }
        }
    }
}
