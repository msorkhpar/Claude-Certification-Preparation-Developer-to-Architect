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
    private static final System.Logger LOG = System.getLogger(Coordinator.class.getName());
    private Coordinator() {}

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static String dropReason(String brief, String key, Set<String> seen, int keptCount, int maxAgents) {
        if (brief.isBlank()) return "empty brief";
        if (seen.contains(key)) return "duplicate scope";
        if (keptCount >= maxAgents) return "over limit";
        return null;
    }

    private static String reportProblem(String report) {
        return report == null || report.isBlank() ? "empty report" : null;
    }

    private static String followUp(String gap, String question) {
        return "Follow up: " + gap + "\nQuestion: " + question;
    }

    private static boolean mayRefine(List<String> gaps, int rounds, int maxRounds) {
        return !gaps.isEmpty() && rounds < maxRounds;
    }

    private static String finalStatus(List<String> gaps, List<Map<String, Object>> failed) {
        return gaps.isEmpty() && failed.isEmpty() ? "complete" : "partial";
    }

    private static boolean wantsTeam(Map<String, Object> plan) {
        return Boolean.TRUE.equals(plan.get("delegate"));
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
        LOG.log(System.Logger.Level.DEBUG, "coordinate input: {0}", question);
        Map<String, Object> plan = planner.apply(question);
        if (!wantsTeam(plan)) { // a question the coordinator can answer itself is not worth a team
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
            String reason = dropReason(brief, key, seen, tasks.size(), maxAgents);
            if (reason != null) dropped.add(map("scope", scope, "reason", reason));
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
            String problem = reportProblem(report);
            if (problem != null) failed.add(map("scope", scope, "error", problem));
            else findings.add(map("scope", scope, "text", report));
        };
        for (Map<String, Object> task : tasks) run.accept((String) task.get("scope"), (String) task.get("brief"));
        if (findings.isEmpty()) {
            return map("status", "failed", "answer", null, "findings", new ArrayList<>(), "failed", failed, "dropped", dropped, "gaps", new ArrayList<>(), "rounds", 0, "subagent_calls", calls[0]);
        }
        int rounds = 0;
        List<String> gaps = cleanGaps(reviewer.apply(question, copy(findings)), maxAgents);
        while (mayRefine(gaps, rounds, maxRounds)) {
            rounds++;
            for (String gap : gaps) run.accept(gap, followUp(gap, question));
            gaps = cleanGaps(reviewer.apply(question, copy(findings)), maxAgents);
        }
        String answer = synthesizer.apply(question, copy(findings));
        String status = finalStatus(gaps, failed);
        return map("status", status, "answer", answer, "findings", findings, "failed", failed, "dropped", dropped, "gaps", gaps, "rounds", rounds, "subagent_calls", calls[0]);
    }
}
