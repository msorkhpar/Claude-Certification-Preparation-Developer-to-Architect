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

        // Two of the course models, with prices in dollars per million tokens, as the tests use them.
        Map<String, Object> haiku = model("claude-haiku-4-5-20251001", 1, 200_000, 64_000, 1, 5, 0.1);
        Map<String, Object> sonnet = model("claude-sonnet-5-5", 2, 1_000_000, 128_000, 2, 10, 0.1);
        List<Map<String, Object>> catalog = List.of(haiku, sonnet);
        Map<String, Object> usage = Map.of("input_tokens", 1200, "output_tokens", 300);

        System.out.println("sonnet cost (micro-dollars): " + Router.requestCost(sonnet, usage, false));
        System.out.println("haiku cost (micro-dollars): " + Router.requestCost(haiku, usage, false));
        try {
            System.out.println("tier 1 task goes to: " + Router.route(catalog, Map.of("min_tier", 1, "usage", usage)));
            System.out.println("tier 2 task goes to: " + Router.route(catalog, Map.of("min_tier", 2, "usage", usage)));
        } catch (NoModelError err) {
            System.out.println("no model: " + err.getMessage());
        }
    }

    private static Map<String, Object> model(String id, int tier, long context, long maxOutput, double input, double output, double mult) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tier", tier);
        m.put("context", context);
        m.put("max_output", maxOutput);
        m.put("input", input);
        m.put("output", output);
        m.put("cache_read_multiplier", mult);
        return m;
    }
}
