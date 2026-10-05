import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main to try your code; Submit runs the tests. */
public class TryIt {
    public static void main(String[] args) {
        // Turn the logger up, so the LOG.log(DEBUG, ...) lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);

        // A scripted model, like the one the tests use: it asks for one tool call, then ends its turn.
        List<Map<String, Object>> replies = new ArrayList<>(List.of(
            Map.of("stop_reason", "tool_use", "content", List.of(Map.of("type", "text", "text", "Looking."),
                Map.of("type", "tool_use", "id", "t1", "name", "lookup", "input", Map.of("n", 1)))),
            Map.of("stop_reason", "end_turn", "content", List.of(Map.of("type", "text", "text", "Record 1 found.")))));
        Function<List<Map<String, Object>>, Map<String, Object>> model = messages -> {
            System.out.println("model called with " + messages.size() + " messages");
            return replies.isEmpty()
                ? Map.of("stop_reason", "end_turn", "content", List.of(Map.of("type", "text", "text", "script ran out")))
                : replies.remove(0);
        };
        Map<String, Function<Map<String, Object>, String>> tools = Map.of("lookup", arguments -> "record " + arguments.get("n"));

        Map<String, Object> result = AgentLoop.runAgent(model, tools, "find record 1");

        System.out.println("status: " + (result == null ? null : result.get("status")) + " | turns: " + (result == null ? null : result.get("turns")));
        System.out.println("final text: " + (result == null ? null : result.get("text")));
        System.out.println("roles: " + (result == null || !(result.get("messages") instanceof List<?> ms) ? null : ms.stream().map(m -> ((Map<?, ?>) m).get("role")).toList()));
    }
}
