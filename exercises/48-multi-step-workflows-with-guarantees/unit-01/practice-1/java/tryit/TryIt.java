import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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

        Map<String, Object> order = Map.of("order_id", "O1", "customer_id", "C1", "total_cents", 5000, "refunded_cents", 0);
        List<Map<String, Object>> refunds = new ArrayList<>();

        // A scripted backend, like the one the tests use: four functions that answer with fixed data.
        Map<String, Function<Map<String, Object>, Map<String, Object>>> backend = new LinkedHashMap<>();
        backend.put("verify_identity", call -> "1234".equals(call.get("code")) ? Map.of("verified", "yes", "customer_id", "C1") : Map.of("verified", "no"));
        backend.put("lookup_order", call -> new LinkedHashMap<>(order));
        backend.put("process_refund", call -> {
            refunds.add(call);
            return Map.of("refund_id", "R" + refunds.size(), "amount_cents", call.get("amount_cents"));
        });
        backend.put("escalate", call -> Map.of("ticket_id", "T1"));
        RefundDesk desk = new RefundDesk(backend);

        // The model breaks the order of the steps: a refund before identity is verified must be blocked in code.
        Map<String, Object> early = desk.call("process_refund", Map.of("order_id", "O1", "amount_cents", 2000));
        System.out.println("refund before verifying: " + (early == null ? null : early.get("content") + " | is_error: " + early.get("is_error")));

        Map<String, Map<String, Object>> steps = new LinkedHashMap<>();
        steps.put("verify_identity", Map.of("code", "1234"));
        steps.put("lookup_order", Map.of("order_id", "O1"));
        steps.put("process_refund", Map.of("order_id", "O1", "amount_cents", 2000));
        for (var step : steps.entrySet()) {
            Map<String, Object> out = desk.call(step.getKey(), step.getValue());
            System.out.println(step.getKey() + ": " + (out == null ? null : out.get("content")));
        }
        System.out.println("refunds that reached the backend: " + refunds.size());
    }
}
