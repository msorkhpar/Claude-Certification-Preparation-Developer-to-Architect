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
        // TODO 1 of 7 (finish this to pass e5): refuse a tool_choice the API would reject.
        // Receives the tools, the model id and the tool_choice map. Throws RequestError, in this order: field "tool_choice.type" unless the type is in
        // CHOICE_TYPES; "tool_choice.disable_parallel_tool_use" when that key is present and not a Boolean; "tool_choice" for type "any" or "tool" on a
        // model in FORCED_UNSUPPORTED; "tool_choice.name" for type "tool" when no tool has that name. Returns normally otherwise.
        // Example: checkChoice(List.of(), "claude-opus-5-5", Map.of("type", "any")) throws a RequestError with field "tool_choice"
    }

    /** The keys listed in the tool's required list that the input lacks. */
    @SuppressWarnings("unchecked")
    private static List<String> missingInputs(Tool tool, Map<String, Object> input) {
        // TODO 2 of 7 (finish this to pass e2): the required keys a call leaves out.
        // Receives a tool and the input map of the call. Returns the keys in the tool's inputSchema "required" list that the input lacks, in the
        // order of that list; an empty list when none is missing.
        // Example: a tool whose inputSchema requires "city", with an empty input -> [city]
        return new ArrayList<>();
    }

    /** The content of a tool result: a string as it is, any other value as JSON text. */
    private static String resultContent(Object out) {
        // TODO 3 of 7 (finish this to pass e6): the content of a tool_result.
        // Receives what a handler returned. Returns a String as it is and any other value as JSON text (use Json.stringify).
        // Example: resultContent(Map.of("city", "Oslo")) -> {"city":"Oslo"}
        return String.valueOf(out);
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
        // TODO 4 of 7 (finish this to pass m1 and e1): run every tool call of a reply.
        // Receives the tools and the content blocks of the reply. Returns one tool_result (from runOne) per block of type "tool_use", in the
        // order of the blocks; blocks of any other type, such as "server_tool_use", get no result.
        // Example: [a text block, a tool_use block] -> a list with one tool_result
        return new ArrayList<>();
    }

    /** The status of a reply that ends the loop: done, refused or truncated. */
    private static String finalStatus(String stop) {
        // TODO 5 of 7 (finish this to pass e4): the status of a reply that ends the loop.
        // Receives the stop_reason. Returns "done" for "end_turn" and "stop_sequence", "refused" for "refusal" and "truncated" for any other.
        // Example: finalStatus("max_tokens") -> "truncated"
        return "done";
    }

    /** The last turn number the loop may use. */
    private static int lastTurn(int maxTurns) {
        // TODO 6 of 7 (finish this to pass e3): the last turn number the loop may use.
        // Receives maxTurns. Returns the highest turn number, so that at most maxTurns calls are made.
        // Example: lastTurn(3) -> 3
        return 1;
    }

    /** The tool_choice to send on this turn: a forced choice only on the first request. */
    private static Map<String, Object> sentChoice(Map<String, Object> toolChoice, int turn) {
        // TODO 7 of 7 (finish this to pass e5): the tool_choice to send on a turn.
        // Receives the tool_choice map and the turn number (1 for the first request). A forced choice (type "any" or "tool") is sent as given on
        // turn 1 and as {type: auto} from turn 2 on; "auto" and "none" are sent unchanged every turn.
        // Example: sentChoice(Map.of("type", "any"), 2) -> {type=auto}
        return toolChoice;
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
