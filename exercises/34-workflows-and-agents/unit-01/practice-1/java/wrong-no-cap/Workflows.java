import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md. */
final class Workflows {
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

    static Map<String, Object> orchestrate(Function<String, String> ask, String task) {
        return orchestrate(ask, task, 5);
    }

    static Map<String, Object> orchestrate(Function<String, String> ask, String task, int maxSubtasks) {
        int calls = 1;
        String planReply = ask.apply("Plan: split the task into at most " + maxSubtasks + " independent subtasks. Reply with a JSON array of strings only.\nTask: " + task);
        List<String> steps = new ArrayList<>();
        if (parseJson(slice(planReply, '[', ']')) instanceof List<?> data) {
            for (Object item : data) if (item instanceof String s && !s.isBlank() && !steps.contains(s.strip())) steps.add(s.strip());
        }
        boolean fallback = steps.isEmpty();
        List<String> plan = fallback ? List.of(task) : steps;
        List<Object> results = new ArrayList<>();
        int okCount = 0;
        for (String subtask : plan) {
            calls++;
            try {
                String output = ask.apply("Subtask: " + subtask + "\nTask: " + task);
                if (output == null || output.isBlank()) throw new IllegalStateException("empty reply");
                results.add(map("subtask", subtask, "status", "ok", "output", output));
                okCount++;
            } catch (RuntimeException e) { // one worker failing must not stop the others
                results.add(map("subtask", subtask, "status", "failed", "error", e.getMessage()));
            }
        }
        if (okCount == 0) return map("status", "failed", "plan", plan, "fallback", fallback, "results", results, "answer", null, "calls", calls);
        StringBuilder lines = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> r = (Map<String, Object>) results.get(i);
            lines.append("\n").append(i + 1).append(". ").append(r.get("subtask")).append(" -> ").append(r.get("status").equals("ok") ? r.get("output") : "FAILED");
        }
        String answer = ask.apply("Combine: write one answer to the task from the results.\nTask: " + task + lines);
        return map("status", okCount == results.size() ? "done" : "partial", "plan", plan, "fallback", fallback, "results", results, "answer", answer, "calls", calls + 1);
    }

    private static final String UNREADABLE = "The judge reply could not be read.";

    /** The score and the feedback from the judge's reply; a score of 0 and a fixed note when the reply cannot be read. */
    @SuppressWarnings("unchecked")
    private static Object[] readJudgement(String text) {
        Object data = parseJson(slice(text, '{', '}'));
        Object score = data instanceof Map<?, ?> m ? m.get("score") : null;
        if (!(score instanceof Number n) || n.doubleValue() < 0 || n.doubleValue() > 10) return new Object[] {0, UNREADABLE};
        Object feedback = ((Map<String, Object>) data).get("feedback");
        return new Object[] {score, feedback instanceof String s ? s : ""};
    }

    static Map<String, Object> refine(Function<String, String> write, Function<String, String> judge, String task) {
        return refine(write, judge, task, 3, 8);
    }

    static Map<String, Object> refine(Function<String, String> write, Function<String, String> judge, String task, int maxRounds, int threshold) {
        List<Object> history = new ArrayList<>();
        String best = null, draft = null, feedback = "";
        Object bestScore = null;
        for (int round = 1; round <= maxRounds; round++) {
            String prompt = round == 1 ? "Task: " + task : "Task: " + task + "\nPrevious draft: " + draft + "\nFeedback: " + feedback + "\nRevise the draft.";
            try {
                draft = write.apply(prompt);
            } catch (RuntimeException e) {
                return map("status", "error", "draft", best, "score", bestScore, "rounds", round - 1, "history", history, "error", e.getMessage());
            }
            Object[] judgement = readJudgement(judge.apply("Judge: score the draft from 0 to 10 and reply with JSON {\"score\": n, \"feedback\": \"...\"}.\nTask: " + task + "\nDraft: " + draft));
            Object score = judgement[0];
            feedback = (String) judgement[1];
            history.add(map("round", round, "score", score, "feedback", feedback));
            if (bestScore == null || ((Number) score).doubleValue() > ((Number) bestScore).doubleValue()) {
                best = draft;
                bestScore = score;
            }
            if (((Number) score).doubleValue() >= threshold) return map("status", "accepted", "draft", draft, "score", score, "rounds", round, "history", history);
        }
        return map("status", "max_rounds", "draft", best, "score", bestScore, "rounds", maxRounds, "history", history);
    }

    /** Classify the text with one model call, then run the handler of the label. routes maps label to a function of the text. */
    static Map<String, Object> route(Function<String, String> ask, String text, Map<String, Function<String, String>> routes, String defaultLabel) {
        String reply = ask.apply("Classify: " + text + "\nLabels: " + String.join(", ", routes.keySet()));
        String label = reply == null ? "" : reply.strip().replaceAll("^[.,;:!\"'`]+|[.,;:!\"'`]+$", "").strip().toLowerCase(Locale.ROOT);
        boolean fallback = !routes.containsKey(label);
        if (fallback) label = defaultLabel;
        return map("label", label, "output", routes.get(label).apply(text), "fallback", fallback);
    }

    static Map<String, Object> vote(Function<String, String> ask, String prompt) {
        return vote(ask, prompt, 5);
    }

    static Map<String, Object> vote(Function<String, String> ask, String prompt, int n) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) counts.merge(ask.apply(prompt).strip().toLowerCase(Locale.ROOT), 1, Integer::sum);
        if (counts.isEmpty()) return map("answer", null, "votes", counts, "agreement", 0.0);
        int top = counts.values().stream().max(Integer::compare).get();
        String winner = counts.entrySet().stream().filter(e -> e.getValue() == top).findFirst().get().getKey();
        return map("answer", winner, "votes", counts, "agreement", (double) top / n);
    }
}
