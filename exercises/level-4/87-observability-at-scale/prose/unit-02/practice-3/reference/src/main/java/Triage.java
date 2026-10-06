import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Trace triage: which traces to keep, the layer that failed, drift, alerts, redaction and one request's trail. See ../../statement.md. */
final class Triage {
    private static final System.Logger LOG = System.getLogger(Triage.class.getName());
    private Triage() {}

    /** One step of a trace: its id and its parent's id (empty for the root), the kind (agent, llm, tool or retrieval), the name, the status (ok or error), the milliseconds and a note. */
    record Span(String id, String parent, String kind, String name, String status, int ms, String note) {}

    /** The layer and name of the origin, why it is blamed and the path of names from the root to it. */
    record Cause(String layer, String name, String why, List<String> path) {}

    /** A log line of one component, tied to a request by its id. */
    record Event(String request, int ts, String component, String message) {}

    static final Set<String> CONTENT = Set.of("prompt", "response", "tool_input", "tool_output");

    static int bucket(String traceId) {
        int h = 7;
        for (char c : traceId.toCharArray()) h = (h * 31 + c) % 1000003;
        return h % 100;
    }

    static String keepTrace(String traceId, List<Span> spans, int rate) {
        return keepTrace(traceId, spans, rate, false, 5000);
    }

    static String keepTrace(String traceId, List<Span> spans, int rate, boolean feedback, int slowMs) {
        LOG.log(System.Logger.Level.DEBUG, "keepTrace input: {0}", spans);
        for (Span s : spans) if (s.status().equals("error")) return "error";
        if (spans.get(0).ms() > slowMs) return "slow";
        Map<String, Integer> tools = new HashMap<>();
        for (Span s : spans) if (s.kind().equals("tool")) tools.merge(s.name(), 1, Integer::sum);
        for (int n : tools.values()) if (n >= 3) return "retries";
        if (feedback) return "feedback";
        return bucket(traceId) < rate ? "sampled" : "dropped";
    }

    private static Span deepest(List<Span> failed) {
        return failed.stream().filter(s -> failed.stream().noneMatch(o -> o.parent().equals(s.id()))).findFirst().orElse(failed.get(0));
    }

    private static Span blamedRetrieval(List<Span> spans) {
        for (Span s : spans) if (s.kind().equals("retrieval") && (s.note().equals("stale") || s.note().equals("no-hits"))) return s;
        return null;
    }

    static Cause rootCause(List<Span> spans) {
        Map<String, Span> byId = new HashMap<>();
        for (Span s : spans) byId.put(s.id(), s);
        List<Span> failed = new ArrayList<>();
        for (Span s : spans) if (s.status().equals("error")) failed.add(s);
        if (!failed.isEmpty()) {
            Span origin = deepest(failed);
            List<String> path = new ArrayList<>();
            for (Span cursor = origin; cursor != null; cursor = byId.get(cursor.parent())) path.add(cursor.name());
            Collections.reverse(path);
            return new Cause(origin.kind(), origin.name(), "failed", path);
        }
        Span s = blamedRetrieval(spans);
        if (s != null) return new Cause("retrieval", s.name(), s.note(), List.of(spans.get(0).name(), s.name()));
        return new Cause("none", "", "no span failed", List.of());
    }

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

    static int alertAt(List<Integer> series, int threshold, int windows) {
        int run = 0;
        for (int i = 0; i < series.size(); i++) {
            run = series.get(i) > threshold ? run + 1 : 0;
            if (run >= windows) return i;
        }
        return -1;
    }

    static Map<String, Object> redact(Map<String, Object> event, Set<String> allowed) {
        Map<String, Object> out = new LinkedHashMap<>();
        event.forEach((k, v) -> {
            if (!CONTENT.contains(k) || allowed.contains(k)) out.put(k, v);
        });
        return out;
    }

    static List<String> requestTrail(List<Event> events, String request) {
        return events.stream().filter(e -> e.request().equals(request)).sorted(Comparator.comparingInt(Event::ts)).map(e -> e.component() + ": " + e.message()).toList();
    }
}
