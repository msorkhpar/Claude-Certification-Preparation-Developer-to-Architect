import java.util.*;
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

        // Prompt modules: static ones first (they can be cached), then the changing ones by priority.
        Map<String, Object> role = Map.of("name", "role", "static", true, "text", "r".repeat(400));
        Map<String, Object> policy = Map.of("name", "policy", "static", true, "text", "p".repeat(1648));
        Map<String, Object> history = Map.of("name", "history", "static", false, "priority", 1, "text", "h".repeat(200));
        Map<String, Object> question = Map.of("name", "question", "static", false, "priority", 9, "text", "Q: {q}");

        Map<String, Object> prompt = PromptPlan.assemble(List.of(question, role, history, policy), Map.of("q", "hello"), 10_000);
        System.out.println("prompt: " + prompt);

        // With a small budget the lowest-priority changing module is dropped.
        Map<String, Object> small = PromptPlan.assemble(List.of(question, role, history, policy), Map.of("q", "hello"), 520);
        System.out.println("dropped with a budget of 520: " + (small == null ? null : small.get("dropped")));

        // The cheapest model that meets the tier and the latency.
        List<Map<String, Object>> models = List.of(Map.of("name", "haiku", "tier", 1, "latency_ms", 300, "price_out", 5),
            Map.of("name", "sonnet", "tier", 2, "latency_ms", 900, "price_out", 15));
        System.out.println("model: " + PromptPlan.chooseModel(Map.of("tier", 2, "max_latency_ms", 1000), models));
    }
}
