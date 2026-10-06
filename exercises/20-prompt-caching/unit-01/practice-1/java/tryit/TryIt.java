import java.util.LinkedHashMap;
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

        // A request whose blocks arrive in the wrong order: the volatile date sits first, in the system prompt.
        List<Map<String, Object>> blocks = List.of(
            block("date", "system", 20, "volatile", true),
            block("tools", "tools", 2000, null, null),
            block("rules", "system", 3000, "breakpoint", true),
            block("manual", "messages", 6000, "breakpoint", true),
            block("question", "messages", 40, null, null));
        try {
            List<Map<String, Object>> plan = CachePlan.planRequest(blocks, 1024);
            System.out.println("order: " + plan.stream().map(p -> p.get("id")).toList());
            System.out.println("cache per block: " + plan.stream().map(p -> p.get("id") + "=" + p.get("cache")).toList());
            System.out.println("breakpoints at: " + plan.stream().filter(p -> p.get("cache") != null).map(p -> p.get("id")).toList());
        } catch (PlanError err) {
            System.out.println("plan error: " + err.getMessage());
        }
    }

    private static Map<String, Object> block(String id, String section, int tokens, String flag, Object value) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("id", id);
        b.put("section", section);
        b.put("tokens", tokens);
        if (flag != null) b.put(flag, value);
        return b;
    }
}
