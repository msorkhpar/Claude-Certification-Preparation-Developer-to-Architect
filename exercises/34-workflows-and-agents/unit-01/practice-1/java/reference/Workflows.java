import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md. */
final class Workflows {
    private static final System.Logger LOG = System.getLogger(Workflows.class.getName());
    private Workflows() {}

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static String slice(String text, char open, char close) {
        int first = text.indexOf(open), last = text.lastIndexOf(close);
        return first != -1 && last > first ? text.substring(first, last + 1) : "";
    }

    private static Object parseJson(String text) {
        try {
            return Json.parse(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static List<String> cleanSteps(Object data) {
        List<String> steps = new ArrayList<>();
        if (data instanceof List<?> items) {
            for (Object item : items) if (item instanceof String s && !s.isBlank() && !steps.contains(s.strip())) steps.add(s.strip());
        }
        return steps;
    }

    private static Map<String, Object> runWorker(Function<String, String> ask, String subtask, String task) {
        LOG.log(System.Logger.Level.DEBUG, "runWorker input: {0}", subtask);
        try {
            String output = ask.apply("Subtask: " + subtask + "\nTask: " + task);
            if (output == null || output.isBlank()) throw new IllegalStateException("empty reply");
            return map("subtask", subtask, "status", "ok", "output", output);
        } catch (RuntimeException e) { // one worker failing must not stop the others
            return map("subtask", subtask, "status", "failed", "error", e.getMessage());
        }
    }

    private static String combinePrompt(String task, List<Map<String, Object>> results) {
        StringBuilder lines = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> r = results.get(i);
            lines.append("\n").append(i + 1).append(". ").append(r.get("subtask")).append(" -> ").append(r.get("status").equals("ok") ? r.get("output") : "FAILED");
        }
        return "Combine: write one answer to the task from the results.\nTask: " + task + lines;
    }

    static Map<String, Object> orchestrate(Function<String, String> ask, String task) {
        return orchestrate(ask, task, 5);
    }

    static Map<String, Object> orchestrate(Function<String, String> ask, String task, int maxSubtasks) {
        int calls = 1;
        String planReply = ask.apply("Plan: split the task into at most " + maxSubtasks + " independent subtasks. Reply with a JSON array of strings only.\nTask: " + task);
        List<String> steps = cleanSteps(parseJson(slice(planReply, '[', ']')));
        boolean fallback = steps.isEmpty();
        List<String> plan = fallback ? List.of(task) : steps.subList(0, Math.min(steps.size(), maxSubtasks));
        List<Map<String, Object>> results = new ArrayList<>();
        for (String subtask : plan) {
            calls++;
            results.add(runWorker(ask, subtask, task));
        }
        int okCount = (int) results.stream().filter(r -> r.get("status").equals("ok")).count();
        if (okCount == 0) return map("status", "failed", "plan", plan, "fallback", fallback, "results", results, "answer", null, "calls", calls);
        String answer = ask.apply(combinePrompt(task, results));
        return map("status", okCount == results.size() ? "done" : "partial", "plan", plan, "fallback", fallback, "results", results, "answer", answer, "calls", calls + 1);
    }

    private static final String UNREADABLE = "The judge reply could not be read.";

    private static boolean scoreOk(Object score) {
        return score instanceof Number n && n.doubleValue() >= 0 && n.doubleValue() <= 10;
    }

    /** The score and the feedback from the judge's reply; a score of 0 and a fixed note when the reply cannot be read. */
    @SuppressWarnings("unchecked")
    private static Object[] readJudgement(String text) {
        Object data = parseJson(slice(text, '{', '}'));
        Object score = data instanceof Map<?, ?> m ? m.get("score") : null;
        if (!scoreOk(score)) return new Object[] {0, UNREADABLE};
        Object feedback = ((Map<String, Object>) data).get("feedback");
        return new Object[] {score, feedback instanceof String s ? s : ""};
    }

    private static String writerPrompt(String task, String draft, String feedback, int round) {
        return round == 1 ? "Task: " + task : "Task: " + task + "\nPrevious draft: " + draft + "\nFeedback: " + feedback + "\nRevise the draft.";
    }

    private static boolean isBetter(Object score, Object bestScore) {
        return bestScore == null || ((Number) score).doubleValue() > ((Number) bestScore).doubleValue();
    }

    private static Map<String, Object> errorResult(String best, Object bestScore, int rounds, List<Object> history, RuntimeException e) {
        return map("status", "error", "draft", best, "score", bestScore, "rounds", rounds, "history", history, "error", e.getMessage());
    }

    static Map<String, Object> refine(Function<String, String> write, Function<String, String> judge, String task) {
        return refine(write, judge, task, 3, 8);
    }

    static Map<String, Object> refine(Function<String, String> write, Function<String, String> judge, String task, int maxRounds, int threshold) {
        List<Object> history = new ArrayList<>();
        String best = null, draft = null, feedback = "";
        Object bestScore = null;
        for (int round = 1; round <= maxRounds; round++) {
            String prompt = writerPrompt(task, draft, feedback, round);
            try {
                draft = write.apply(prompt);
            } catch (RuntimeException e) {
                return errorResult(best, bestScore, round - 1, history, e);
            }
            Object[] judgement = readJudgement(judge.apply("Judge: score the draft from 0 to 10 and reply with JSON {\"score\": n, \"feedback\": \"...\"}.\nTask: " + task + "\nDraft: " + draft));
            Object score = judgement[0];
            feedback = (String) judgement[1];
            history.add(map("round", round, "score", score, "feedback", feedback));
            if (isBetter(score, bestScore)) {
                best = draft;
                bestScore = score;
            }
            if (((Number) score).doubleValue() >= threshold) return map("status", "accepted", "draft", draft, "score", score, "rounds", round, "history", history);
        }
        return map("status", "max_rounds", "draft", best, "score", bestScore, "rounds", maxRounds, "history", history);
    }

    private static String normaliseLabel(String reply) {
        return reply == null ? "" : reply.strip().replaceAll("^[.,;:!\"'`]+|[.,;:!\"'`]+$", "").strip().toLowerCase(Locale.ROOT);
    }

    /** Classify the text with one model call, then run the handler of the label. routes maps label to a function of the text. */
    static Map<String, Object> route(Function<String, String> ask, String text, Map<String, Function<String, String>> routes, String defaultLabel) {
        String reply = ask.apply("Classify: " + text + "\nLabels: " + String.join(", ", routes.keySet()));
        String label = normaliseLabel(reply);
        boolean fallback = !routes.containsKey(label);
        if (fallback) label = defaultLabel;
        return map("label", label, "output", routes.get(label).apply(text), "fallback", fallback);
    }

    private static String pickWinner(Map<String, Integer> counts, int top) {
        return counts.entrySet().stream().filter(e -> e.getValue() == top).findFirst().get().getKey();
    }

    static Map<String, Object> vote(Function<String, String> ask, String prompt) {
        return vote(ask, prompt, 5);
    }

    static Map<String, Object> vote(Function<String, String> ask, String prompt, int n) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) counts.merge(ask.apply(prompt).strip().toLowerCase(Locale.ROOT), 1, Integer::sum);
        if (counts.isEmpty()) return map("answer", null, "votes", counts, "agreement", 0.0);
        int top = counts.values().stream().max(Integer::compare).get();
        String winner = pickWinner(counts, top);
        return map("answer", winner, "votes", counts, "agreement", (double) top / n);
    }
}
