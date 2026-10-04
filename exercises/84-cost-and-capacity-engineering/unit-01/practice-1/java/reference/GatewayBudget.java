import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. Requests, policies and rows are JSON-like maps. */
final class GatewayBudget {
    private GatewayBudget() {}

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> asList(Object value) {
        return (List<String>) value;
    }

    static String route(Map<String, Object> request, Map<String, Object> policy, String status) {
        if (status.equals("block")) {
            return null;
        }
        Object wanted = request.get("model");
        String model = wanted != null && asList(policy.get("allowed")).contains(wanted) ? (String) wanted
                : (String) asMap(policy.get("routes")).getOrDefault(request.get("task"), policy.get("default"));
        if (status.equals("warn")) {
            model = (String) asMap(policy.get("cheaper")).getOrDefault(model, model);
        }
        return model;
    }

    static String admit(long spend, long budget, long estimate) {
        if (budget <= 0) {
            return "block";
        }
        long after = spend + estimate;
        if (after > budget) return "block";
        if (after * 100 >= budget * 80) return "warn";
        return "allow";
    }

    static List<Map<String, Object>> showback(List<Map<String, Object>> rows, Map<String, Object> prices) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            if (!prices.containsKey(r.get("model"))) throw new IllegalArgumentException("unknown model: " + r.get("model"));
            Map<String, Object> p = asMap(prices.get(r.get("model")));
            long value = ((Number) r.get("input")).longValue() * ((Number) p.get("input")).longValue() + ((Number) r.get("cache_read")).longValue() * ((Number) p.get("cache_read")).longValue()
                    + ((Number) r.get("output")).longValue() * ((Number) p.get("output")).longValue();
            totals.merge((String) r.get("team"), value, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : totals.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("team", e.getKey());
            row.put("cents", (e.getValue() + 500_000) / 1_000_000);
            result.add(row);
        }
        result.sort(Comparator.comparingLong((Map<String, Object> x) -> -(Long) x.get("cents")).thenComparing(x -> (String) x.get("team")));
        return result;
    }

    static String delivery(long p95Seconds, long timeoutSeconds, long marginPercent) {
        return p95Seconds * (100 + marginPercent) <= timeoutSeconds * 100 ? "sync" : "accept-and-poll";
    }
}
