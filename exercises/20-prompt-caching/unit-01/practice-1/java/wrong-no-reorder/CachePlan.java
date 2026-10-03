import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract. Blocks are JSON-like maps. */
final class CachePlan {
    private CachePlan() {}

    private static final Map<String, Integer> SECTIONS = Map.of("tools", 0, "system", 1, "messages", 2);
    private static final int MAX_BREAKPOINTS = 4;

    private static boolean flag(Map<String, Object> block, String key) {
        return Boolean.TRUE.equals(block.get(key));
    }

    static List<Map<String, Object>> planRequest(List<Map<String, Object>> blocks, int minTokens) {
        for (Map<String, Object> b : blocks) {
            if (flag(b, "volatile") && "tools".equals(b.get("section"))) {
                throw new PlanError("tool definition " + b.get("id") + " cannot be volatile: tools come first");
            }
        }
        List<Map<String, Object>> ordered = new ArrayList<>(blocks);

        List<Map<String, Object>> plan = new ArrayList<>();
        long total = 0;
        for (Map<String, Object> b : ordered) {
            boolean isVolatile = flag(b, "volatile");
            if (!isVolatile) total += ((Number) b.get("tokens")).longValue();
            boolean wanted = flag(b, "breakpoint") && !isVolatile && total >= minTokens;
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", b.get("id"));
            p.put("cache", wanted ? (b.containsKey("ttl") ? b.get("ttl") : "5m") : null);
            plan.add(p);
        }
        List<Map<String, Object>> marked = plan.stream().filter(p -> p.get("cache") != null).toList();
        if (marked.size() > MAX_BREAKPOINTS) throw new PlanError(marked.size() + " breakpoints: at most " + MAX_BREAKPOINTS);
        boolean seenFive = false;
        for (Map<String, Object> p : marked) {
            if ("5m".equals(p.get("cache"))) seenFive = true;
            else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");
        }
        return plan;
    }
}
