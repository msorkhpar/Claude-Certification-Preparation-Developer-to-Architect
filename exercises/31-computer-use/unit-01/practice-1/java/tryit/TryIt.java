import java.util.LinkedHashMap;
import java.util.Arrays;
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

        // A toy screen in memory: a search box and a payment button.
        Map<String, Object> screen = map("width", 1920, "height", 1080, "cursor", list(0, 0), "typed", "", "log", list(),
            "elements", list(map("id", "search", "x", 1000, "y", 200, "w", 400, "h", 40, "risk", "none"),
                map("id", "pay", "x", 800, "y", 600, "w", 200, "h", 60, "risk", "payment")));

        // The model is a function that returns scripted replies in order, like the one the tests use.
        List<Map<String, Object>> replies = new ArrayList<>(List.of(
            map("content", list(use("t1", "screenshot")), "stop_reason", "tool_use"),
            map("content", list(use("t2", "left_click", "coordinate", list(894, 164)), use("t3", "type", "text", "weather"), use("t4", "screenshot")), "stop_reason", "tool_use"),
            map("content", list(map("type", "text", "text", "Typed it.")), "stop_reason", "end_turn")));
        List<Integer> requests = new ArrayList<>();
        Function<Map<String, Object>, Map<String, Object>> ask = request -> {
            requests.add(((List<?>) request.get("messages")).size());
            // when the script runs out, the model just ends the conversation
    return replies.isEmpty() ? map("content", list(map("type", "text", "text", "script ran out")), "stop_reason", "end_turn") : replies.remove(0);
        };

        Map<String, Object> result = Computer.runComputerLoop(ask, screen);

        System.out.println("status and turns: " + (result == null ? null : result.get("status") + " " + result.get("turns")));
        System.out.println("messages in each request: " + requests);
        System.out.println("actions performed on the screen: " + screen.get("log"));
        System.out.println("typed text: " + screen.get("typed"));
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static List<Object> list(Object... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static Map<String, Object> use(String id, String name, Object... input) {
        return map("type", "tool_use", "id", id, "name", name, "toolset_name", "computer", "input", map(input));
    }
}
