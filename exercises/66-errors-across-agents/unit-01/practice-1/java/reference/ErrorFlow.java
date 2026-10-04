import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        int attempts = 0;
        while (true) {
            attempts += 1;
            Reply reply = call.apply(query, attempts);
            if (reply.status().equals("ok")) {
                return new Outcome(reply.items().isEmpty() ? "empty" : "success", reply.items(), attempts, null, null, List.of(), List.of());
            }
            String kind = reply.type();
            if (TRANSIENT.contains(kind) && attempts < maxAttempts) continue;
            return new Outcome("failed", List.of(), attempts, kind, query, reply.partial() == null ? List.of() : reply.partial(), ALTERNATIVES.getOrDefault(kind, List.of()));
        }
    }

    static List<Step> coordinatorPlan(Map<String, Outcome> results) {
        List<Step> plan = new ArrayList<>();
        for (Map.Entry<String, Outcome> e : results.entrySet()) {
            Outcome o = e.getValue();
            String action;
            if (o.status().equals("success")) action = "use";
            else if (o.status().equals("empty")) action = "no_findings";
            else if (!o.partialResults().isEmpty()) action = "use_partial";
            else if (!o.alternatives().isEmpty()) action = "try_alternative";
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
            if (o == null) groups.get("Gaps").add(topic + " (not searched)");
            else if (o.status().equals("success")) groups.get("Well-supported").add(topic);
            else if (o.status().equals("empty")) groups.get("No findings").add(topic);
            else if (!o.partialResults().isEmpty()) groups.get("Partial").add(topic + " (" + o.failureType() + ")");
            else groups.get("Gaps").add(topic + " (" + o.failureType() + ": " + o.attempted() + ")");
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<String>> g : groups.entrySet()) if (!g.getValue().isEmpty()) lines.add(g.getKey() + ": " + String.join(", ", g.getValue()));
        return String.join("\n", lines);
    }
}
