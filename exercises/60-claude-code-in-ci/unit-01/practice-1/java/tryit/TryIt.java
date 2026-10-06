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

        // A small schema of the model's answer (your review-schema.json is the full one) and the team's policy.
        String schemaText = "{\"type\":\"object\",\"required\":[\"findings\"],\"properties\":{\"findings\":{\"type\":\"array\",\"items\":{\"type\":\"object\","
            + "\"required\":[\"file\",\"line\",\"category\",\"severity\",\"issue\",\"suggested_fix\",\"detected_pattern\"],\"properties\":{"
            + "\"file\":{\"type\":\"string\"},\"line\":{\"type\":\"integer\"},\"category\":{\"type\":\"string\"},"
            + "\"severity\":{\"type\":\"string\",\"enum\":[\"low\",\"medium\",\"high\"]},\"issue\":{\"type\":\"string\"},"
            + "\"suggested_fix\":{\"type\":\"string\"},\"detected_pattern\":{\"type\":\"string\"}}}}}}";
        com.fasterxml.jackson.databind.JsonNode schema = json(schemaText);
        Map<String, Object> policy = Map.of("min_severity", "medium", "disabled_categories", List.of("style"), "fail_on", List.of("high"));

        // What `claude -p --output-format json` prints for a successful run with one finding.
        String stdout = "{\"type\":\"result\",\"subtype\":\"success\",\"is_error\":false,\"structured_output\":{\"findings\":[{\"file\":\"api.py\",\"line\":12,"
            + "\"category\":\"bug\",\"severity\":\"medium\",\"issue\":\"Unchecked None.\",\"suggested_fix\":\"Return early.\",\"detected_pattern\":\"missing-none-check\"}]}}";

        System.out.println("a valid run: " + ReviewGate.gate(stdout, 0, schema, policy));
        System.out.println("claude exited with 2: " + ReviewGate.gate(stdout, 2, schema, policy));
        System.out.println("not JSON at all: " + ReviewGate.gate("Error: no key", 1, schema, policy));
        System.out.println(ReviewGate.reviewPrompt("+ x = 1", List.of(), List.of()).lines().limit(3).toList());
    }

    static com.fasterxml.jackson.databind.JsonNode json(String text) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
