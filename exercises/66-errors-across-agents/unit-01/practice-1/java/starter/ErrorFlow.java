import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** How a subagent's failure reaches the coordinator and the report: local recovery, structured error context, valid empty results, and coverage notes. See ../../statement.md. */
final class ErrorFlow {
    private static final System.Logger LOG = System.getLogger(ErrorFlow.class.getName());
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
        LOG.log(System.Logger.Level.DEBUG, "searchWithRecovery input: {0}", query);
        int attempts = 0;
        while (true) {
            attempts += 1;
            Reply reply = call.apply(query, attempts);
            if (reply.status().equals("ok")) {
                // TODO 2 of 6 (finish this to pass e1): the status of an ok reply. Return success when the reply holds
                //   items and empty when it holds none (a search that found nothing is a valid answer, not a failure).
                //   Example: ok with [] -> empty.
                return new Outcome("success", reply.items(), attempts, null, null, List.of(), List.of());
            }
            String kind = reply.type();
            // TODO 1 of 6 (finish this to pass m1): the local retry. When the failure type is one of TRANSIENT and the
            //   attempts so far are below max_attempts, try again; otherwise give up. Example: timeout then ok, limit 2 ->
            //   success with attempts 2.
            // TODO 3 of 6 (finish this to pass e2, e3): the failed outcome. Return status failed with the failure type,
            //   the query that was attempted, the attempts, the partial results the reply carried (none when absent) and
            //   the alternatives listed for that failure type (none when unknown). Example: permission error ->
            //   alternatives [request access, use a public source].
            return new Outcome("failed", List.of(), attempts, kind, query, List.of(), List.of());
        }
    }

    static List<Step> coordinatorPlan(Map<String, Outcome> results) {
        List<Step> plan = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : results.entrySet()) {
            Outcome o = e.getValue();
            String action;
            if (o.status().equals("success")) action = "use";
            else if (o.status().equals("empty")) action = "no_findings";
            // TODO 4 of 6 (finish this to pass e4): the rows for a failed topic. When the outcome has partial results,
            //   the action is use_partial; otherwise try_alternative when it has alternatives; otherwise flag_gap.
            //   Example: failed, no partial, alternatives [rewrite the query] -> try_alternative.
            else action = "flag_gap";
            plan.add(new Step(e.getKey(), action));
        }
        return plan;
    }

    static String coverageNote(Map<String, Outcome> results, List<String> topics) {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String name : List.of("Well-supported", "Partial", "No findings", "Gaps")) groups.put(name, new ArrayList<>());
        for (String topic : topics) {
            Outcome o = results.get(topic);
            // TODO 6 of 6 (finish this to pass e6): the topic with no result. When a requested topic has no outcome at
            //   all, list it under Gaps as "topic (not searched)". Example: topics [a, b], results only for a -> Gaps: b
            //   (not searched).
            if (o == null) continue;
            else if (o.status().equals("success")) groups.get("Well-supported").add(topic);
            else if (o.status().equals("empty")) groups.get("No findings").add(topic);
            // TODO 5 of 6 (finish this to pass e5): the groups of a failed topic in the coverage note. With partial
            //   results, list the topic under Partial as "topic (failure type)"; without, under Gaps as "topic (failure
            //   type: query attempted)". Example: partial, timeout -> Partial: topic (timeout).
            else groups.get("Gaps").add(topic);
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<String>> g : groups.entrySet()) if (!g.getValue().isEmpty()) lines.add(g.getKey() + ": " + String.join(", ", g.getValue()));
        return String.join("\n", lines);
    }
}
