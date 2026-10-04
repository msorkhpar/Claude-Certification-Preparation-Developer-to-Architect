import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reading a trace: where did it fail, in the integration or in the model, and what should happen next?
 *
 * <p>The Claude documentation on API errors and on stop reasons (read on 2026-10-03) lists the error types and says that a stop reason is part of
 * a successful response ("Response contains valid content") while an error is a 4xx or 5xx status. It also says that adding text right after
 * a tool result can make Claude end its turn with an empty reply. This file reads three hand-written traces, each a list of events, and names
 * the first failure, its origin and the next action. The traces are scripted and carry no live output.
 */
public final class ReadATrace {
    sealed interface Event permits Request, Response, ApiError, Parse {}

    /** A request; only the kinds of block in its last user message matter here. */
    record Request(List<String> lastUserBlocks) implements Event {}

    /** A successful reply; `content` holds the kinds of its blocks. */
    record Response(int status, String stopReason, List<String> content) implements Event {}

    record ApiError(int status, String errorType) implements Event {}

    record Parse(boolean ok, String text) implements Event {}

    record Failure(int index, String what, String origin, String next) {}

    static final Map<String, String> ORIGIN = Map.of(
        "invalid_request_error", "integration", "authentication_error", "account", "rate_limit_error", "service",
        "api_error", "service", "overloaded_error", "service", "timeout_error", "service");
    static final Map<String, String> NEXT = Map.of(
        "invalid_request_error", "fix the request, do not retry", "authentication_error", "fix the credential",
        "rate_limit_error", "wait, then retry", "api_error", "retry with back-off", "overloaded_error", "retry with back-off",
        "timeout_error", "stream the request");

    static final Map<String, List<Event>> TRACES = new LinkedHashMap<>();

    static {
        TRACES.put("A: a tool loop that ends in silence", List.of(
            new Request(List.of("text")),
            new Response(200, "tool_use", List.of("tool_use")),
            new Request(List.of("tool_result", "text")),
            new Response(200, "end_turn", List.of())));
        TRACES.put("B: a busy service and a retry", List.of(
            new Request(List.of("text")),
            new ApiError(529, "overloaded_error"),
            new Request(List.of("text")),
            new Response(200, "end_turn", List.of("text"))));
        TRACES.put("C: JSON in a code fence", List.of(
            new Request(List.of("text")),
            new Response(200, "end_turn", List.of("text")),
            new Parse(false, "```json\n{\"label\": \"spam\"}\n```")));
    }

    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private static boolean isJson(String text) {
        try {
            JSON.readValue(text, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** The first failing event with its origin and next action, or empty. */
    static Optional<Failure> firstFailure(List<Event> trace) {
        List<String> lastBlocks = List.of();
        for (int i = 0; i < trace.size(); i++) {
            Event e = trace.get(i);
            if (e instanceof Request r) {
                lastBlocks = r.lastUserBlocks();
            } else if (e instanceof ApiError err) {
                return Optional.of(new Failure(i, err.errorType(), ORIGIN.get(err.errorType()), NEXT.get(err.errorType())));
            } else if (e instanceof Response r && r.stopReason().equals("end_turn") && r.content().isEmpty()) {
                int at = lastBlocks.indexOf("tool_result");
                if (at >= 0 && lastBlocks.subList(at, lastBlocks.size()).contains("text")) {
                    return Optional.of(new Failure(i, "empty reply", "integration", "send the tool result alone, with no text after it"));
                }
                return Optional.of(new Failure(i, "empty reply", "model", "add a new user message that asks it to continue"));
            } else if (e instanceof Parse p && !p.ok()) {
                int start = p.text().indexOf('{');
                int end = p.text().lastIndexOf('}');
                String candidate = start >= 0 && end >= start ? p.text().substring(start, end + 1) : "";
                return Optional.of(isJson(candidate)
                    ? new Failure(i, "parse failure", "integration", "extract the JSON object before parsing")
                    : new Failure(i, "parse failure", "model", "validate the output and retry"));
            }
        }
        return Optional.empty();
    }

    public static void main(String[] args) {
        for (Map.Entry<String, List<Event>> entry : TRACES.entrySet()) {
            List<Event> trace = entry.getValue();
            Failure found = firstFailure(trace).orElseThrow();
            boolean recovered = trace.subList(found.index() + 1, trace.size()).stream()
                .anyMatch(e -> e instanceof Response r && r.stopReason().equals("end_turn") && !r.content().isEmpty());
            System.out.println(entry.getKey());
            System.out.println("  first failure: event " + found.index() + ", " + found.what() + "; origin: " + found.origin()
                + "; next: " + found.next() + "; recovered later: " + (recovered ? "True" : "False"));
        }
    }
}
