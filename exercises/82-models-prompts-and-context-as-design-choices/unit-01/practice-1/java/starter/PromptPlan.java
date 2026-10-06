import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. Modules, prompts and models are JSON-like maps. */
final class PromptPlan {
    private static final System.Logger LOG = System.getLogger(PromptPlan.class.getName());
    private PromptPlan() {}

    static final int MIN_CACHEABLE = 512; // tokens: a shorter prefix cannot be cached

    static final Pattern VARIABLE = Pattern.compile("\\{(\\w+)\\}");

    /** One token per four characters, rounded up. */
    static int tokens(String text) {
        return (text.length() + 3) / 4;
    }

    private static Map<String, Object> block(String name, String text) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("name", name);
        b.put("text", text);
        return b;
    }

    private static int keptTokens(List<Map<String, Object>> kept) {
        int sum = 0;
        for (Map<String, Object> k : kept) sum += tokens((String) k.get("text"));
        return sum;
    }

    /**
     * TODO 1 of 7 (unlocks e2): fill the variables of a dynamic module's text.
     * Receives the text and a map of variables. Returns the text with every {@code {name}} replaced by that variable's value (extra variables are ignored).
     * Throws {@code IllegalArgumentException("missing variable: <name>")} when a variable has no value ({@code VARIABLE} matches a variable).
     * Example: fill("Q: {q}", Map.of("q", "hello")) -> "Q: hello"
     */
    static String fill(String text, Map<String, String> variables) {
        return text;
    }

    /**
     * TODO 2 of 7 (unlocks e1): refuse a variable in a static module.
     * Receives the list of static modules. Throws {@code IllegalArgumentException} with a message that says {@code static} (for example {@code static module <name> holds a variable, which
     * would break the cache}) when any module's text holds a variable; otherwise returns nothing.
     * Example: a static module with the text "policy for {customer}" -> IllegalArgumentException
     */
    static void checkStatic(List<Map<String, Object>> stat) {}

    /**
     * TODO 3 of 7 (unlocks e3): which dynamic module is dropped first?
     * Receives the list of kept dynamic modules, each a map with {@code name}, {@code text} and an Integer {@code priority}. Returns the index of the one with the lowest priority;
     * of two with the same priority, the later one (the higher index).
     * Example: priorities [1, 9, 1] -> 2
     */
    static int pickVictim(List<Map<String, Object>> kept) {
        return 0;
    }

    /**
     * TODO 4 of 7 (unlocks e3 and e4): drop dynamic modules until the prompt fits the budget.
     * Receives the tokens of the static prefix, the list {@code kept} of dynamic modules (change it in place) and the budget. While {@code prefix} plus the tokens of the
     * kept modules ({@code keptTokens(kept)}) exceeds the budget, remove the module {@code pickVictim} chooses and note its name. Returns the dropped names in the order they were dropped.
     * When nothing is left to drop and the budget is still exceeded, throws {@code IllegalArgumentException("over budget: ...")}: static modules are never dropped.
     * Example: kept priorities [1, 1] and a budget that fits one of them -> the later name is dropped
     */
    static List<String> fitBudget(int prefix, List<Map<String, Object>> kept, int budget) {
        return new ArrayList<>();
    }

    /**
     * TODO 5 of 7 (unlocks m1 and e5): where the cache breakpoint goes.
     * Receives the number of static blocks and the tokens of the static prefix. Returns the index of the last static block when there is at least one static
     * block and the prefix has at least {@code MIN_CACHEABLE} tokens; otherwise {@code null}.
     * Example: breakpointOf(2, 512) -> 1, breakpointOf(2, 511) -> null, breakpointOf(0, 900) -> null
     */
    static Integer breakpointOf(int staticCount, int prefix) {
        return null;
    }

    static Map<String, Object> assemble(List<Map<String, Object>> modules, Map<String, String> variables, int budget) {
        LOG.log(System.Logger.Level.DEBUG, "assemble input: {0}", modules);
        List<Map<String, Object>> stat = new ArrayList<>();
        List<Map<String, Object>> dynamic = new ArrayList<>();
        for (Map<String, Object> m : modules) (Boolean.TRUE.equals(m.get("static")) ? stat : dynamic).add(m);
        checkStatic(stat);
        List<Map<String, Object>> kept = new ArrayList<>();
        for (Map<String, Object> m : dynamic) {
            Map<String, Object> k = block((String) m.get("name"), fill((String) m.get("text"), variables));
            k.put("priority", m.get("priority") == null ? 0 : (Integer) m.get("priority"));
            kept.add(k);
        }
        List<Map<String, Object>> blocks = new ArrayList<>();
        int prefix = 0;
        for (Map<String, Object> m : stat) {
            blocks.add(block((String) m.get("name"), (String) m.get("text")));
            prefix += tokens((String) m.get("text"));
        }
        List<String> dropped = fitBudget(prefix, kept, budget);
        int used = prefix + keptTokens(kept);
        for (Map<String, Object> k : kept) blocks.add(block((String) k.get("name"), (String) k.get("text")));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("blocks", blocks);
        out.put("tokens", used);
        out.put("dropped", dropped);
        out.put("breakpoint", breakpointOf(stat.size(), prefix));
        return out;
    }

    /**
     * TODO 6 of 7 (unlocks e6): choose the model for a workload.
     * Receives the workload ({@code tier}, {@code max_latency_ms}) and a list of models ({@code name}, {@code tier}, {@code latency_ms}, {@code price_out}). Returns the name of the model with the lowest
     * {@code price_out} among those whose {@code tier} is at least the workload's and whose {@code latency_ms} is within the limit; equal prices go to the lower name; {@code null} when no
     * model fits. Example: two models at the same price named "mid" and "mid2", both fitting -> "mid"
     */
    static String chooseModel(Map<String, Object> workload, List<Map<String, Object>> models) {
        return null;
    }

    /**
     * TODO 7 of 7 (unlocks e7): how many tokens of cached prefix can be reused?
     * Receives two assembled prompts (each a map with {@code blocks} and {@code breakpoint}). When both have a breakpoint, the breakpoints are equal and every block up to and
     * including it is identical (name and text) in both, returns the tokens of those blocks; otherwise returns 0.
     * Example: two prompts that differ only in their dynamic blocks -> the tokens of the static prefix; one edited static block -> 0
     */
    static Integer reusablePrefix(Map<String, Object> a, Map<String, Object> b) {
        return 0;
    }
}
