import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract. */
final class Params {
    private static final System.Logger LOG = System.getLogger(Params.class.getName());
    private Params() {}

    private static final List<String> EFFORTS = List.of("low", "medium", "high", "xhigh", "max");
    private static final String FAST_BETA = "fast-mode-2026-02-01";
    private static final Map<String, String> DEFAULT_EFFORT = Map.of(
            "claude-fable-5-1", "high", "claude-opus-5-5", "medium", "claude-sonnet-5-5", "high");

    private static String family(String model) {
        if (model.startsWith("claude-haiku-4-5")) return "claude-haiku-4-5";
        if (DEFAULT_EFFORT.containsKey(model)) return model;
        throw new RejectedRequest("model", "unknown model " + model);
    }

    /** Refuse an effort level the model cannot take. */
    private static void checkEffort(boolean haiku, String effort) {
        if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");
        if (!EFFORTS.contains(effort)) throw new RejectedRequest("output_config.effort", effort + " is not an effort level");
    }

    /** Refuse a thinking mode the model does not have. */
    private static void checkMode(boolean haiku, String kind) {
        if (kind.equals("adaptive") && haiku) throw new RejectedRequest("thinking", "adaptive thinking is not available on this model");
        if (kind.equals("enabled") && !haiku) throw new RejectedRequest("thinking", "manual thinking budgets are not accepted on this model");
        if (kind.equals("disabled") && !haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");
    }

    /** Refuse a manual thinking budget that is missing, below 1024 or not below maxTokens. */
    private static void checkBudget(Number budget, int maxTokens) {
        if (budget == null || budget.longValue() < 1024 || budget.longValue() >= maxTokens) {
            throw new RejectedRequest("thinking.budget_tokens", "at least 1024 and below max_tokens");
        }
    }

    /** Refuse between_tools off Sonnet 5.5, or when the effective effort is xhigh or max. */
    private static void checkBetweenTools(String family, String effort) {
        if (!family.equals("claude-sonnet-5-5")) throw new RejectedRequest("thinking", "between_tools exists only on Claude Sonnet 5.5");
        String level = effort != null ? effort : DEFAULT_EFFORT.get(family);
        if (level.equals("xhigh") || level.equals("max")) {
            throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");
        }
    }

    /** The `thinking` value of the request: the type, and the budget for a manual one. */
    private static Map<String, Object> thinkingObject(String kind, Number budget) {
        Map<String, Object> thinking = new LinkedHashMap<>();
        thinking.put("type", kind);
        if (kind.equals("enabled")) thinking.put("budget_tokens", budget);
        return thinking;
    }

    /** Whether the model accepts this sampling parameter: Haiku all, the others only temperature 1.0. */
    private static boolean samplingAllowed(boolean haiku, String name, Object value) {
        return haiku || (name.equals("temperature") && ((Number) value).doubleValue() == 1.0);
    }

    /** The parameters fast mode adds, or a refusal: Opus 5.5 only, and never in a batch. */
    private static Map<String, Object> fastParams(String family, boolean batch) {
        if (!family.equals("claude-opus-5-5")) throw new RejectedRequest("speed", "fast mode is not available on this model");
        if (batch) throw new RejectedRequest("speed", "fast mode is not available in a batch");
        Map<String, Object> fast = new LinkedHashMap<>();
        fast.put("speed", "fast");
        fast.put("betas", List.of(FAST_BETA));
        return fast;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> buildParams(String model, int maxTokens, Map<String, Object> options) {
        LOG.log(System.Logger.Level.DEBUG, "buildParams input: {0} {1} {2}", model, maxTokens, options);
        String family = family(model);
        boolean haiku = family.equals("claude-haiku-4-5");
        if (maxTokens < 1) throw new RejectedRequest("max_tokens", "must be at least 1");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("model", model);
        params.put("max_tokens", maxTokens);

        String effort = (String) options.get("effort");
        if (effort != null) {
            checkEffort(haiku, effort);
            params.put("output_config", Map.of("effort", effort));
        }

        Map<String, Object> thinking = (Map<String, Object>) options.get("thinking");
        if (thinking != null) {
            String kind = (String) thinking.get("type");
            if (kind == null || !List.of("adaptive", "enabled", "disabled", "between_tools").contains(kind)) {
                throw new RejectedRequest("thinking", "unknown thinking type " + kind);
            }
            if (kind.equals("between_tools")) checkBetweenTools(family, effort);
            else checkMode(haiku, kind);
            Number budget = (Number) thinking.get("budget_tokens");
            if (kind.equals("enabled")) checkBudget(budget, maxTokens);
            params.put("thinking", thinkingObject(kind, budget));
        }

        for (String name : List.of("temperature", "top_p", "top_k")) {
            Object value = options.get(name);
            if (value == null) continue;
            if (!samplingAllowed(haiku, name, value)) throw new RejectedRequest(name, "this model rejects a non-default value");
            params.put(name, value);
        }

        if ("fast".equals(options.get("speed"))) params.putAll(fastParams(family, Boolean.TRUE.equals(options.get("batch"))));
        return params;
    }
}
