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

    /** Replace every {variable} in the text from the variables; a variable with no value is refused. */
    static String fill(String text, Map<String, String> variables) {
        return VARIABLE.matcher(text).replaceAll(m -> {
            if (!variables.containsKey(m.group(1))) throw new IllegalArgumentException("missing variable: " + m.group(1));
            return Matcher.quoteReplacement(variables.get(m.group(1)));
        });
    }

    /** A static module whose text holds a {variable} is refused: a value that changes in the prefix breaks the cache. */
    static void checkStatic(List<Map<String, Object>> stat) {
        for (Map<String, Object> m : stat) if (VARIABLE.matcher((String) m.get("text")).find()) throw new IllegalArgumentException("static module " + m.get("name") + " holds a variable, which would break the cache");
    }

    /** The index of the dynamic module to drop first: the lowest priority, and of a tie the later one. */
    static int pickVictim(List<Map<String, Object>> kept) {
        int victim = 0;
        for (int i = 1; i < kept.size(); i++) if ((Integer) kept.get(i).get("priority") <= (Integer) kept.get(victim).get("priority")) victim = i;
        return victim;
    }

    /** Drop dynamic modules until prefix + the kept tokens fit the budget; returns the dropped names in order. */
    static List<String> fitBudget(int prefix, List<Map<String, Object>> kept, int budget) {
        List<String> dropped = new ArrayList<>();
        while (prefix + keptTokens(kept) > budget) {
            if (kept.isEmpty()) throw new IllegalArgumentException("over budget: the static modules alone exceed it");
            dropped.add((String) kept.remove(pickVictim(kept)).get("name"));
        }
        return dropped;
    }

    /** The index of the last static block when there is one and the prefix has at least MIN_CACHEABLE tokens; otherwise null. */
    static Integer breakpointOf(int staticCount, int prefix) {
        return staticCount > 0 && prefix >= MIN_CACHEABLE ? staticCount - 1 : null;
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

    /** The name of the cheapest model that meets the tier and the latency; ties go to the lower name; null when none fits. */
    static String chooseModel(Map<String, Object> workload, List<Map<String, Object>> models) {
        return models.stream()
            .filter(m -> (Integer) m.get("tier") >= (Integer) workload.get("tier") && (Integer) m.get("latency_ms") <= (Integer) workload.get("max_latency_ms"))
            .sorted(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("price_out")).thenComparing(m -> (String) m.get("name")))
            .map(m -> (String) m.get("name")).findFirst().orElse(null);
    }

    /** The tokens of the cached prefix two assembled prompts share, or 0. */
    @SuppressWarnings("unchecked")
    static Integer reusablePrefix(Map<String, Object> a, Map<String, Object> b) {
        Integer ia = (Integer) a.get("breakpoint");
        Integer ib = (Integer) b.get("breakpoint");
        if (ia == null || ib == null || !ia.equals(ib)) return 0;
        List<Map<String, Object>> headA = ((List<Map<String, Object>>) a.get("blocks")).subList(0, ia + 1);
        List<Map<String, Object>> headB = ((List<Map<String, Object>>) b.get("blocks")).subList(0, ib + 1);
        return headA.equals(headB) ? headA.stream().mapToInt(x -> tokens((String) x.get("text"))).sum() : 0;
    }
}
