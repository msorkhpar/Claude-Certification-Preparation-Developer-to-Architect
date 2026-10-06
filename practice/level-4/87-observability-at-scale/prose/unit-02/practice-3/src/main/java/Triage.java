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
        // TODO 1 of 7 (unlocks m1 and e1): why a trace is kept.
        // Receives the trace id, its spans (`status`, `ms`, `kind`, `name`; the first span is the root), the sampling rate (0 to 100) and the flags. Returns the first reason
        // that applies, in this order: "error" when any span has the status "error"; "slow" when the root's `ms` is over `slowMs` (equal is not slow); "retries" when
        // one tool (a span of kind "tool") was called three times or more by name; "feedback" when `feedback` is set; otherwise "sampled" when `bucket(traceId)` is
        // below `rate` and "dropped" when it is not.
        // Example: one tool span called "fetch" three times, nothing else wrong -> "retries"
        return null;
    }

    private static Span deepest(List<Span> failed) {
        // TODO 2 of 7 (unlocks e2): the span that is the origin of a failure.
        // Receives the spans that failed (each has `id` and `parent`). Returns the first one that is not the parent of another failed span, so the error that a
        // parent merely reported is passed over for the span below it.
        // Example: s1 (root) <- s2 <- s3, all failed -> s3; two failed children of one parent -> the first of them
        return failed.get(0);
    }

    private static Span blamedRetrieval(List<Span> spans) {
        // TODO 3 of 7 (unlocks e3): the retrieval span to blame when nothing failed.
        // Receives the spans. Returns the first span of kind "retrieval" whose `note` is "stale" or "no-hits", or null when there is none.
        // Example: a retrieval span with the note "stale" -> that span
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
        // TODO 4 of 7 (unlocks e4): the metrics that moved.
        // Receives the baseline and the current values (metric name to a whole number) and a tolerance in percent. Goes through the baseline names in alphabetical order.
        // The change is `Math.abs(now - base) * 100 / base` (integer division); a baseline of 0 counts as 100 when the value is not 0 and 0 when it is. Returns one string
        // "<name> up <pct>%" or "<name> down <pct>%" for each metric whose change is over the tolerance (equal is fine).
        // Example: baseline 10, now 14, tolerance 30 -> [a up 40%]
        return new ArrayList<>();
    }

    static int alertAt(List<Integer> series, int threshold, int windows) {
        // TODO 5 of 7 (unlocks e5): when an alert fires.
        // Receives the series of window values, the threshold and how many windows in a row must be over it (strictly over). Returns the index of the window at which the
        // count of consecutive windows over the threshold first reaches `windows`, or -1 when it never does. A window at or under the threshold starts the count again.
        // Example: [1, 2, 9, 2, 8, 9, 10, 3], threshold 5, windows 3 -> 6
        return -1;
    }

    static Map<String, Object> redact(Map<String, Object> event, Set<String> allowed) {
        // TODO 6 of 7 (unlocks e6): what a log record keeps.
        // Receives a record (a map) and the names that may stay. Returns a new map without the fields in CONTENT (`prompt`, `response`, `tool_input`, `tool_output`) unless the
        // field's name is in `allowed`; every other field stays, in the same order.
        // Example: {trace: t, prompt: x} -> {trace: t}
        return new LinkedHashMap<>(event);
    }

    static List<String> requestTrail(List<Event> events, String request) {
        // TODO 7 of 7 (unlocks e7): one request's story.
        // Receives the events of every component (`request`, `ts`, `component`, `message`) and a request id. Returns "<component>: <message>" for each event of that request, in
        // order of `ts` (events with the same `ts` stay in the order they came); an unknown request gives an empty list.
        // Example: events at ts 30 (tool), 10 (api), 20 (agent) -> [api: ..., agent: ..., tool: ...]
        return new ArrayList<>();
    }
}
