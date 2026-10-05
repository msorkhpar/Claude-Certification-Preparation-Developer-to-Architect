import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract. Blocks are JSON-like maps. */
final class CachePlan {
    private static final System.Logger LOG = System.getLogger(CachePlan.class.getName());
    private CachePlan() {}

    private static final Map<String, Integer> SECTIONS = Map.of("tools", 0, "system", 1, "messages", 2);
    private static final int MAX_BREAKPOINTS = 4;

    private static boolean flag(Map<String, Object> block, String key) {
        return Boolean.TRUE.equals(block.get(key));
    }

    private static void checkTools(List<Map<String, Object>> blocks) {
        for (Map<String, Object> b : blocks) {
            if (flag(b, "volatile") && "tools".equals(b.get("section"))) {
                throw new PlanError("tool definition " + b.get("id") + " cannot be volatile: tools come first");
            }
        }
    }

    private static List<Map<String, Object>> ordered(List<Map<String, Object>> blocks) {
        List<Map<String, Object>> bySection = new ArrayList<>(blocks);
        bySection.sort(Comparator.comparingInt(b -> SECTIONS.get((String) b.get("section")))); // List.sort is stable
        List<Map<String, Object>> result = new ArrayList<>();
        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(result::add);
        bySection.stream().filter(b -> flag(b, "volatile")).forEach(result::add);
        return result;
    }

    private static boolean wantsBreakpoint(Map<String, Object> block, long total, int minTokens) {
        return flag(block, "breakpoint") && !flag(block, "volatile") && total >= minTokens;
    }

    private static Object marker(Map<String, Object> block) {
        return block.containsKey("ttl") ? block.get("ttl") : "5m";
    }

    private static void checkCount(List<Object> marked) {
        if (marked.size() > MAX_BREAKPOINTS) throw new PlanError(marked.size() + " breakpoints: at most " + MAX_BREAKPOINTS);
    }

    private static void checkLifetimes(List<Object> marked) {
        boolean seenFive = false;
        for (Object cache : marked) {
            if ("5m".equals(cache)) seenFive = true;
            else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");
        }
    }

    static List<Map<String, Object>> planRequest(List<Map<String, Object>> blocks, int minTokens) {
        LOG.log(System.Logger.Level.DEBUG, "planRequest input: {0}", blocks);
        checkTools(blocks);
        List<Map<String, Object>> plan = new ArrayList<>();
        List<Object> marked = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> b : ordered(blocks)) {
            if (!flag(b, "volatile")) total += ((Number) b.get("tokens")).longValue();
            Object cache = wantsBreakpoint(b, total, minTokens) ? marker(b) : null;
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", b.get("id"));
            p.put("cache", cache);
            plan.add(p);
            if (cache != null) marked.add(cache);
        }
        checkCount(marked);
        checkLifetimes(marked);
        return plan;
    }
}
