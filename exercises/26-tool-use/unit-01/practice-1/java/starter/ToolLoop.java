import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** A tool loop against a scripted model. See ../../statement.md. Messages and replies are JSON-like maps. */
final class ToolLoop {
    private ToolLoop() {}

    static Map<String, Object> runAgent(Function<Map<String, Object>, Map<String, Object>> ask, List<Tool> tools, String userText) {
        return runAgent(ask, tools, userText, "claude-sonnet-5-5", 8, null);
    }

    static Map<String, Object> runAgent(Function<Map<String, Object>, Map<String, Object>> ask, List<Tool> tools, String userText, String model, int maxTurns,
            Map<String, Object> toolChoice) {
        // TODO: call the model, run every tool it asks for, send the results back, until it ends its turn or a limit is hit.
        return null;
    }
}
