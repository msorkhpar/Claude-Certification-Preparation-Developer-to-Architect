import java.util.function.Function;
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

        String doc = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business.";
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) Json.parse("""
            {"type":"object","required":["vendor","total","currency","evidence"],
             "properties":{"vendor":{"type":"string"},"total":{"type":"number","minimum":0},
                           "currency":{"type":"string","enum":["USD","EUR","GBP"]},"evidence":{"type":"string"}},
             "additionalProperties":false}""");
        String good = "{\"vendor\":\"Acme Tools\",\"total\":120.5,\"currency\":\"EUR\",\"evidence\":\"Total due: 120.50 EUR\"}";

        // A hand-written stand-in for the model, in the shape of a Messages API reply: it always answers with valid JSON.
        Function<List<Map<String, Object>>, Map<String, Object>> ask = messages ->
            Map.of("type", "message", "role", "assistant", "stop_reason", "end_turn",
                "content", List.of(Map.of("type", "text", "text", good)));

        Map<String, Object> result = Extractor.extract(ask, doc, schema, 3, List.of("evidence"));

        System.out.println("status: " + result.get("status"));
        System.out.println("attempts: " + result.get("attempts"));
        System.out.println("value: " + result.get("value"));
        System.out.println("errors: " + result.get("errors"));
    }
}
