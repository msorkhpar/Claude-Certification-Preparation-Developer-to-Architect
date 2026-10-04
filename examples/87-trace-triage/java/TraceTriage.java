import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Observability decisions for a system of agents and tools: which traces to keep, how to find the layer that failed, when a change in a metric is drift, when to alert and what a log record may hold.
 *
 * The traces, metrics and events are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domains 3 and 4), Anthropic's article on its multi-agent research system
 * and the Claude Code monitoring documentation, read on 2026-10-04. Nothing here calls a model.
 */
public class TraceTriage {
    record Span(String id, String parent, String kind, String name, String status, int ms, String note) {}

    record Cause(String layer, String name, String why, List<String> path) {}

    static final Set<String> CONTENT = Set.of("prompt", "response", "tool_input", "tool_output");
    static final Map<String, List<Span>> TRACES = new LinkedHashMap<>();

    static {
        TRACES.put("t-refund", List.of(new Span("s1", "", "agent", "orchestrator", "error", 9200, ""), new Span("s2", "s1", "agent", "order-researcher", "error", 8700, ""),
                new Span("s3", "s2", "llm", "plan", "ok", 900, ""), new Span("s4", "s2", "tool", "web_fetch", "error", 5000, ""), new Span("s5", "s1", "llm", "summarise", "ok", 400, "")));
        TRACES.put("t-policy", List.of(new Span("s1", "", "agent", "assistant", "ok", 2100, ""), new Span("s2", "s1", "retrieval", "policy_search", "ok", 120, "stale"), new Span("s3", "s1", "llm", "answer", "ok", 1800, "")));
        TRACES.put("t-empty", List.of(new Span("s1", "", "agent", "assistant", "ok", 1500, ""), new Span("s2", "s1", "retrieval", "policy_search", "ok", 90, "no-hits"), new Span("s3", "s1", "llm", "answer", "ok", 1300, "")));
        TRACES.put("t-plain", List.of(new Span("s1", "", "agent", "assistant", "ok", 1900, ""), new Span("s2", "s1", "retrieval", "policy_search", "ok", 110, ""), new Span("s3", "s1", "llm", "answer", "ok", 1700, "")));
    }

    /** A number from 0 to 99 that depends on the trace id alone, the same in every agent and every language. */
    static int bucket(String traceId) {
        int h = 7;
        for (char c : traceId.toCharArray()) h = (h * 31 + c) % 1000003;
        return h % 100;
    }

    /** Tail-based sampling: a trace with an error, a slow root or a bad-answer flag is always kept, and the rest are kept by their id at `rate` percent. */
    static String keepReason(String traceId, List<Span> spans, int rate, boolean feedback, int slowMs) {
        for (Span s : spans) if (s.status().equals("error")) return "error";
        if (spans.get(0).ms() > slowMs) return "slow";
        if (feedback) return "feedback";
        return bucket(traceId) < rate ? "sampled" : "dropped";
    }

    static String keepReason(String traceId, List<Span> spans, int rate) {
        return keepReason(traceId, spans, rate, false, 5000);
    }

    /** The deepest failing span is the origin, not the span that reported the error; with no failure, a retrieval that returned stale or no chunks is blamed. */
    static Cause rootCause(List<Span> spans) {
        Map<String, Span> byId = new HashMap<>();
        for (Span s : spans) byId.put(s.id(), s);
        List<Span> failed = new ArrayList<>();
        for (Span s : spans) if (s.status().equals("error")) failed.add(s);
        if (!failed.isEmpty()) {
            Set<String> parents = new HashSet<>();
            for (Span s : failed) parents.add(s.parent());
            Span origin = null;
            for (Span s : failed) if (!parents.contains(s.id())) { origin = s; break; }
            List<String> path = new ArrayList<>();
            for (Span cursor = origin; cursor != null; cursor = byId.get(cursor.parent())) path.add(cursor.name());
            Collections.reverse(path);
            return new Cause(origin.kind(), origin.name(), "failed", path);
        }
        for (Span s : spans) {
            if (s.kind().equals("retrieval") && (s.note().equals("stale") || s.note().equals("no-hits"))) return new Cause("retrieval", s.name(), s.note(), List.of(spans.get(0).name(), s.name()));
        }
        return new Cause("none", "", "no span failed", List.of());
    }

