import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class WorkflowsTest {
    private static final String PLAN3 = "[\"research the topic\", \"draft the outline\", \"check the facts\"]";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** A stand-in for the model: the first handler whose prefix starts the prompt answers; every prompt is kept. */
    private static final class Model implements Function<String, String> {
        final Map<String, Function<String, String>> byPrefix = new LinkedHashMap<>();
        final List<String> prompts = new ArrayList<>();

        Model on(String prefix, String answer) {
            byPrefix.put(prefix, p -> answer);
            return this;
        }

        Model on(String prefix, Function<String, String> handler) {
            byPrefix.put(prefix, handler);
            return this;
        }

        public String apply(String prompt) {
            prompts.add(prompt);
            for (Map.Entry<String, Function<String, String>> e : byPrefix.entrySet()) if (prompt.startsWith(e.getKey())) return e.getValue().apply(prompt);
            return fail("no scripted answer for " + prompt);
        }
    }

    private static Function<String, String> sequence(String... items) {
        List<String> queue = new ArrayList<>(Arrays.asList(items));
        return p -> queue.remove(0);
    }

    private static Model judged(String... replies) {
        return new Model().on("Judge", sequence(replies));
    }

    private static List<String> starting(Model m, String prefix) {
        return m.prompts.stream().filter(p -> p.startsWith(prefix)).toList();
    }

    private static Map<String, Object> orEmpty(Map<String, Object> m) {
        return m == null ? new LinkedHashMap<>() : m;
    }

    private static Object n(Object o) {
        return o instanceof Number x ? (Object) x.doubleValue() : o;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? new ArrayList<>() : (List<Map<String, Object>>) v;
    }

    @Test
    void m1_anOrchestratorPlansRunsAWorkerPerSubtaskAndCombines() {
        Model m = new Model().on("Plan", PLAN3).on("Subtask", p -> "done: " + p.split("\n")[0].substring("Subtask: ".length())).on("Combine", "FINAL");
        Map<String, Object> result = orEmpty(Workflows.orchestrate(m, "Write a guide"));
        assertEquals(Arrays.asList("done", false, "FINAL", 5), Arrays.asList(result.get("status"), result.get("fallback"), result.get("answer"), result.get("calls")));
        assertEquals(List.of("research the topic", "draft the outline", "check the facts"), result.get("plan"));
        assertEquals(List.of(List.of("research the topic", "ok", "done: research the topic"), List.of("draft the outline", "ok", "done: draft the outline"), List.of("check the facts", "ok", "done: check the facts")),
                list(result, "results").stream().map(r -> List.of(r.get("subtask"), r.get("status"), r.get("output"))).toList());
        assertEquals(List.of("Plan: split the task into at most 5 independent subtasks. Reply with a JSON array of strings only.\nTask: Write a guide"), m.prompts.subList(0, Math.min(1, m.prompts.size())));
        assertEquals(List.of("Subtask: research the topic\nTask: Write a guide"), starting(m, "Subtask").subList(0, Math.min(1, starting(m, "Subtask").size())));
        assertEquals(3, starting(m, "Subtask").size());
        assertEquals(List.of("Combine: write one answer to the task from the results.\nTask: Write a guide\n1. research the topic -> done: research the topic\n"
                + "2. draft the outline -> done: draft the outline\n3. check the facts -> done: check the facts"), starting(m, "Combine"));
    }

    @Test
    void e1_thePlanIsReadFromProseCleanedCappedAndReplacedWhenUnusable() {
        String reply = "Here is the plan:\n```json\n[\"a\", \"b\", \"a\", \"  c  \", \"\", 5, \"d\", \"e\"]\n```\nGood luck.";
        Map<String, Object> capped = orEmpty(Workflows.orchestrate(new Model().on("Plan", reply).on("Subtask", "x").on("Combine", "F"), "T", 3));
        assertEquals(List.of("a", "b", "c"), capped.get("plan"));
        assertEquals(5, capped.get("calls"));
        assertEquals(List.of("a", "b", "c", "d", "e"), orEmpty(Workflows.orchestrate(new Model().on("Plan", reply).on("Subtask", "x").on("Combine", "F"), "T")).get("plan"));
        Model seen = new Model().on("Plan", reply).on("Subtask", "x").on("Combine", "F");
        Workflows.orchestrate(seen, "T", 2);
        assertTrue(!seen.prompts.isEmpty() && seen.prompts.get(0).startsWith("Plan: split the task into at most 2 independent subtasks."));
        for (String bad : List.of("I cannot plan this.", "[]", "[\"\", 7]", "[not json]")) {
            Model m = new Model().on("Plan", bad).on("Subtask", "x").on("Combine", "F");
            Map<String, Object> result = orEmpty(Workflows.orchestrate(m, "T"));
            assertEquals(Arrays.asList(List.of("T"), true, 3), Arrays.asList(result.get("plan"), result.get("fallback"), result.get("calls")));
            assertEquals(List.of("Subtask: T\nTask: T"), starting(m, "Subtask"));
        }
    }

    @Test
    void e2_oneFailingWorkerDoesNotStopTheOthersOrTheAnswer() {
        Function<String, String> worker = prompt -> {
            if (prompt.startsWith("Subtask: b")) throw new IllegalStateException("disk full");
            return prompt.startsWith("Subtask: c") ? "" : "ok " + prompt.charAt(9);
        };
        Model m = new Model().on("Plan", "[\"a\", \"b\", \"c\"]").on("Subtask", worker).on("Combine", "FINAL");
        Map<String, Object> result = orEmpty(Workflows.orchestrate(m, "T"));
        assertEquals(Arrays.asList("partial", "FINAL", 5), Arrays.asList(result.get("status"), result.get("answer"), result.get("calls")));
        assertEquals(List.of(Arrays.asList("a", "ok", null), Arrays.asList("b", "failed", "disk full"), Arrays.asList("c", "failed", "empty reply")),
                list(result, "results").stream().map(r -> Arrays.asList(r.get("subtask"), r.get("status"), r.get("error"))).toList());
        assertEquals(List.of("Combine: write one answer to the task from the results.\nTask: T\n1. a -> ok a\n2. b -> FAILED\n3. c -> FAILED"), starting(m, "Combine"));
        Model nothing = new Model().on("Plan", "[\"a\", \"b\"]").on("Subtask", "  ").on("Combine", "never");
        Map<String, Object> failed = orEmpty(Workflows.orchestrate(nothing, "T"));
        assertEquals(Arrays.asList("failed", null, 3), Arrays.asList(failed.get("status"), failed.get("answer"), failed.get("calls")));
        assertEquals(List.of(), starting(nothing, "Combine"));
    }

    @Test
    void e3_aDraftIsRevisedWithTheFeedbackUntilTheJudgeAcceptsIt() {
        Model writer = new Model().on("Task", sequence("draft1", "draft2", "draft3"));
        Model judge = judged("{\"score\": 5, \"feedback\": \"add examples\"}", "{\"score\": 9, \"feedback\": \"good\"}");
        Map<String, Object> result = orEmpty(Workflows.refine(writer, judge, "T"));
        assertEquals(Arrays.asList("accepted", "draft2", 9.0, 2), Arrays.asList(result.get("status"), result.get("draft"), n(result.get("score")), result.get("rounds")));
        assertEquals(List.of("Task: T", "Task: T\nPrevious draft: draft1\nFeedback: add examples\nRevise the draft."), writer.prompts);
        assertEquals("Judge: score the draft from 0 to 10 and reply with JSON {\"score\": n, \"feedback\": \"...\"}.\nTask: T\nDraft: draft1", judge.prompts.isEmpty() ? "" : judge.prompts.get(0));
        assertEquals(List.of(Arrays.asList(1, 5.0, "add examples"), Arrays.asList(2, 9.0, "good")),
                list(result, "history").stream().map(h -> Arrays.asList(h.get("round"), n(h.get("score")), h.get("feedback"))).toList());
        Map<String, Object> edge = orEmpty(Workflows.refine(new Model().on("Task", "d"), judged("{\"score\": 8, \"feedback\": \"\"}"), "T"));
        assertEquals(Arrays.asList("accepted", 1), Arrays.asList(edge.get("status"), edge.get("rounds")));
        Map<String, Object> custom = orEmpty(Workflows.refine(new Model().on("Task", "d"), judged("{\"score\": 8, \"feedback\": \"\"}", "{\"score\": 10}"), "T", 4, 10));
        assertEquals(Arrays.asList("accepted", 2, 10.0), Arrays.asList(custom.get("status"), custom.get("rounds"), n(custom.get("score"))));
    }

    @Test
    void e4_whenTheRoundsRunOutTheBestDraftWinsAndAnUnreadableJudgeScoresZero() {
        Map<String, Object> result = orEmpty(Workflows.refine(new Model().on("Task", sequence("draft1", "draft2", "draft3")),
                judged("{\"score\": 6, \"feedback\": \"x\"}", "{\"score\": 7, \"feedback\": \"y\"}", "{\"score\": 7, \"feedback\": \"z\"}"), "T"));
        assertEquals(Arrays.asList("max_rounds", "draft2", 7.0, 3), Arrays.asList(result.get("status"), result.get("draft"), n(result.get("score")), result.get("rounds")));
        Map<String, Object> unreadable = orEmpty(Workflows.refine(new Model().on("Task", sequence("draft1", "draft2", "draft3", "draft4")),
                judged("not json", "{\"score\": \"high\"}", "{\"score\": 11}", "{\"score\": true, \"feedback\": \"x\"}"), "T", 4, 8));
        assertEquals(Arrays.asList("max_rounds", "draft1", 0.0), Arrays.asList(unreadable.get("status"), unreadable.get("draft"), n(unreadable.get("score"))));
        assertEquals(Collections.nCopies(4, "The judge reply could not be read."), list(unreadable, "history").stream().map(h -> h.get("feedback")).toList());
        Map<String, Object> prose = orEmpty(Workflows.refine(new Model().on("Task", sequence("d1", "d2")), judged("hmm", "Verdict: {\"score\": 9, \"feedback\": \"ok\"} thanks"), "T"));
        assertEquals(Arrays.asList("accepted", "d2", 9.0), Arrays.asList(prose.get("status"), prose.get("draft"), n(prose.get("score"))));
    }

    @Test
    void e5_aLabelIsReadFromTheReplyAndAnythingElseTakesTheDefaultRoute() {
        Map<String, Function<String, String>> routes = new LinkedHashMap<>();
        routes.put("billing", t -> "B:" + t);
        routes.put("technical", t -> "T:" + t);
        routes.put("other", t -> "O:" + t);
        String text = "My card was charged twice";
        Model m = new Model().on("Classify", " Billing. ");
        assertEquals(map("label", "billing", "output", "B:" + text, "fallback", false), Workflows.route(m, text, routes, "other"));
        assertEquals(List.of("Classify: My card was charged twice\nLabels: billing, technical, other"), m.prompts);
        assertEquals("technical", orEmpty(Workflows.route(new Model().on("Classify", "TECHNICAL"), text, routes, "other")).get("label"));
        for (String reply : List.of("I think it is billing", "", "refund")) {
            assertEquals(map("label", "other", "output", "O:" + text, "fallback", true), Workflows.route(new Model().on("Classify", reply), text, routes, "other"));
        }
    }

    @Test
    void e6_theMajorityAnswerWinsAndATieGoesToTheOneSeenFirst() {
        Model m = new Model().on("Is", sequence("Yes", "yes ", " NO", "yes"));
        Map<String, Object> result = orEmpty(Workflows.vote(m, "Is it safe?", 4));
        assertEquals(Arrays.asList("yes", Map.of("yes", 3, "no", 1), 0.75), Arrays.asList(result.get("answer"), result.get("votes"), result.get("agreement")));
        assertEquals(Collections.nCopies(4, "Is it safe?"), m.prompts);
        assertEquals("b", orEmpty(Workflows.vote(new Model().on("Is", sequence("b", "a", "a", "b")), "Is it safe?", 4)).get("answer"));
        Map<String, Object> none = Workflows.vote(new Model().on("Is", "x"), "Is it safe?", 0);
        assertTrue(none != null && none.containsKey("answer") && none.get("answer") == null);
        assertEquals(Map.of("x", 5), orEmpty(Workflows.vote(new Model().on("Is", "x"), "Is it safe?")).get("votes"));
    }

    @Test
    void e7_aWriterThatFailsEndsTheLoopWithTheBestDraftSoFar() {
        boolean[] first = {true};
        Function<String, String> flaky = p -> {
            if (first[0]) {
                first[0] = false;
                return "draft1";
            }
            throw new IllegalStateException("boom");
        };
        Map<String, Object> result = orEmpty(Workflows.refine(new Model().on("Task", flaky), judged("{\"score\": 4, \"feedback\": \"more\"}"), "T"));
        assertEquals(Arrays.asList("error", "draft1", 4.0, 1, "boom"), Arrays.asList(result.get("status"), result.get("draft"), n(result.get("score")), result.get("rounds"), result.get("error")));
        assertEquals(1, list(result, "history").size());
        Map<String, Object> none = Workflows.refine(new Model().on("Task", p -> {
            throw new IllegalStateException("down");
        }), judged(), "T");
        assertTrue(none != null && none.containsKey("draft"));
        assertEquals(Arrays.asList("error", null, null, 0, "down"), Arrays.asList(none.get("status"), none.get("draft"), none.get("score"), none.get("rounds"), none.get("error")));
    }
}
