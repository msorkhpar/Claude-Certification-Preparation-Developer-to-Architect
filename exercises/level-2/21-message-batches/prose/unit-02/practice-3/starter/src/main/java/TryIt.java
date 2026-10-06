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

        String model = "claude-haiku-4-5-20251001";
        List<Map<String, Object>> items = new ArrayList<>();
        for (String id : List.of("t-1", "t-2", "t-3")) {
            items.add(Map.of("id", id, "params", Map.of("model", model, "max_tokens", 200,
                "messages", List.of(Map.of("role", "user", "content", "Classify ticket " + id)))));
        }

        try {
            List<Map<String, Object>> requests = Batches.buildRequests(items);
            System.out.println("custom ids: " + requests.stream().map(r -> r.get("custom_id")).toList());
            // The results come back in any order: they are matched by custom_id, not by position.
            Map<String, Object> done = Batches.collect(requests, List.of(
                resultLine("t-3", "billing"), resultLine("t-1", "refund"), resultLine("t-2", "shipping")));
            List<String> outcomes = new ArrayList<>();
            for (Object o : (List<?>) done.getOrDefault("outcomes", List.of())) {
                Map<?, ?> m = (Map<?, ?>) o;
                outcomes.add(m.get("custom_id") + "/" + m.get("status") + "/" + m.get("text"));
            }
            System.out.println("outcomes: " + outcomes);
            System.out.println("to retry / fix / unknown: " + done.get("retry") + " " + done.get("fix") + " " + done.get("unknown"));
        } catch (BatchError err) {
            System.out.println("batch error: " + err.getMessage());
        }
    }

    /** One line of the .jsonl results file, like the ones the tests build. */
    private static String resultLine(String customId, String text) {
        return "{\"custom_id\":\"" + customId + "\",\"result\":{\"type\":\"succeeded\",\"message\":{\"id\":\"msg_illustrative\","
            + "\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-haiku-4-5-20251001\",\"stop_reason\":\"end_turn\","
            + "\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\"}],\"usage\":{\"input_tokens\":50,\"output_tokens\":7}}}}";
    }
}
