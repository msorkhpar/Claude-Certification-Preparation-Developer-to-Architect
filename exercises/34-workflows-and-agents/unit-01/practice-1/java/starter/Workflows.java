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

    /**
     * TODO 1 of 9 (unlocks e1): the usable subtasks from the parsed plan.
     * Receives whatever Json.parse gave (a list, or anything else, or null) and returns the string items of a list, stripped, with
     * empty ones and repeats dropped (the first of a repeat stays), in order; anything that is not a list gives an empty list.
     * Example: cleanSteps(List.of("a", " a ", "", 3, "b")) -> ["a", "b"]
     */
    private static List<String> cleanSteps(Object data) {
        return new ArrayList<>();
    }

    /**
     * TODO 2 of 9 (unlocks m1 and e2): run one worker call for one subtask.
     * Receives ask, the subtask and the task. Calls ask with "Subtask: " + subtask + "\nTask: " + task and returns the map
     * {subtask, status "ok", output} for a good reply, or {subtask, status "failed", error} when the reply is null, empty or only
     * spaces (error "empty reply") or the call throws (error: the exception's message). It never throws.
     * Example: a worker that replies "  " -> {subtask: "s", status: "failed", error: "empty reply"}
     */
    private static Map<String, Object> runWorker(Function<String, String> ask, String subtask, String task) {
        LOG.log(System.Logger.Level.DEBUG, "runWorker input: {0}", subtask);
        return map("subtask", subtask, "status", "failed", "error", "");
    }

    /**
     * TODO 3 of 9 (unlocks m1): the prompt of the combine call.
     * Receives the task and the results in plan order. Returns "Combine: write one answer to the task from the results.\nTask: " + task
     * followed by one line per result, "\n{i}. {subtask} -> {output}" or "\n{i}. {subtask} -> FAILED" (i from 1).
     * Example: task "T", one ok result "a" with output "x" -> "Combine: write one answer to the task from the results.\nTask: T\n1. a -> x"
     */
    private static String combinePrompt(String task, List<Map<String, Object>> results) {
        return "";
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

    /**
     * TODO 4 of 9 (unlocks e4): is this a usable judge score?
     * Receives the score field of the judge's JSON (any object, or null). Returns true for a number from 0 to 10 and false for
     * anything else, a boolean included.
     * Example: scoreOk(7.5) -> true, scoreOk(true) -> false, scoreOk(11) -> false
     */
    private static boolean scoreOk(Object score) {
        return score instanceof Number; // a number of any size; add the range
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

    /**
     * TODO 5 of 9 (unlocks e3): the prompt for the writer in one round.
     * Receives the task, the latest draft, the judge's latest feedback and the round number (from 1). Returns "Task: " + task in
     * round 1 and afterwards "Task: " + task + "\nPrevious draft: " + draft + "\nFeedback: " + feedback + "\nRevise the draft."
     * Example: writerPrompt("T", "d", "f", 2) -> "Task: T\nPrevious draft: d\nFeedback: f\nRevise the draft."
     */
    private static String writerPrompt(String task, String draft, String feedback, int round) {
        return "Task: " + task;
    }

    /**
     * TODO 6 of 9 (unlocks e4): does this draft replace the best one so far?
     * Receives the new score and the best score so far (null before any draft was judged), both numbers. Returns true when there is
     * no best yet or the new score is strictly higher (so the earliest wins a tie).
     * Example: isBetter(5, null) -> true, isBetter(5, 5) -> false
     */
    private static boolean isBetter(Object score, Object bestScore) {
        return false;
    }

    /**
     * TODO 7 of 9 (unlocks e7): the result when the writer threw.
     * Receives the best draft and score so far (null, null before any), the rounds completed, the history and the exception. Returns
     * the map {status "error", draft, score, rounds, history, error: the exception's message}.
     * Example: errorResult("d", 4, 1, List.of(), new RuntimeException("boom")).get("error") -> "boom"
     */
    private static Map<String, Object> errorResult(String best, Object bestScore, int rounds, List<Object> history, RuntimeException e) {
        return map("status", "error", "draft", best, "score", bestScore, "rounds", rounds, "history", history, "error", "");
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

    /**
     * TODO 8 of 9 (unlocks e5): the label in a classifier's reply.
     * Receives the reply (a string, or null). Returns it stripped of spaces, then of the punctuation .,;:!"' and the backtick at both
     * ends, then of spaces again, in lower case; "" when the reply is null.
     * Example: normaliseLabel("  \"Billing.\" ") -> "billing"
     */
    private static String normaliseLabel(String reply) {
        return "";
    }

    /** Classify the text with one model call, then run the handler of the label. routes maps label to a function of the text. */
    static Map<String, Object> route(Function<String, String> ask, String text, Map<String, Function<String, String>> routes, String defaultLabel) {
        String reply = ask.apply("Classify: " + text + "\nLabels: " + String.join(", ", routes.keySet()));
        String label = normaliseLabel(reply);
        boolean fallback = !routes.containsKey(label);
        if (fallback) label = defaultLabel;
        return map("label", label, "output", routes.get(label).apply(text), "fallback", fallback);
    }

    /**
     * TODO 9 of 9 (unlocks e6): the answer that wins the vote.
     * Receives the counts (answer to count, in order of first appearance) and the highest count. Returns the first answer whose count
     * is that highest count, so a tie goes to the one seen first.
     * Example: pickWinner({a=2, b=2}, 2) -> "a"
     */
    private static String pickWinner(Map<String, Integer> counts, int top) {
        return "";
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
