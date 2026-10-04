import java.util.List;
import java.util.Map;

/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. Requests, policies and rows are JSON-like maps. */
final class GatewayBudget {
    private GatewayBudget() {}

    static String route(Map<String, Object> request, Map<String, Object> policy, String status) {
        // TODO: the model for the request, or null when the team is blocked.
        return null;
    }

    static String admit(long spend, long budget, long estimate) {
        // TODO: "allow", "warn" or "block" for one more request against the budget.
        return null;
    }

    static List<Map<String, Object>> showback(List<Map<String, Object>> rows, Map<String, Object> prices) {
        // TODO: a list of {team, cents} for each team, most expensive first, ties by team name (cents is a Long).
        return null;
    }

    static String delivery(long p95Seconds, long timeoutSeconds, long marginPercent) {
        // TODO: "sync" or "accept-and-poll".
        return null;
    }
}
