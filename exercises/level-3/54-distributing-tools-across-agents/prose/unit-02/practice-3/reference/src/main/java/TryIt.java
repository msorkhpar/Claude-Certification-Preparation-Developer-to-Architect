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

        // The catalog and roles the tests use for the first main case m1.
        List<Map<String, Object>> catalog = List.of(
            Map.of("name", "web_search", "tags", List.of("web")),
            Map.of("name", "fetch_page", "tags", List.of("web")),
            Map.of("name", "load_document", "tags", List.of("documents")),
            Map.of("name", "verify_fact", "tags", List.of("web"), "scoped", true),
            Map.of("name", "publish_report", "tags", List.of("reports"), "irreversible", true));
        Map<String, Map<String, Object>> roles = new LinkedHashMap<>();
        roles.put("searcher", Map.of("specialisation", List.of("web")));
        roles.put("analyst", Map.of("specialisation", List.of("documents")));

        Map<String, List<String>> assigned = Distribute.assignTools(roles, catalog);
        System.out.println("searcher tools: " + (assigned == null ? null : assigned.get("searcher")));
        System.out.println("analyst tools: " + (assigned == null ? null : assigned.get("analyst")));

        // A call to a tool that cannot be undone, checked against the policy.
        Map<String, Object> policy = Map.of("tools", Map.of("process_refund", Map.of("cap", 500, "irreversible", true)));
        Map<String, Object> call = Map.of("id", "c1", "tool", "process_refund", "amount", 50, "customer", "C-1", "verified_customer", "C-1");
        System.out.println("without approval: " + Distribute.authorize(call, policy, List.of()));
        System.out.println("with approval: " + Distribute.authorize(call, policy, List.of("c1")));
    }
}
