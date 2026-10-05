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

    /**
     * TODO 1 of 7 (unlocks e3): why a subtask is dropped, or null to keep it.
     * Receives the brief, the lower-cased scope key, the keys already kept, how many are kept and the limit. Returns "empty brief" for a
     * blank brief, else "duplicate scope" for a key already seen, else "over limit" when keptCount has reached maxAgents, else null.
     * Example: dropReason("  ", "a", Set.of(), 0, 3) returns "empty brief".
     */
    private static String dropReason(String brief, String key, Set<String> seen, int keptCount, int maxAgents) {
        return null;
    }

    /**
     * TODO 2 of 7 (unlocks e4): what is wrong with a subagent's report, or null.
     * Receives what the subagent returned (maybe null). Returns "empty report" when it is null or blank, else null.
     * Example: reportProblem("   ") returns "empty report".
     */
    private static String reportProblem(String report) {
        return null;
    }

    /**
     * TODO 3 of 7 (unlocks e5): the brief for a follow-up on one gap.
     * Returns "Follow up: " + gap, a newline, then "Question: " + question. Example: followUp("2023", "q") returns "Follow up: 2023\nQuestion: q".
     */
    private static String followUp(String gap, String question) {
        return gap;
    }

    /**
     * TODO 4 of 7 (unlocks e5 and e6): is another refinement round allowed?
     * True while there are gaps and fewer than maxRounds rounds have run. Example: mayRefine(List.of("x"), 2, 2) returns false.
     */
    private static boolean mayRefine(List<String> gaps, int rounds, int maxRounds) {
        return false;
    }

    /**
     * TODO 5 of 7 (unlocks m1 and e6): the status of a run that has an answer.
     * "complete" when no gaps remain and nothing failed, else "partial". Example: finalStatus(List.of("x"), List.of()) returns "partial".
     */
    private static String finalStatus(List<String> gaps, List<Map<String, Object>> failed) {
        return "complete";
    }

    /**
     * TODO 6 of 7 (unlocks e1): does the plan ask for subagents?
     * Receives the planner's map. True when its "delegate" value is true, else false. Example: wantsTeam(Map.of("delegate", false)) returns false.
     */
    private static boolean wantsTeam(Map<String, Object> plan) {
        return true;
    }

    /**
     * TODO 7 of 7 (unlocks e5 and e6): the reviewer's gaps, tidied.
     * Receives the reviewer's list (maybe null) and the limit. Returns the gaps stripped, without empty or repeated ones, in order, at most
     * maxAgents of them. Example: cleanGaps(List.of(" a ", "", "a", "b"), 1) returns ["a"].
     */
    private static List<String> cleanGaps(List<String> gaps, int maxAgents) {
        return gaps == null ? new ArrayList<>() : new ArrayList<>(gaps);
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
