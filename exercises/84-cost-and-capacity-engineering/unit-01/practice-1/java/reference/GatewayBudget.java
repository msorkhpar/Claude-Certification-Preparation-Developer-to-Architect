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
        Object wanted = request.get("model");
        return wanted != null && asList(policy.get("allowed")).contains(wanted) ? (String) wanted
                : (String) asMap(policy.get("routes")).getOrDefault(request.get("task"), policy.get("default"));
    }

    static String route(Map<String, Object> request, Map<String, Object> policy, String status) {
        LOG.log(System.Logger.Level.DEBUG, "route input: {0}", request);
        if (status.equals("block")) return null;
        String model = chosenModel(request, policy);
        return status.equals("warn") ? (String) asMap(policy.get("cheaper")).getOrDefault(model, model) : model;
    }

    static String admit(long spend, long budget, long estimate) {
        if (budget <= 0) return "block";
        long after = spend + estimate;
        if (after > budget) return "block";
        if (after * 100 >= budget * 80) return "warn";
        return "allow";
    }

    private static long cost(Map<String, Object> row, Map<String, Object> prices) {
        if (!prices.containsKey(row.get("model"))) throw new IllegalArgumentException("unknown model: " + row.get("model"));
        Map<String, Object> p = asMap(prices.get(row.get("model")));
        return n(row, "input") * n(p, "input") + n(row, "cache_read") * n(p, "cache_read") + n(row, "output") * n(p, "output");
    }

    private static long toCents(long value) {
        return (value + 500_000) / 1_000_000;
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
        return p95Seconds * (100 + marginPercent) <= timeoutSeconds * 100 ? "sync" : "accept-and-poll";
    }
}
