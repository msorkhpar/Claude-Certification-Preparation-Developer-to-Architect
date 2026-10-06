import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main to try your code; Submit runs the tests. */
public class TryIt {
    public static void main(String[] args) {
        // Turn the logger up, so the LOG.log(DEBUG, ...) lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);

        // One trace of a request: the assistant calls a search, then the model answers slowly.
        List<Triage.Span> spans = List.of(new Triage.Span("s1", "", "agent", "assistant", "ok", 1900, ""),
            new Triage.Span("s2", "s1", "retrieval", "search", "ok", 100, ""), new Triage.Span("s3", "s1", "llm", "answer", "ok", 1700, ""));
        System.out.println("kept as: " + Triage.keepTrace("trace-1", spans, 0));
        System.out.println("kept as (a failed span): " + Triage.keepTrace("trace-1", List.of(new Triage.Span("s1", "", "agent", "assistant", "error", 90, "timeout")), 0));

        // Where the failure started, from the deepest failed span.
        List<Triage.Span> failed = List.of(new Triage.Span("s1", "", "agent", "assistant", "error", 900, ""),
            new Triage.Span("s2", "s1", "tool", "lookup_order", "error", 800, "HTTP 500"));
        System.out.println("root cause: " + Triage.rootCause(failed));

        // A log event without content, and the trail of one request across components.
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("trace", "t");
        event.put("tool_input", "secret");
        event.put("input_tokens", 5);
        System.out.println("redacted: " + Triage.redact(event, Set.of()));
        List<Triage.Event> events = List.of(new Triage.Event("r1", 30, "tool", "lookup done"), new Triage.Event("r2", 10, "api", "other"),
            new Triage.Event("r1", 10, "api", "received"));
        System.out.println("trail: " + Triage.requestTrail(events, "r1"));
    }
}
