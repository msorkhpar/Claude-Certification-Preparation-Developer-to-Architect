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

        Map<String, Capability.Tool> catalog = new LinkedHashMap<>();
        catalog.put("read_ticket", new Capability.Tool("read", 160));
        catalog.put("draft_reply", new Capability.Tool("draft", 220));
        catalog.put("issue_refund", new Capability.Tool("money", 240));
        catalog.put("export_report", new Capability.Tool("read", 300));

        // An agent holds more tools than its role needs: the audit names what to remove and which of them are risky.
        Capability.Agent agent = new Capability.Agent(new ArrayList<>(catalog.keySet()), List.of("read_ticket", "draft_reply"), Map.of("read_ticket", 12, "draft_reply", 9));
        System.out.println("audit: " + Capability.audit(agent, catalog));

        // Loading a long tool list: the most used tools load now, the rest wait behind a search tool.
        Map<String, Integer> tools = new LinkedHashMap<>();
        for (int i = 1; i <= 7; i++) tools.put(String.format("t%02d", i), 100);
        System.out.println("loading plan: " + Capability.planLoading(tools, Map.of("t01", 9, "t02", 5), 3, 350));

        // The gateway checks one request against the policy.
        Capability.Policy policy = new Capability.Policy(Map.of("k1", "support"), Map.of("support", Set.of("standard")),
            Map.of("support", Set.of("read_ticket", "draft_reply")), Map.of("support", 2), Map.of("standard", "claude-sonnet-5-5"));
        System.out.println("allowed: " + Capability.gateway(new Capability.Request("k1", "standard", "read_ticket", 0), policy));
        System.out.println("over the limit: " + Capability.gateway(new Capability.Request("k1", "standard", "read_ticket", 2), policy));
    }
}
