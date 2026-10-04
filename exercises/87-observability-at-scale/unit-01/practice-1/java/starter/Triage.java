import java.util.List;
import java.util.Map;
import java.util.Set;

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
        // TODO: error, slow, retries, feedback, sampled or dropped, in that order of priority.
        return null;
    }

    static Cause rootCause(List<Span> spans) {
        // TODO: the layer, name, why and path of the deepest failing span, or of a stale or empty retrieval when nothing failed.
        return null;
    }

    static List<String> drift(Map<String, Integer> baseline, Map<String, Integer> current, int tolerance) {
        // TODO: "<name> up|down <percent>%" for each metric that moved by more than the tolerance, sorted by name.
        return null;
    }

    static int alertAt(List<Integer> series, int threshold, int windows) {
        // TODO: the index that completes `windows` consecutive values over the threshold, or -1.
        return -2;
    }

    static Map<String, Object> redact(Map<String, Object> event, Set<String> allowed) {
        // TODO: drop the content fields unless they are allowed by name.
        return null;
    }

    static List<String> requestTrail(List<Event> events, String request) {
        // TODO: "<component>: <message>" for each event of the request, in time order.
        return null;
    }
}
