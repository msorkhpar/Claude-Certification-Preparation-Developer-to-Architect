import java.util.List;
import java.util.Map;

/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. Modules, prompts and models are JSON-like maps. */
final class PromptPlan {
    private PromptPlan() {}

    static final int MIN_CACHEABLE = 512; // tokens: a shorter prefix cannot be cached

    /** One token per four characters, rounded up. */
    static int tokens(String text) {
        return (text.length() + 3) / 4;
    }

    static Map<String, Object> assemble(List<Map<String, Object>> modules, Map<String, String> variables, int budget) {
        // TODO: static modules first, then the dynamic ones with their variables filled; drop dynamic modules to fit the budget; mark the breakpoint.
        return null;
    }

    static String chooseModel(Map<String, Object> workload, List<Map<String, Object>> models) {
        // TODO: the name of the cheapest model that meets the tier and the latency, or null.
        return null;
    }

    static Integer reusablePrefix(Map<String, Object> a, Map<String, Object> b) {
        // TODO: the tokens of the cached prefix that two assembled prompts share, or 0.
        return null;
    }
}
