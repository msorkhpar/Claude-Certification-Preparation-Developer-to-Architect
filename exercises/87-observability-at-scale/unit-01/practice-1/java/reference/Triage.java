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
        for (Span s : spans) if (s.status().equals("error")) return "error";
        if (spans.get(0).ms() > slowMs) return "slow";
        Map<String, Integer> tools = new HashMap<>();
        for (Span s : spans) if (s.kind().equals("tool")) tools.merge(s.name(), 1, Integer::sum);
        for (int n : tools.values()) if (n >= 3) return "retries";
        if (feedback) return "feedback";
        return bucket(traceId) < rate ? "sampled" : "dropped";
    }

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
        List<Event> mine = new ArrayList<>();
        for (Event e : events) if (e.request().equals(request)) mine.add(e);
        mine.sort(Comparator.comparingInt(Event::ts));
        List<String> out = new ArrayList<>();
        for (Event e : mine) out.add(e.component() + ": " + e.message());
        return out;
    }
}
