import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CachePlanTest {
    private static Map<String, Object> block(String id, String section, int tokens, Object... extra) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("id", id);
        b.put("section", section);
        b.put("tokens", tokens);
        for (int i = 0; i < extra.length; i += 2) b.put((String) extra[i], extra[i + 1]);
        return b;
    }

    private static List<String> ids(List<Map<String, Object>> plan) {
        return plan == null ? List.of() : plan.stream().map(p -> (String) p.get("id")).collect(Collectors.toList());
    }

    private static Map<String, Object> caches(List<Map<String, Object>> plan) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (plan != null) for (Map<String, Object> p : plan) out.put((String) p.get("id"), p.get("cache"));
        return out;
    }

    private static List<Object> values(List<Map<String, Object>> plan) {
        return new ArrayList<>(caches(plan).values());
    }

    private static Map<String, Object> expect(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Throwable raised(List<Map<String, Object>> blocks, int minTokens) {
        try {
            CachePlan.planRequest(blocks, minTokens);
        } catch (RuntimeException e) {
            return e;
        }
        return null;
    }

    @Test
    void m1_stableContentComesFirstAndTheVolatileDateGoesLast() {
        List<Map<String, Object>> blocks = List.of(
                block("date", "system", 20, "volatile", true),
                block("tools", "tools", 2000),
                block("rules", "system", 3000, "breakpoint", true),
                block("manual", "messages", 6000, "breakpoint", true),
                block("question", "messages", 40));
        List<Map<String, Object>> plan = CachePlan.planRequest(blocks, 1024);
        assertEquals(List.of("tools", "rules", "manual", "question", "date"), ids(plan));
        assertEquals(expect("tools", null, "rules", "5m", "manual", "5m", "question", null, "date", null), caches(plan));
    }

    @Test
    void e1_sectionsFollowThePrefixOrderAndKeepTheirOwnOrder() {
        List<Map<String, Object>> blocks = List.of(block("m1", "messages", 10), block("s1", "system", 10), block("t1", "tools", 10),
                block("s2", "system", 10), block("t2", "tools", 10), block("m2", "messages", 10));
        assertEquals(List.of("t1", "t2", "s1", "s2", "m1", "m2"), ids(CachePlan.planRequest(blocks, 1024)));
    }

    @Test
    void e2_aBreakpointNeedsTheStablePrefixToReachTheMinimum() {
        List<Map<String, Object>> blocks = List.of(block("tools", "tools", 400, "breakpoint", true), block("rules", "system", 500, "breakpoint", true),
                block("doc", "messages", 700, "breakpoint", true), block("ask", "messages", 30));
        assertEquals(expect("tools", null, "rules", null, "doc", "5m", "ask", null), caches(CachePlan.planRequest(blocks, 1024)));
        assertEquals(expect("tools", null, "rules", null, "doc", null, "ask", null), caches(CachePlan.planRequest(blocks, 4096)));
        // volatile tokens come after the prefix, so they never help it reach the minimum
        List<Map<String, Object>> padded = List.of(block("stamp", "system", 5000, "volatile", true), block("rules", "system", 500, "breakpoint", true));
        assertEquals(expect("rules", null, "stamp", null), caches(CachePlan.planRequest(padded, 1024)));
    }

    @Test
    void e3_atMostFourBreakpointsAreSent() {
        List<Map<String, Object>> five = new ArrayList<>();
        for (int i = 0; i < 5; i++) five.add(block("b" + i, "messages", 2000, "breakpoint", true));
        assertTrue(raised(five, 1024) instanceof PlanError);
        assertEquals(List.of("5m", "5m", "5m", "5m"), values(CachePlan.planRequest(five.subList(0, 4), 1024)));
        // two of the five never reach the minimum, so only three breakpoints are sent
        List<Map<String, Object>> small = new ArrayList<>(List.of(block("a", "tools", 10, "breakpoint", true), block("b", "system", 10, "breakpoint", true)));
        small.addAll(five.subList(0, 3));
        List<Object> expected = new ArrayList<>();
        expected.add(null);
        expected.add(null);
        expected.addAll(List.of("5m", "5m", "5m"));
        assertEquals(expected, values(CachePlan.planRequest(small, 1024)));
    }

    @Test
    void e4_aOneHourBreakpointMayNotFollowAFiveMinuteOne() {
        List<Map<String, Object>> longFirst = List.of(block("docs", "system", 3000, "breakpoint", true, "ttl", "1h"), block("turns", "messages", 3000, "breakpoint", true));
        assertEquals(expect("docs", "1h", "turns", "5m"), caches(CachePlan.planRequest(longFirst, 1024)));
        List<Map<String, Object>> wrongWay = List.of(block("turns", "system", 3000, "breakpoint", true), block("docs", "messages", 3000, "breakpoint", true, "ttl", "1h"));
        assertTrue(raised(wrongWay, 1024) instanceof PlanError);
    }

    @Test
    void e5_volatileBlocksNeverCarryABreakpointAndToolsCannotBeVolatile() {
        List<Map<String, Object>> blocks = List.of(block("rules", "system", 3000, "breakpoint", true), block("stamp", "messages", 3000, "volatile", true, "breakpoint", true));
        assertEquals(expect("rules", "5m", "stamp", null), caches(CachePlan.planRequest(blocks, 1024)));
        assertTrue(raised(List.of(block("t", "tools", 3000, "volatile", true)), 1024) instanceof PlanError);
    }

    @Test
    void e6_everyBlockComesOutOnceAndTheInputIsNotChanged() {
        List<Map<String, Object>> blocks = new ArrayList<>(List.of(block("date", "system", 20, "volatile", true), block("a", "messages", 2000, "breakpoint", true), block("t", "tools", 2000)));
        List<Map<String, Object>> before = new ArrayList<>();
        for (Map<String, Object> b : blocks) before.add(new LinkedHashMap<>(b));
        List<Map<String, Object>> plan = CachePlan.planRequest(blocks, 1024);
        List<String> got = new ArrayList<>(ids(plan));
        got.sort(null);
        assertEquals(List.of("a", "date", "t"), got);
        assertEquals(before, blocks);
    }
}
