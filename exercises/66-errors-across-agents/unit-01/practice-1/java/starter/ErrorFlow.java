import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** How a subagent's failure reaches the coordinator and the report: local recovery, structured error context, valid empty results, and coverage notes. See ../../statement.md. */
final class ErrorFlow {
    private ErrorFlow() {}

    static final Map<String, List<String>> ALTERNATIVES = Map.of(
        "timeout", List.of("retry later", "try a narrower query"),
        "unavailable", List.of("use a cached source", "try another provider"),
        "permission", List.of("request access", "use a public source"),
        "invalid_query", List.of("rewrite the query"));
    static final List<String> TRANSIENT = List.of("timeout", "unavailable");

    /** What a search tool returns: status ok with items, or status error with a type and the partial items found before it failed. */
    record Reply(String status, String type, List<String> items, List<String> partial) {}

    record Outcome(String status, List<String> items, int attempts, String failureType, String attempted, List<String> partialResults, List<String> alternatives) {}

    record Step(String topic, String action) {}

    static Outcome searchWithRecovery(String query, BiFunction<String, Integer, Reply> call) {
        return searchWithRecovery(query, call, 2);
    }

    static Outcome searchWithRecovery(String query, BiFunction<String, Integer, Reply> call, int maxAttempts) {
        // TODO: call.apply(query, attempt) until it succeeds, fails for good or the attempts run out; return the outcome.
        return null;
    }

    static List<Step> coordinatorPlan(Map<String, Outcome> results) {
        // TODO: a step for every topic of results, in order; the run is never stopped.
        return null;
    }

    static String coverageNote(Map<String, Outcome> results, List<String> topics) {
        // TODO: the coverage annotation for the report: which topics are well supported, partial, without findings or gaps.
        return null;
    }
}
