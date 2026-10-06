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

    private static void checkEffort(boolean haiku, String effort) {
        // TODO 1 of 7 (finish this to pass e3): refuse an effort level the model cannot take.
        // Receives whether the model is Haiku and the effort given. Throws RejectedRequest("output_config.effort", ...) for Haiku (it has no effort) and for a level that is not in EFFORTS.
        // Example: checkEffort(true, "low") throws, checkEffort(false, "adaptive") throws, checkEffort(false, "high") returns
    }

    private static void checkMode(boolean haiku, String kind) {
        // TODO 2 of 7 (finish this to pass e1): refuse a thinking mode the model does not have.
        // Receives whether the model is Haiku and the thinking type. Throws RejectedRequest("thinking", ...) for "adaptive" on Haiku, and for "enabled" or "disabled" on any model that is not Haiku.
        // Example: checkMode(false, "disabled") throws, checkMode(true, "disabled") returns
    }

    private static void checkBudget(Number budget, int maxTokens) {
        // TODO 3 of 7 (finish this to pass e6): refuse a manual thinking budget that does not fit.
        // Receives the budget (null when missing) and maxTokens. Throws RejectedRequest("thinking.budget_tokens", ...) when the budget is missing, below 1024 or not below maxTokens.
        // Example: checkBudget(1023, 4096) throws, checkBudget(2048, 2048) throws, checkBudget(1024, 2048) returns
    }

    private static void checkBetweenTools(String family, String effort) {
        // TODO 4 of 7 (finish this to pass e2): refuse between_tools where it does not work.
        // Receives the model family and the effort given (null when absent). Throws RejectedRequest("thinking", ...) unless the family is "claude-sonnet-5-5", and when the
        // effective effort (the one given, else DEFAULT_EFFORT.get(family)) is "xhigh" or "max".
        // Example: checkBetweenTools("claude-sonnet-5-5", "max") throws, checkBetweenTools("claude-sonnet-5-5", null) returns
    }

    private static Map<String, Object> thinkingObject(String kind, Number budget) {
        // TODO 5 of 7 (finish this to pass m1 and e7): the `thinking` value of the request.
        // Receives the thinking type and the budget (null unless "enabled"). Returns a map with "type" = kind, plus "budget_tokens" = budget for "enabled". Effort never goes in here.
        // Example: thinkingObject("adaptive", null) -> {type=adaptive}, thinkingObject("enabled", 2048) -> {type=enabled, budget_tokens=2048}
        return new LinkedHashMap<>();
    }

    private static boolean samplingAllowed(boolean haiku, String name, Object value) {
        // TODO 6 of 7 (finish this to pass e4): whether the model accepts this sampling parameter.
        // Receives whether the model is Haiku, the parameter name (temperature, top_p or top_k) and its value. Returns true for Haiku, and for the others only a temperature of exactly 1.0.
        // Example: samplingAllowed(false, "temperature", 0.2) -> false, samplingAllowed(true, "top_k", 40) -> true
        return true;
    }

    private static Map<String, Object> fastParams(String family, boolean batch) {
        // TODO 7 of 7 (finish this to pass e5): the parameters fast mode adds, or a refusal.
        // Receives the model family and whether the request goes into a batch. Throws RejectedRequest("speed", ...) unless the family is "claude-opus-5-5", and when `batch` is true.
        // Otherwise returns a map with "speed" = "fast" and "betas" = List.of(FAST_BETA).
        // Example: fastParams("claude-opus-5-5", false) -> {speed=fast, betas=[fast-mode-2026-02-01]}
        return new LinkedHashMap<>();
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
