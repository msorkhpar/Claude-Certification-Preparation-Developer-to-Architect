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

        // A stand-in for a worker agent, like the tests use: its first call is lost, then it answers.
        int[] calls = {0};
        ReliableAgents.Agent flaky = (key, inputs) -> {
            calls[0]++;
            if (calls[0] == 1) throw new ReliableAgents.Transient("lost");
            return key + "|" + String.join(",", new TreeMap<>(inputs).values());
        };

        // Task b needs the result of a; the lost call is retried, the result is checkpointed in the store.
        Map<String, String> store = new LinkedHashMap<>();
        Map<String, Object> result = ReliableAgents.runPlan(List.of(task("a", List.of()), task("b", List.of("a"))), Map.of("w", flaky), store, 3, 3);
        System.out.println("done: " + (result == null ? null : result.get("done")));
        System.out.println("attempts: " + (result == null ? null : result.get("attempts")));
        System.out.println("failed: " + (result == null ? null : result.get("failed")) + " | skipped: " + (result == null ? null : result.get("skipped")));
        System.out.println("store: " + store);
    }

    static Map<String, Object> task(String id, List<String> needs) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", id);
        t.put("agent", "w");
        t.put("key", "key-" + id);
        t.put("needs", needs);
        t.put("fallback", null);
        return t;
    }
}