    /** Metrics whose relative change since the baseline is over `tolerance` percent, in either direction. */
    static List<String> drift(Map<String, Integer> baseline, Map<String, Integer> current, int tolerance) {
        List<String> out = new ArrayList<>();
        for (String name : new TreeSet<>(baseline.keySet())) {
            int base = baseline.get(name);
            int now = current.get(name);
            int pct = base != 0 ? Math.abs(now - base) * 100 / base : (now != 0 ? 100 : 0);
            if (pct > tolerance) out.add(name + " " + (now > base ? "up" : "down") + " " + pct + "%");
        }
        return out;
    }

    /** The index of the window that completes `windows` consecutive values over the threshold, or -1. */
    static int alertAt(List<Integer> series, int threshold, int windows) {
        int run = 0;
        for (int i = 0; i < series.size(); i++) {
            run = series.get(i) > threshold ? run + 1 : 0;
            if (run >= windows) return i;
        }
        return -1;
    }

    /** A log record keeps ids, counts and timings and drops the content fields unless they are allowed by name. */
    static Map<String, Object> redact(Map<String, Object> event, Set<String> allowed) {
        Map<String, Object> out = new LinkedHashMap<>();
        event.forEach((k, v) -> {
            if (!CONTENT.contains(k) || allowed.contains(k)) out.put(k, v);
        });
        return out;
    }

    static Map<String, Integer> metrics(int hits, int refusals, int tokens, int toolErrors) {
        Map<String, Integer> m = new TreeMap<>();
        m.put("retrieval_hits", hits);
        m.put("refusals_per_1000", refusals);
        m.put("tokens_per_answer", tokens);
        m.put("tool_errors_per_1000", toolErrors);
        return m;
    }

    public static void main(String[] args) {
        int kept = 0;
        int byBucket = 0;
        for (int i = 0; i < 100; i++) {
            String id = "trace-" + i;
            if (keepReason(id, TRACES.get("t-plain"), 10).equals("sampled")) kept++;
            if (bucket(id) < 10) byBucket++;
        }
        System.out.println("100 healthy traces at a 10 percent rate: " + kept + " kept by id, the same " + kept + " in every agent: " + (kept == byBucket ? "True" : "False"));
        TRACES.forEach((name, spans) -> {
            Cause cause = rootCause(spans);
            List<String> parts = new ArrayList<>();
            for (String x : List.of(cause.layer(), cause.name(), cause.why())) if (!x.isEmpty()) parts.add(x);
            System.out.println(name + ": kept as " + keepReason(name, spans, 0) + "; cause: " + String.join(" ", parts));
        });
        System.out.println("path of t-refund: " + String.join(" > ", rootCause(TRACES.get("t-refund")).path()));
        System.out.println("a bad-answer flag keeps t-plain at rate 0: " + keepReason("t-plain", TRACES.get("t-plain"), 0, true, 5000));
        System.out.println("drift against last week, tolerance 25%: " + String.join(", ", drift(metrics(5, 4, 900, 12), metrics(3, 4, 1260, 13), 25)));
        List<Integer> series = List.of(1, 2, 9, 2, 8, 9, 10, 3);
        System.out.println("error rate per window " + series + ", threshold 5: one window over fires at " + alertAt(series, 5, 1) + ", three in a row fire at " + alertAt(series, 5, 3));
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("trace", "t-1");
        event.put("model", "claude-sonnet-5-5");
        event.put("input_tokens", 1200);
        event.put("output_tokens", 300);
        event.put("tool", "lookup_order");
        event.put("status", "ok");
        event.put("prompt", "(text)");
        event.put("tool_input", "(text)");
        TreeSet<String> plain = new TreeSet<>(redact(event, Set.of()).keySet());
        TreeSet<String> extra = new TreeSet<>(redact(event, Set.of("tool_input")).keySet());
        extra.removeAll(plain);
        System.out.println("log record keeps: " + String.join(", ", plain) + "; with tool_input allowed by name: " + String.join(", ", extra));
    }
}
