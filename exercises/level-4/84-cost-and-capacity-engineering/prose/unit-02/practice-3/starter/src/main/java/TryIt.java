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

        Map<String, Object> policy = Map.of("allowed", List.of("haiku", "sonnet"), "routes", Map.of("classify", "haiku", "draft", "sonnet", "review", "opus"),
            "default", "sonnet", "cheaper", Map.of("opus", "sonnet", "sonnet", "haiku"));

        // The gateway first checks the team's budget, then routes the request: near the limit it picks a cheaper model.
        for (long spend : new long[] {0, 850, 990}) {
            String status = GatewayBudget.admit(spend, 1000, 100);
            System.out.println("spend " + spend + "/1000 -> " + status + ": a review request goes to " + GatewayBudget.route(Map.of("task", "review"), policy, status));
        }

        // A delivery check: does a 95th-percentile latency of 40 s leave 20 percent of margin under a 60 s timeout?
        System.out.println("delivery: " + GatewayBudget.delivery(40, 60, 20));
    }
}
