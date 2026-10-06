import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TriageTest {
    private static Triage.Span sp(String id, String parent, String kind, String name, String status, int ms, String note) {
        return new Triage.Span(id, parent, kind, name, status, ms, note);
    }

    private static Triage.Span sp(String id) {
        return sp(id, "", "agent", "n", "ok", 100, "");
    }

    private static final List<Triage.Span> PLAIN = List.of(sp("s1", "", "agent", "assistant", "ok", 1900, ""), sp("s2", "s1", "retrieval", "search", "ok", 100, ""), sp("s3", "s1", "llm", "answer", "ok", 1700, ""));

    private static String keep(String id, List<Triage.Span> spans, int rate, boolean feedback, int slowMs) {
        String result = Triage.keepTrace(id, spans, rate, feedback, slowMs);
        assertNotNull(result, "keepTrace returned nothing");
        return result;
    }

    private static String keep(String id, List<Triage.Span> spans, int rate) {
        return keep(id, spans, rate, false, 5000);
    }

    private static Triage.Cause cause(List<Triage.Span> spans) {
        Triage.Cause result = Triage.rootCause(spans);
        assertNotNull(result, "rootCause returned nothing");
        return result;
    }

    private static List<String> drift(Map<String, Integer> baseline, Map<String, Integer> current, int tolerance) {
        List<String> result = Triage.drift(baseline, current, tolerance);
        assertNotNull(result, "drift returned nothing");
        return result;
    }

    private static Map<String, Integer> m(Object... kv) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) map.put((String) kv[i], (Integer) kv[i + 1]);
        return map;
    }

    private static List<Triage.Span> plus(Triage.Span first, List<Triage.Span> rest) {
        List<Triage.Span> all = new ArrayList<>();
        all.add(first);
        all.addAll(rest);
        return all;
    }

    @Test
    void m1_everyTraceGetsOneReasonAndAHealthyTraceIsKeptByItsId() {
        assertEquals("error", keep("a", List.of(sp("s1", "", "agent", "n", "error", 100, "")), 0));
        assertEquals("slow", keep("a", List.of(sp("s1", "", "agent", "n", "ok", 6000, "")), 0));
        assertEquals("feedback", keep("a", PLAIN, 0, true, 5000));
        assertEquals("dropped", keep("a", PLAIN, 0));
        assertEquals("sampled", keep("a", PLAIN, 100));
        int sampled = 0;
        for (int i = 0; i < 100; i++) {
            String first = keep("trace-" + i, PLAIN, 10);
            assertEquals(first, keep("trace-" + i, PLAIN, 10));
            if (first.equals("sampled")) sampled++;
        }
        assertTrue(sampled > 0 && sampled < 100);
    }

    @Test
    void e1_anErrorOutranksASlowRootWhichOutranksRetriesWhichOutrankAFlag() {
        List<Triage.Span> calls = List.of(sp("s2", "s1", "tool", "fetch", "ok", 100, ""), sp("s3", "s1", "tool", "fetch", "ok", 100, ""), sp("s4", "s1", "tool", "fetch", "ok", 100, ""));
        assertEquals("error", keep("a", List.of(sp("s1", "", "agent", "n", "error", 9000, "")), 0));
        assertEquals("slow", keep("a", plus(sp("s1", "", "agent", "n", "ok", 9000, ""), calls), 0, true, 5000));
        assertEquals("retries", keep("a", plus(sp("s1"), calls), 0, true, 5000));
        assertEquals("dropped", keep("a", plus(sp("s1"), calls.subList(0, 2)), 0));
        assertEquals("dropped", keep("a", List.of(sp("s1", "", "agent", "n", "ok", 5000, "")), 0));
        assertEquals("slow", keep("a", List.of(sp("s1", "", "agent", "n", "ok", 3000, "")), 0, false, 2000));
    }

    @Test
    void e2_theRootCauseIsTheDeepestFailingSpanAndItsPathStartsAtTheRoot() {
        List<Triage.Span> spans = List.of(sp("s1", "", "agent", "orchestrator", "error", 100, ""), sp("s2", "s1", "agent", "researcher", "error", 100, ""),
            sp("s3", "s2", "tool", "fetch", "error", 100, ""), sp("s4", "s1", "llm", "summarise", "ok", 100, ""));
        Triage.Cause c = cause(spans);
        assertEquals("tool", c.layer());
        assertEquals("fetch", c.name());
        assertEquals("failed", c.why());
        assertEquals(List.of("orchestrator", "researcher", "fetch"), c.path());
        List<Triage.Span> two = List.of(sp("s1", "", "agent", "top", "ok", 100, ""), sp("s2", "s1", "tool", "first", "error", 100, ""), sp("s3", "s1", "tool", "second", "error", 100, ""));
        assertEquals("first", cause(two).name());
    }

    @Test
    void e3_aStaleOrEmptyRetrievalIsBlamedOnlyWhenNoSpanFailed() {
        List<Triage.Span> stale = List.of(sp("s1", "", "agent", "assistant", "ok", 100, ""), sp("s2", "s1", "retrieval", "search", "ok", 100, "stale"), sp("s3", "s1", "llm", "answer", "ok", 100, ""));
        assertEquals("retrieval", cause(stale).layer());
        assertEquals("stale", cause(stale).why());
        assertEquals(List.of("assistant", "search"), cause(stale).path());
        List<Triage.Span> empty = List.of(sp("s1", "", "agent", "assistant", "ok", 100, ""), sp("s2", "s1", "retrieval", "search", "ok", 100, "no-hits"));
        assertEquals("no-hits", cause(empty).why());
        List<Triage.Span> both = List.of(stale.get(0), stale.get(1), sp("s3", "s1", "llm", "answer", "error", 100, ""));
        assertEquals("llm", cause(both).layer());
        assertEquals("failed", cause(both).why());
        assertEquals(new Triage.Cause("none", "", "no span failed", List.of()), cause(PLAIN));
    }

    @Test
    void e4_driftReportsAMoveInEitherDirectionOverTheToleranceAndNeverDividesByZero() {
        assertEquals(List.of("a up 40%", "b down 40%"), drift(m("a", 10, "b", 10, "c", 10), m("a", 14, "b", 6, "c", 11), 25));
        assertEquals(List.of(), drift(m("a", 10), m("a", 13), 30));
        assertEquals(List.of("a up 40%"), drift(m("a", 10), m("a", 14), 30));
        assertEquals(List.of("z up 100%"), drift(m("z", 0, "y", 0), m("z", 5, "y", 0), 25));
        assertEquals(List.of("a up 100%", "b up 100%"), drift(m("b", 5, "a", 5), m("b", 10, "a", 10), 0));
    }

    @Test
    void e5_anAlertNeedsConsecutiveWindowsOverTheThresholdAndADipStartsTheCountAgain() {
        List<Integer> s = List.of(1, 2, 9, 2, 8, 9, 10, 3);
        assertEquals(2, Triage.alertAt(s, 5, 1));
        assertEquals(6, Triage.alertAt(s, 5, 3));
        assertEquals(-1, Triage.alertAt(List.of(9, 1, 9, 1, 9), 5, 2));
        assertEquals(-1, Triage.alertAt(List.of(5, 5, 5), 5, 1));
        assertEquals(-1, Triage.alertAt(List.of(), 5, 1));
    }

    @Test
    void e6_aLogRecordDropsTheContentFieldsUnlessTheyAreAllowedByName() {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("trace", "t");
        e.put("prompt", "x");
        e.put("response", "y");
        e.put("tool_input", "z");
        e.put("tool_output", "w");
        e.put("input_tokens", 5);
        Map<String, Object> plain = Triage.redact(e, Set.of());
        assertNotNull(plain, "redact returned nothing");
        assertEquals(Map.of("trace", "t", "input_tokens", 5), plain);
        Map<String, Object> some = Triage.redact(e, Set.of("tool_input"));
        assertNotNull(some, "redact returned nothing");
        assertEquals(Map.of("trace", "t", "tool_input", "z", "input_tokens", 5), some);
        Map<String, Object> same = Triage.redact(e, Set.of("input_tokens"));
        assertNotNull(same, "redact returned nothing");
        assertEquals(Map.of("trace", "t", "input_tokens", 5), same);
    }

    @Test
    void e7_aRequestsTrailJoinsTheEventsOfEveryComponentInTimeOrder() {
        List<Triage.Event> events = List.of(new Triage.Event("r1", 30, "tool", "lookup done"), new Triage.Event("r2", 10, "api", "other"), new Triage.Event("r1", 10, "api", "received"),
            new Triage.Event("r1", 20, "agent", "plan"), new Triage.Event("r1", 20, "llm", "called"));
        List<String> trail = Triage.requestTrail(events, "r1");
        assertNotNull(trail, "requestTrail returned nothing");
        assertEquals(List.of("api: received", "agent: plan", "llm: called", "tool: lookup done"), trail);
        List<String> none = Triage.requestTrail(events, "r9");
        assertNotNull(none, "requestTrail returned nothing");
        assertEquals(List.of(), none);
    }
}
