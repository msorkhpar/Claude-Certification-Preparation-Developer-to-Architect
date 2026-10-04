import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A prompt plan: modules in cache-friendly order under a token budget, a model for the workload, and what a cache can reuse. See ../../statement.md. Modules, prompts and models are JSON-like maps. */
final class PromptPlan {
    private PromptPlan() {}

    static final int MIN_CACHEABLE = 512; // tokens: a shorter prefix cannot be cached

    private static final Pattern VARIABLE = Pattern.compile("\\{(\\w+)\\}");

    /** One token per four characters, rounded up. */
    static int tokens(String text) {
        return (text.length() + 3) / 4;
    }

    private static String fill(String text, Map<String, String> variables) {
        Matcher m = VARIABLE.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            if (!variables.containsKey(name)) throw new IllegalArgumentException("missing variable: " + name);
            m.appendReplacement(out, Matcher.quoteReplacement(variables.get(name)));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static Map<String, Object> block(String name, String text) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("name", name);
        b.put("text", text);
        return b;
    }

    static Map<String, Object> assemble(List<Map<String, Object>> modules, Map<String, String> variables, int budget) {
        List<Map<String, Object>> stat = new ArrayList<>();
        List<Map<String, Object>> dynamic = new ArrayList<>();
        for (Map<String, Object> m : modules) (Boolean.TRUE.equals(m.get("static")) ? stat : dynamic).add(m);
        for (Map<String, Object> m : stat) {
            if (VARIABLE.matcher((String) m.get("text")).find()) throw new IllegalArgumentException("static module " + m.get("name") + " holds a variable, which would break the cache");
        }
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
        List<String> dropped = new ArrayList<>();
        while (prefix + keptTokens(kept) > budget) {
            if (kept.isEmpty()) throw new IllegalArgumentException("over budget: the static modules alone exceed it");
            int victim = 0;
            for (int i = 1; i < kept.size(); i++) {
                if ((Integer) kept.get(i).get("priority") <= (Integer) kept.get(victim).get("priority")) victim = i;
            }
            dropped.add((String) kept.remove(victim).get("name"));
        }
        int used = prefix + keptTokens(kept);
        for (Map<String, Object> k : kept) blocks.add(block((String) k.get("name"), (String) k.get("text")));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("blocks", blocks);
        out.put("tokens", used);
        out.put("dropped", dropped);
        out.put("breakpoint", !stat.isEmpty() && prefix >= MIN_CACHEABLE ? stat.size() - 1 : null);
        return out;
    }

    private static int keptTokens(List<Map<String, Object>> kept) {
        int sum = 0;
        for (Map<String, Object> k : kept) sum += tokens((String) k.get("text"));
        return sum;
    }

    static String chooseModel(Map<String, Object> workload, List<Map<String, Object>> models) {
        List<Map<String, Object>> fit = new ArrayList<>();
        for (Map<String, Object> m : models) {
            if ((Integer) m.get("tier") >= (Integer) workload.get("tier") && (Integer) m.get("latency_ms") <= (Integer) workload.get("max_latency_ms")) fit.add(m);
        }
        if (fit.isEmpty()) return null;
        fit.sort(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("price_out")).thenComparing(m -> (String) m.get("name")));
        return (String) fit.get(0).get("name");
    }

    @SuppressWarnings("unchecked")
    static Integer reusablePrefix(Map<String, Object> a, Map<String, Object> b) {
        Integer ia = (Integer) a.get("breakpoint");
        Integer ib = (Integer) b.get("breakpoint");
        if (ia == null || ib == null || !ia.equals(ib)) return 0;
        List<Map<String, Object>> blocksA = (List<Map<String, Object>>) a.get("blocks");
        List<Map<String, Object>> blocksB = (List<Map<String, Object>>) b.get("blocks");
        boolean same = true;
        int sum = 0;
        for (int i = 0; i <= ia; i++) {
            same = same && blocksA.get(i).equals(blocksB.get(i));
            sum += tokens((String) blocksA.get(i).get("text"));
        }
        return same ? sum : 0;
    }
}
