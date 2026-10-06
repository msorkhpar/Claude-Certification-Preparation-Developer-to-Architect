import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. Requests, policies and rows are JSON-like maps. */
final class GatewayBudget {
    private static final System.Logger LOG = System.getLogger(GatewayBudget.class.getName());
    private GatewayBudget() {}

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> asList(Object value) {
        return (List<String>) value;
    }

    private static long n(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).longValue();
    }

    private static String chosenModel(Map<String, Object> request, Map<String, Object> policy) {
        // TODO 1 of 6 (unlocks m1 and e6): the model the policy picks for a request.
        // Receives the request (`task`, and perhaps `model`, a pin) and the policy (`allowed`, `routes`, `default`; use `asList` and `asMap`). Returns the pinned model
        // when it is in the `allowed` list; otherwise the model `routes` names for the task, or `default` for a task that is not listed.
        // Example: task "review" with no pin -> "opus"; task "classify" pinned to "opus" (not allowed) -> "haiku"
        return null;
    }

    static String route(Map<String, Object> request, Map<String, Object> policy, String status) {
        LOG.log(System.Logger.Level.DEBUG, "route input: {0}", request);
        // TODO 2 of 6 (unlocks e1): the model for the request once the budget status is known.
        // Receives the request, the policy and the budget status ("allow", "warn" or "block"). Returns null when the status is "block"; the model from
        // `chosenModel` when it is "allow"; for "warn" the cheaper model that the policy's `cheaper` map names for it (the model itself when none is named).
        // Example: status "warn", chosen model "opus", cheaper {opus: sonnet} -> "sonnet"
        return null;
    }

    static String admit(long spend, long budget, long estimate) {
        // TODO 3 of 6 (unlocks e2): admit one more request against the budget.
        // Receives what the team has spent, its budget and the estimated cost of the request, all in cents. Returns "block" when the budget is not positive or
        // `spend + estimate` is more than the budget; "warn" when it reaches 80 percent of the budget (80 percent itself warns) and is not over; else "allow".
        // Example: admit(700, 1000, 100) -> "warn", admit(900, 1000, 101) -> "block"
        return null;
    }

    private static long cost(Map<String, Object> row, Map<String, Object> prices) {
        // TODO 4 of 6 (unlocks e3): what one row of tokens costs.
        // Receives a row (`model`, `input`, `cache_read`, `output`: token counts) and `prices`, which maps a model to cents per million tokens for the same three
        // kinds (`n(map, key)` reads a number). Returns tokens times price, summed over the three kinds (not yet divided by a million). Throws
        // IllegalArgumentException("unknown model: " + name) for a model without a price.
        // Example: input 1_000_000 at price 200 and nothing else -> 200_000_000
        return 0;
    }

    private static long toCents(long value) {
        // TODO 5 of 6 (unlocks e4): a team's total in whole cents.
        // Receives a total in cents times a million. Returns it divided by one million and rounded to the nearest cent, halves up, with integer arithmetic.
        // Example: toCents(500_000) -> 1, toCents(499_999) -> 0
        return 0;
    }

    static List<Map<String, Object>> showback(List<Map<String, Object>> rows, Map<String, Object> prices) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) totals.merge((String) r.get("team"), cost(r, prices), Long::sum);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : totals.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("team", e.getKey());
            row.put("cents", toCents(e.getValue()));
            result.add(row);
        }
        result.sort(Comparator.comparingLong((Map<String, Object> x) -> -(Long) x.get("cents")).thenComparing(x -> (String) x.get("team")));
        return result;
    }

    static String delivery(long p95Seconds, long timeoutSeconds, long marginPercent) {
        // TODO 6 of 6 (unlocks e5): sync or accept-and-poll.
        // Receives the p95 latency and the caller's timeout in seconds and a safety margin in percent. Returns "sync" when the p95 plus the margin fits within the
        // timeout (an exact fit counts), otherwise "accept-and-poll". Compare p95 * (100 + margin) with timeout * 100.
        // Example: delivery(8, 10, 25) -> "sync", delivery(8, 10, 26) -> "accept-and-poll"
        return null;
    }
}
