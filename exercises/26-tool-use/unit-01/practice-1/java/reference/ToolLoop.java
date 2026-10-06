import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** A tool loop against a scripted model. See ../../statement.md. Messages and replies are JSON-like maps. */
final class ToolLoop {
    private static final System.Logger LOG = System.getLogger(ToolLoop.class.getName());
    private ToolLoop() {}

    private static final Set<String> FORCED_UNSUPPORTED = Set.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1");
    private static final Set<String> CHOICE_TYPES = Set.of("auto", "any", "tool", "none");

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** Refuse a tool_choice the API would reject. */
    private static void checkChoice(List<Tool> tools, String model, Map<String, Object> choice) {
        Object kind = choice.get("type");
        if (!CHOICE_TYPES.contains(kind)) throw new RequestError("tool_choice.type", "must be auto, any, tool or none");
        if (choice.containsKey("disable_parallel_tool_use") && !(choice.get("disable_parallel_tool_use") instanceof Boolean)) {
            throw new RequestError("tool_choice.disable_parallel_tool_use", "must be a boolean");
        }
        if (("any".equals(kind) || "tool".equals(kind)) && FORCED_UNSUPPORTED.contains(model)) throw new RequestError("tool_choice", model + " does not support forced tool use");
        if ("tool".equals(kind) && tools.stream().noneMatch(t -> t.name().equals(choice.get("name")))) throw new RequestError("tool_choice.name", "names no tool in the request");
    }

    /** The keys listed in the tool's required list that the input lacks. */
    @SuppressWarnings("unchecked")
    private static List<String> missingInputs(Tool tool, Map<String, Object> input) {
        List<String> missing = new ArrayList<>();
        for (Object key : (List<Object>) tool.inputSchema().getOrDefault("required", List.of())) if (!input.containsKey(key)) missing.add((String) key);
        return missing;
    }

    /** The content of a tool result: a string as it is, any other value as JSON text. */
    private static String resultContent(Object out) {
        return out instanceof String s ? s : Json.stringify(out);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> runOne(List<Tool> tools, Map<String, Object> block) {
        Map<String, Object> result = map("type", "tool_result", "tool_use_id", block.get("id"));
        Tool tool = tools.stream().filter(t -> t.name().equals(block.get("name"))).findFirst().orElse(null);
        if (tool == null) {
            result.put("content", "Unknown tool: " + block.get("name"));
            result.put("is_error", true);
            return result;
        }
        Map<String, Object> input = (Map<String, Object>) block.get("input");
        List<String> missing = missingInputs(tool, input);
        if (!missing.isEmpty()) {
            result.put("content", "Missing required input: " + String.join(", ", missing));
            result.put("is_error", true);
            return result;
        }
        try {
            Object out = tool.handler().apply(input);
            result.put("content", resultContent(out));
        } catch (RuntimeException e) { // a tool that fails must not end the loop
            result.put("content", e.getMessage());
            result.put("is_error", true);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static String textOf(List<Map<String, Object>> content) {
        StringBuilder out = new StringBuilder();
        for (Map<String, Object> b : content) if ("text".equals(b.get("type"))) out.append(b.get("text"));
        return out.toString();
    }

    /** One tool_result per tool_use block of the reply, in order. */
    private static List<Object> toolResults(List<Tool> tools, List<Map<String, Object>> content) {
        List<Object> results = new ArrayList<>();
        for (Map<String, Object> b : content) if ("tool_use".equals(b.get("type"))) results.add(runOne(tools, b));
        return results;
    }

    /** The status of a reply that ends the loop: done, refused or truncated. */
    private static String finalStatus(String stop) {
        if (stop.equals("end_turn") || stop.equals("stop_sequence")) return "done";
        if (stop.equals("refusal")) return "refused";
        return "truncated"; // max_tokens, model_context_window_exceeded: the answer is cut off
    }

    /** The last turn number the loop may use. */
    private static int lastTurn(int maxTurns) {
        return maxTurns;
    }

    /** The tool_choice to send on this turn: a forced choice only on the first request. */
    private static Map<String, Object> sentChoice(Map<String, Object> toolChoice, int turn) {
        boolean forced = "any".equals(toolChoice.get("type")) || "tool".equals(toolChoice.get("type"));
        return forced && turn > 1 ? map("type", "auto") : toolChoice;
    }

    private static Map<String, Object> outcome(String status, String text, int turns, List<Map<String, Object>> messages) {
        return map("status", status, "text", text, "turns", turns, "messages", messages);
    }

    static Map<String, Object> runAgent(Function<Map<String, Object>, Map<String, Object>> ask, List<Tool> tools, String userText) {
        return runAgent(ask, tools, userText, "claude-sonnet-5-5", 8, null);
    }

    /** Call the model, run every tool it asks for, send the results back, until it ends its turn or a limit is hit. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> runAgent(Function<Map<String, Object>, Map<String, Object>> ask, List<Tool> tools, String userText, String model, int maxTurns,
            Map<String, Object> toolChoice) {
        LOG.log(System.Logger.Level.DEBUG, "runAgent input: {0}", userText);
        if (toolChoice != null) checkChoice(tools, model, toolChoice);
        List<Object> definitions = new ArrayList<>();
        for (Tool t : tools) definitions.add(map("name", t.name(), "description", t.description(), "input_schema", t.inputSchema()));
        List<Map<String, Object>> messages = new ArrayList<>(List.of(map("role", "user", "content", userText)));
        String text = "";
        int calls = 0;
        for (int turn = 1; turn <= lastTurn(maxTurns); turn++) {
            Map<String, Object> request = map("model", model, "max_tokens", 1024, "messages", new ArrayList<>(messages), "tools", definitions);
            if (toolChoice != null) request.put("tool_choice", sentChoice(toolChoice, turn));
            Map<String, Object> reply = ask.apply(request);
            calls = turn;
            List<Map<String, Object>> content = (List<Map<String, Object>>) reply.get("content");
            messages.add(map("role", "assistant", "content", content));
            text = textOf(content);
            String stop = (String) reply.get("stop_reason");
            if (stop.equals("tool_use")) {
                messages.add(map("role", "user", "content", toolResults(tools, content)));
            } else if (stop.equals("pause_turn")) {
                continue;
            } else {
                return outcome(finalStatus(stop), text, calls, messages);
            }
        }
        return outcome("max_turns", text, calls, messages);
    }
}
