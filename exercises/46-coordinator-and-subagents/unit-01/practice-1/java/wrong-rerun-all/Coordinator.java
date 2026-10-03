import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/** A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md. Results are JSON-like maps. */
final class Coordinator {
    private Coordinator() {}

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static List<String> cleanGaps(List<String> gaps, int maxAgents) {
        List<String> out = new ArrayList<>();
        if (gaps != null) for (String gap : gaps) {
            String text = String.valueOf(gap).strip();
            if (!text.isEmpty() && !out.contains(text)) out.add(text);
        }
        return out.size() > maxAgents ? new ArrayList<>(out.subList(0, maxAgents)) : out;
    }

    private static List<Map<String, Object>> copy(List<Map<String, Object>> findings) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> f : findings) out.add(new LinkedHashMap<>(f));
        return out;
    }

    static Map<String, Object> coordinate(Function<String, Map<String, Object>> planner, Function<String, String> subagent,
            BiFunction<String, List<Map<String, Object>>, List<String>> reviewer, BiFunction<String, List<Map<String, Object>>, String> synthesizer, String question) {
        return coordinate(planner, subagent, reviewer, synthesizer, question, 4, 2);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> coordinate(Function<String, Map<String, Object>> planner, Function<String, String> subagent,
            BiFunction<String, List<Map<String, Object>>, List<String>> reviewer, BiFunction<String, List<Map<String, Object>>, String> synthesizer,
            String question, int maxAgents, int maxRounds) {
        Map<String, Object> plan = planner.apply(question);
        if (!Boolean.TRUE.equals(plan.get("delegate"))) { // a question the coordinator can answer itself is not worth a team
            return map("status", "direct", "answer", plan.get("answer"), "findings", new ArrayList<>(), "failed", new ArrayList<>(), "dropped", new ArrayList<>(),
                    "gaps", new ArrayList<>(), "rounds", 0, "subagent_calls", 0);
        }
        List<Map<String, Object>> tasks = new ArrayList<>();
        List<Map<String, Object>> dropped = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> subtasks = (List<Map<String, Object>>) plan.getOrDefault("subtasks", List.of());
        for (Map<String, Object> task : subtasks) {
            String scope = String.valueOf(task.getOrDefault("scope", ""));
            String brief = String.valueOf(task.getOrDefault("brief", ""));
            String key = scope.strip().toLowerCase(Locale.ROOT);
            if (brief.isBlank()) dropped.add(map("scope", scope, "reason", "empty brief"));
            else if (seen.contains(key)) dropped.add(map("scope", scope, "reason", "duplicate scope"));
            else if (tasks.size() >= maxAgents) dropped.add(map("scope", scope, "reason", "over limit"));
            else {
                seen.add(key);
                tasks.add(map("scope", scope, "brief", brief));
            }
        }
        List<Map<String, Object>> findings = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        int[] calls = {0};
        java.util.function.BiConsumer<String, String> run = (scope, brief) -> {
            calls[0]++;
            String report;
            try {
                report = subagent.apply(brief); // the brief is everything the subagent knows
            } catch (RuntimeException error) { // one subagent failing must not stop the others
                failed.add(map("scope", scope, "error", String.valueOf(error.getMessage())));
                return;
            }
            if (report == null || report.isBlank()) failed.add(map("scope", scope, "error", "empty report"));
            else findings.add(map("scope", scope, "text", report));
        };
        for (Map<String, Object> task : tasks) run.accept((String) task.get("scope"), (String) task.get("brief"));
        if (findings.isEmpty()) {
            return map("status", "failed", "answer", null, "findings", new ArrayList<>(), "failed", failed, "dropped", dropped, "gaps", new ArrayList<>(), "rounds", 0, "subagent_calls", calls[0]);
        }
        int rounds = 0;
        List<String> gaps = cleanGaps(reviewer.apply(question, copy(findings)), maxAgents);
        while (!gaps.isEmpty() && rounds < maxRounds) {
            rounds++;
            for (Map<String, Object> task : tasks) run.accept((String) task.get("scope"), (String) task.get("brief"));
            for (String gap : gaps) run.accept(gap, "Follow up: " + gap + "\nQuestion: " + question);
            gaps = cleanGaps(reviewer.apply(question, copy(findings)), maxAgents);
        }
        String answer = synthesizer.apply(question, copy(findings));
        String status = gaps.isEmpty() && failed.isEmpty() ? "complete" : "partial";
        return map("status", status, "answer", answer, "findings", findings, "failed", failed, "dropped", dropped, "gaps", gaps, "rounds", rounds, "subagent_calls", calls[0]);
    }
}
