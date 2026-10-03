import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract. */
final class Params {
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

    @SuppressWarnings("unchecked")
    static Map<String, Object> buildParams(String model, int maxTokens, Map<String, Object> options) {
        String family = family(model);
        boolean haiku = family.equals("claude-haiku-4-5");
        if (maxTokens < 1) throw new RejectedRequest("max_tokens", "must be at least 1");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("model", model);
        params.put("max_tokens", maxTokens);

        String effort = (String) options.get("effort");
        if (effort != null) {
            if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");
            if (!EFFORTS.contains(effort)) throw new RejectedRequest("output_config.effort", effort + " is not an effort level");
            params.put("output_config", Map.of("effort", effort));
        }

        Map<String, Object> thinking = (Map<String, Object>) options.get("thinking");
        if (thinking != null) {
            String kind = (String) thinking.get("type");
            if ("adaptive".equals(kind)) {
                if (haiku) throw new RejectedRequest("thinking", "adaptive thinking is not available on this model");
                params.put("thinking", Map.of("type", "adaptive"));
            } else if ("enabled".equals(kind)) {
                Number budget = (Number) thinking.get("budget_tokens");
                if (!haiku) throw new RejectedRequest("thinking", "manual thinking budgets are not accepted on this model");
                if (budget == null || budget.longValue() < 1024 || budget.longValue() >= maxTokens) {
                    throw new RejectedRequest("thinking.budget_tokens", "at least 1024 and below max_tokens");
                }
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("type", "enabled");
                t.put("budget_tokens", budget);
                params.put("thinking", t);
            } else if ("disabled".equals(kind)) {
                if (!haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");
                params.put("thinking", Map.of("type", "disabled"));
            } else if ("between_tools".equals(kind)) {
                if (!family.equals("claude-sonnet-5-5")) throw new RejectedRequest("thinking", "between_tools exists only on Claude Sonnet 5.5");
                String level = effort != null ? effort : DEFAULT_EFFORT.get(family);
                if (level.equals("xhigh") || level.equals("max")) {
                    throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");
                }
                params.put("thinking", Map.of("type", "between_tools"));
            } else {
                throw new RejectedRequest("thinking", "unknown thinking type " + kind);
            }
        }

        for (String name : List.of("temperature", "top_p", "top_k")) {
            Object value = options.get(name);
            if (value == null) continue;
            boolean defaultTemperature = name.equals("temperature") && ((Number) value).doubleValue() == 1.0;
            params.put(name, value);
        }

        if ("fast".equals(options.get("speed"))) {
            if (!family.equals("claude-opus-5-5")) throw new RejectedRequest("speed", "fast mode is not available on this model");
            if (Boolean.TRUE.equals(options.get("batch"))) throw new RejectedRequest("speed", "fast mode is not available in a batch");
            params.put("speed", "fast");
            params.put("betas", List.of(FAST_BETA));
        }
        return params;
    }
}
