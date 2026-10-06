import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** A cost model and a model router. See ../../statement.md for the contract. Models, usages and tasks are JSON-like maps. */
final class Router {
    private static final System.Logger LOG = System.getLogger(Router.class.getName());
    private Router() {}

    private static long n(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static double d(Map<String, Object> m, String key) {
        return ((Number) m.get(key)).doubleValue();
    }

    /** The cache-write tokens as {5-minute kind, 1-hour kind}; an unsplit count is all the 5-minute kind. */
    @SuppressWarnings("unchecked")
    private static long[] writes(Map<String, Object> usage) {
        Map<String, Object> split = (Map<String, Object>) usage.get("cache_creation");
        if (split == null) return new long[] {n(usage, "cache_creation_input_tokens"), 0};
        return new long[] {n(split, "ephemeral_5m_input_tokens"), n(split, "ephemeral_1h_input_tokens")};
    }

    /** The cost of the cache parts of a request: the writes and the reads. */
    private static double cacheCost(Map<String, Object> model, Map<String, Object> usage) {
        long[] written = writes(usage);
        double price = d(model, "input");
        return written[0] * price * 1.25 + written[1] * price * 2.0 + n(usage, "cache_read_input_tokens") * price * d(model, "cache_read_multiplier");
    }

    /** The cost after the batch discount: half of it when `batch` is true. */
    private static double applyBatch(double total, boolean batch) {
        return batch ? total * 0.5 : total;
    }

    /** Cost of one request in micro-dollars (a price of $N per million tokens is N micro-dollars per token). */
    static double requestCost(Map<String, Object> model, Map<String, Object> usage, boolean batch) {
        double total = n(usage, "input_tokens") * d(model, "input") + cacheCost(model, usage) + n(usage, "output_tokens") * d(model, "output");
        return Math.round(applyBatch(total, batch) * 1e6) / 1e6;
    }

    /** The tokens the model has to hold as input: uncached input, cache reads and every cache write. */
    private static long inputTokens(Map<String, Object> usage) {
        long[] written = writes(usage);
        return n(usage, "input_tokens") + n(usage, "cache_read_input_tokens") + written[0] + written[1];
    }

    /** Whether the model is allowed and big enough for the task. */
    private static boolean canTake(Map<String, Object> model, Map<String, Object> task, long tokens) {
        long minTier = task.containsKey("min_tier") ? n(task, "min_tier") : 1;
        return !Boolean.TRUE.equals(model.get("deprecated")) && n(model, "tier") >= minTier && tokens <= n(model, "context") && n(task, "max_tokens") <= n(model, "max_output");
    }

    /** The id of the cheapest of `models` for this usage; on a tie the lower tier, then the smaller id. */
    private static String cheapest(List<Map<String, Object>> models, Map<String, Object> usage, boolean batch) {
        return models.stream()
                .min(Comparator.<Map<String, Object>>comparingDouble(m -> requestCost(m, usage, batch))
                        .thenComparingLong(m -> n(m, "tier"))
                        .thenComparing(m -> (String) m.get("id")))
                .map(m -> (String) m.get("id"))
                .orElse("");
    }

    /** An empty list of models is an error: no model can take the task. */
    private static void requireChoice(List<Map<String, Object>> models) {
        if (models.isEmpty()) throw new NoModelError("no model can take this task");
    }

    /** The id of the cheapest model that can take the task; throws NoModelError when none can. */
    @SuppressWarnings("unchecked")
    static String route(List<Map<String, Object>> catalog, Map<String, Object> task) {
        LOG.log(System.Logger.Level.DEBUG, "route input: {0}", task);
        Map<String, Object> usage = (Map<String, Object>) task.get("usage");
        long tokens = inputTokens(usage);
        List<Map<String, Object>> eligible = catalog.stream().filter(m -> canTake(m, task, tokens)).toList();
        requireChoice(eligible);
        return cheapest(eligible, usage, Boolean.TRUE.equals(task.get("batch")));
    }
}
