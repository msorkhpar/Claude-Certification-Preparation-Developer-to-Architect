import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** A cost model and a model router. See ../../statement.md for the contract. Models, usages and tasks are JSON-like maps. */
final class Router {
    private Router() {}

    private static long n(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? 0 : ((Number) v).longValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> created(Map<String, Object> usage) {
        return (Map<String, Object>) usage.get("cache_creation");
    }

    /** Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token). */
    static double requestCost(Map<String, Object> model, Map<String, Object> usage, boolean batch) {
        double price = ((Number) model.get("input")).doubleValue();
        Map<String, Object> split = created(usage);
        long five = split == null ? n(usage, "cache_creation_input_tokens") : n(split, "ephemeral_5m_input_tokens");
        long hour = split == null ? 0 : n(split, "ephemeral_1h_input_tokens");
        double total = n(usage, "input_tokens") * price
                + five * price * 1.25
                + hour * price * 2.0
                + n(usage, "cache_read_input_tokens") * price * ((Number) model.get("cache_read_multiplier")).doubleValue()
                + n(usage, "output_tokens") * ((Number) model.get("output")).doubleValue();
        if (batch) total *= 0.5;
        return Math.round(total * 1e6) / 1e6;
    }

    private static long inputTokens(Map<String, Object> usage) {
        Map<String, Object> split = created(usage);
        long written = split == null ? n(usage, "cache_creation_input_tokens")
                : n(split, "ephemeral_5m_input_tokens") + n(split, "ephemeral_1h_input_tokens");
        return n(usage, "input_tokens") + n(usage, "cache_read_input_tokens") + written;
    }

    /** The id of the cheapest model that can take the task; throws NoModelError when none can. */
    @SuppressWarnings("unchecked")
    static String route(List<Map<String, Object>> catalog, Map<String, Object> task) {
        Map<String, Object> usage = (Map<String, Object>) task.get("usage");
        long wanted = n(task, "max_tokens");
        long minTier = task.containsKey("min_tier") ? n(task, "min_tier") : 1;
        boolean batch = Boolean.TRUE.equals(task.get("batch"));
        return catalog.stream()
                .filter(m -> !Boolean.TRUE.equals(m.get("deprecated")))
                .filter(m -> n(m, "tier") >= minTier)
                .min(Comparator.<Map<String, Object>>comparingDouble(m -> requestCost(m, usage, batch))
                        .thenComparingLong(m -> n(m, "tier"))
                        .thenComparing(m -> (String) m.get("id")))
                .map(m -> (String) m.get("id"))
                .orElseThrow(() -> new NoModelError("no model can take this task"));
    }
}
