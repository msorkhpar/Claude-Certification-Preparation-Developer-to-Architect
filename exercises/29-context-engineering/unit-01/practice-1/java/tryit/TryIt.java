import java.util.LinkedHashMap;
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

        // A conversation of four turns, like the one the tests compact.
        List<Map<String, Object>> messages = new ArrayList<>(List.of(
            user("first question"), said("first answer " + "x".repeat(80)),
            user("second question"), call("a1", "search"), result("a1", "R".repeat(300)), said("second answer"),
            user("third question"), said("third answer " + "y".repeat(60)),
            user("fourth question"), call("a2", "search"), result("a2", "S".repeat(100)), said("fourth answer")));

        // A stand-in for the summariser model: it is told what to fold away and answers with a fixed text.
        java.util.function.Function<List<Map<String, Object>>, String> summarise = older -> {
            System.out.println("summariser asked to fold " + older.size() + " messages");
            return "The user asked three things.";
        };

        int budget = Context.countTokens(messages) / 2;
        List<Map<String, Object>> done = Context.compact(messages, budget, summarise, 1);

        System.out.println("tokens before: " + Context.countTokens(messages) + " | budget: " + budget);
        System.out.println("messages before and after: " + messages.size() + " -> " + (done == null ? 0 : done.size()));
        System.out.println("roles after: " + (done == null ? null : done.stream().map(m -> m.get("role")).toList()));
        System.out.println("first message: " + (done == null || done.isEmpty() ? null : done.get(0).get("content")));
    }

    private static Map<String, Object> user(String text) {
        return Map.of("role", "user", "content", text);
    }

    private static Map<String, Object> said(String text) {
        return Map.of("role", "assistant", "content", List.of(Map.of("type", "text", "text", text)));
    }

    private static Map<String, Object> call(String id, String name) {
        return Map.of("role", "assistant", "content", List.of(Map.of("type", "tool_use", "id", id, "name", name, "input", Map.of())));
    }

    private static Map<String, Object> result(String id, String text) {
        return Map.of("role", "user", "content", List.of(Map.of("type", "tool_result", "tool_use_id", id, "content", text)));
    }
}
