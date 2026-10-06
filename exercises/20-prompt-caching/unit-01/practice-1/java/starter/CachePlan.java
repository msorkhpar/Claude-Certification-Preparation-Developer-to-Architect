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
        // TODO 1 of 6 (finish this to pass e5): refuse a volatile tool definition.
        // Receives the blocks. Throws PlanError when a block in the "tools" section is volatile (tools come first, nothing volatile may
        // sit in the prefix); returns nothing otherwise. Example: a tools block with "volatile" true makes it throw
    }

    private static List<Map<String, Object>> ordered(List<Map<String, Object>> blocks) {
        // TODO 2 of 6 (finish this to pass m1, e1 and e6): the blocks in cache-friendly order, as a new list.
        // Receives the blocks. Returns a new list: sections in the order of SECTIONS (tools, system, messages), blocks of one section in
        // the order given, then every volatile block moved to the very end in its given order. The input list is left unchanged.
        // Example: ids of [m (messages), s (system, volatile), t (tools)] come out as t, m, s
        return new ArrayList<>();
    }

    private static boolean wantsBreakpoint(Map<String, Object> block, long total, int minTokens) {
        // TODO 3 of 6 (finish this to pass e2 and e5): does this block carry a breakpoint?
        // Receives the block, total (the stable tokens from the start up to and including this block) and minTokens. Returns true when
        // the block asks for a breakpoint, is not volatile and total is at least minTokens. Example: a breakpoint block at 500 of 1024 -> false
        return false;
    }

    private static Object marker(Map<String, Object> block) {
        // TODO 4 of 6 (finish this to pass m1): the cache lifetime a breakpoint carries.
        // Receives the block. Returns its "ttl" ("5m" or "1h"), "5m" when it has none. Example: a block with ttl "1h" -> "1h"
        return "5m";
    }

    private static void checkCount(List<Object> marked) {
        // TODO 5 of 6 (finish this to pass e3): at most MAX_BREAKPOINTS breakpoints.
        // Receives the lifetimes of the blocks that kept a breakpoint. Throws PlanError when there are more than MAX_BREAKPOINTS.
        // Example: five "5m" entries make it throw
    }

    private static void checkLifetimes(List<Object> marked) {
        // TODO 6 of 6 (finish this to pass e4): a 1h breakpoint must come before every 5m one.
        // Receives the lifetimes of the kept breakpoints in request order. Throws PlanError when a "1h" follows a "5m".
        // Example: ["5m", "1h"] throws, ["1h", "5m"] does not
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
