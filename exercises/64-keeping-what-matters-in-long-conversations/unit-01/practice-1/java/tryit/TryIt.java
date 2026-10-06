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

        // A tool result is large; keep only the fields the next turn needs, with their exact values.
        Map<String, String> order = new LinkedHashMap<>();
        order.put("order_id", "A-1042");
        order.put("items", "2 x kettle");
        order.put("warehouse_bin", "R7-22");
        order.put("refund_amount", "$129.50");
        System.out.println("trimmed record: " + ContextBuilder.trimRecord(order, List.of("refund_amount", "order_id")));

        // A newer value replaces the current one; the old one is kept as superseded.
        Map<String, ContextBuilder.Fact> facts = ContextBuilder.updateFacts(new LinkedHashMap<>(), "address", "12 Oak St", "2026-08-01");
        facts = ContextBuilder.updateFacts(facts, "address", "9 Elm Rd", "2026-09-10");
        System.out.println("facts: " + facts);

        // The context of the next request: case facts, then the summary, then the recent messages.
        List<ContextBuilder.FactEntry> caseFacts = List.of(new ContextBuilder.FactEntry("c1", "refund", "$129.50", "2026-09-02"));
        List<ContextBuilder.Message> recent = List.of(new ContextBuilder.Message("user", "text", "m1", "Where is my refund?"));
        System.out.println(ContextBuilder.buildContext("c1", caseFacts, "The customer asked about a refund.", recent));

        // A tool call and its result stay together when the window drops old messages.
        List<ContextBuilder.Message> messages = List.of(new ContextBuilder.Message("user", "text", "m1", "x".repeat(40)),
            new ContextBuilder.Message("assistant", "tool_use", "t1", "lookup"), new ContextBuilder.Message("user", "tool_result", "t1", "found"));
        System.out.println("window ids: " + ContextBuilder.window(messages, 6).stream().map(ContextBuilder.Message::id).toList());
    }
}
