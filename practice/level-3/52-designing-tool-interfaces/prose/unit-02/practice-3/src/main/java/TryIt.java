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

        // A well-made tool, like the one the tests start from: a clear description with when to use it and when not.
        Map<String, Object> good = Map.of(
            "name", "lookup_order",
            "description", "Looks up one order by its id and returns its status, items and total in cents. Use when the customer gives an order id "
                + "such as A-1042 or asks where an order is. Do not use it to find a customer by name; use get_customer instead of "
                + "this tool for that. It returns no payment details.",
            "input_schema", Map.of("type", "object", "required", List.of("order_id"),
                "properties", Map.of("order_id", Map.of("type", "string", "description", "The order id, for example A-1042."))),
            "input_examples", List.of(Map.of("order_id", "A-1042")),
            "annotations", Map.of("readOnlyHint", true));
        // A poor one: a vague name, a short description and a parameter nobody explained.
        Map<String, Object> poor = Map.of("name", "helper", "description", "Gets stuff.",
            "input_schema", Map.of("type", "object", "properties", Map.of("q", Map.of("type", "string")), "required", List.of("q")));

        System.out.println("good tool: " + Toolset.lintTool(good));
        var poorRules = Toolset.lintTool(poor);
        System.out.println("poor tool: " + poorRules);
        System.out.println("rules the poor tool breaks: " + (poorRules == null ? 0 : poorRules.size()));
    }
}
