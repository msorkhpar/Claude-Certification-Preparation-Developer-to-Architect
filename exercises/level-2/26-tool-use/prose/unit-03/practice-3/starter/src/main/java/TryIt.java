import java.util.ArrayList;
import java.util.List;
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

        Map<String, Object> schema = Map.of("type", "object", "properties", Map.of("city", Map.of("type", "string")), "required", List.of("city"));
        List<Tool> tools = List.of(new Tool("get_weather", "Current weather for a city.", schema, input -> input.get("city") + ": 18 C"));

        // A stand-in for the model, like the one the tests use: it asks for one tool call, then ends its turn.
        List<Map<String, Object>> replies = new ArrayList<>(List.of(
            Map.of("content", List.of(Map.of("type", "text", "text", "Let me check."),
                    Map.of("type", "tool_use", "id", "tu_1", "name", "get_weather", "input", Map.of("city", "Oslo"))),
                "stop_reason", "tool_use"),
            Map.of("content", List.of(Map.of("type", "text", "text", "It is 18 C in Oslo.")), "stop_reason", "end_turn")));

        Map<String, Object> result = ToolLoop.runAgent(request -> replies.remove(0), tools, "Weather in Oslo?");

        System.out.println("status: " + result.get("status"));
        System.out.println("text: " + result.get("text"));
        System.out.println("model calls: " + result.get("turns"));
        System.out.println("messages: " + Json.stringify(result.get("messages")));
    }
}
