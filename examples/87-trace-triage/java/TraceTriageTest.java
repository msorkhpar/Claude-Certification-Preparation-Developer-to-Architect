import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TraceTriageTest {
    private static List<TraceTriage.Span> trace(String name) {
        List<TraceTriage.Span> spans = TraceTriage.TRACES.get(name);
        assertNotNull(spans);
        return spans;
    }

    private static Map<String, Integer> two(int a, int b, int c) {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", a);
        m.put("b", b);
        m.put("c", c);
        return m;
    }

    @Test
    void m1_errorsAndSlowTracesAreAlwaysKept() {
        assertEquals("error", TraceTriage.keepReason("t-refund", trace("t-refund"), 0));
        List<TraceTriage.Span> slow = List.of(new TraceTriage.Span("s1", "", "agent", "a", "ok", 6000, ""));
        assertEquals("slow", TraceTriage.keepReason("x", slow, 0));
    }

    @Test
    void healthyTracesAreKeptByIdAndNotByChance() {
        int sampled = 0;
        for (int i = 0; i < 100; i++) {
            String id = "trace-" + i;
            String first = TraceTriage.keepReason(id, trace("t-plain"), 10);
            assertEquals(first, TraceTriage.keepReason(id, trace("t-plain"), 10));
            if (first.equals("sampled")) sampled++;
        }
        assertTrue(sampled > 0 && sampled < 100);
        assertEquals("dropped", TraceTriage.keepReason("trace-1", trace("t-plain"), 0));
    }

    @Test
    void rootCauseIsTheDeepestFailingSpan() {
        TraceTriage.Cause c = TraceTriage.rootCause(trace("t-refund"));
        assertEquals("tool", c.layer());
        assertEquals("web_fetch", c.name());
        assertEquals(List.of("orchestrator", "order-researcher", "web_fetch"), c.path());
    }

    @Test
    void aStaleRetrievalIsBlamedWhenNothingFailed() {
        assertEquals("stale", TraceTriage.rootCause(trace("t-policy")).why());
        assertEquals("no-hits", TraceTriage.rootCause(trace("t-empty")).why());
        assertEquals("none", TraceTriage.rootCause(trace("t-plain")).layer());
    }

    @Test
    void driftFlagsBothDirectionsOverTolerance() {
        assertEquals(List.of("a up 40%", "b down 40%"), TraceTriage.drift(two(10, 10, 10), two(14, 6, 11), 25));
    }

    @Test
    void anAlertNeedsConsecutiveWindows() {
        List<Integer> s = List.of(1, 2, 9, 2, 8, 9, 10, 3);
        assertEquals(2, TraceTriage.alertAt(s, 5, 1));
        assertEquals(6, TraceTriage.alertAt(s, 5, 3));
        assertEquals(-1, TraceTriage.alertAt(List.of(9, 1, 9, 1, 9), 5, 2));
    }

    @Test
    void redactDropsContentUnlessAllowed() {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("trace", "t");
        e.put("prompt", "x");
        e.put("tool_input", "y");
        e.put("input_tokens", 5);
        assertEquals(Set.of("trace", "input_tokens"), TraceTriage.redact(e, Set.of()).keySet());
        assertEquals(Set.of("trace", "input_tokens", "tool_input"), TraceTriage.redact(e, Set.of("tool_input")).keySet());
    }
}
