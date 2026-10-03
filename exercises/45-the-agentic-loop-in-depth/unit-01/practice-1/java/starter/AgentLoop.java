import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** The agent loop, driven by the stop reason. See ../../statement.md. Messages and replies are JSON-like maps. */
final class AgentLoop {
    private AgentLoop() {}

    static Map<String, Object> runAgent(Function<List<Map<String, Object>>, Map<String, Object>> model, Map<String, Function<Map<String, Object>, String>> tools, String task) {
        return runAgent(model, tools, task, 8);
    }

    static Map<String, Object> runAgent(Function<List<Map<String, Object>>, Map<String, Object>> model, Map<String, Function<Map<String, Object>, String>> tools, String task, int maxTurns) {
        // TODO: send the task, run the tool calls the model asks for, and decide what to do next from the stop reason only.
        return null;
    }
}
