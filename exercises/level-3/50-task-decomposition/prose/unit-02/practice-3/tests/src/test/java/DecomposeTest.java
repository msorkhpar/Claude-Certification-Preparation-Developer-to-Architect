import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class DecomposeTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, String> file(String path, String text) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("path", path);
        m.put("text", text);
        return m;
    }

    private static Map<String, String> summary(String path, String text) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("path", path);
        m.put("summary", text);
        return m;
    }

    private static List<Map<String, String>> files() {
        return List.of(file("api.py", "def get(): ..."), file("db.py", "def query(): ..."), file("ui.py", "def show(): ..."));
    }

    /** A scripted file pass: findings and summary by the name before the dot; every call is kept. A script that is a RuntimeException is thrown. */
    private static final class Passes implements Decompose.FilePass {
        final Map<String, Object> scripted = new LinkedHashMap<>();
        final List<List<Object>> calls = new ArrayList<>();

        Passes on(String name, Object script) {
            scripted.put(name, script);
            return this;
        }

        public Map<String, Object> apply(String path, String text, int part, int parts) {
            calls.add(Arrays.asList(path, text, part, parts));
            Object script = scripted.getOrDefault(path.split("\\.")[0], map("findings", List.of(), "summary", path + " part " + part));
            if (script instanceof RuntimeException e) throw e;
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) script;
            return result;
        }
    }

    private static final class Cross implements Function<List<Map<String, String>>, List<String>> {
        final List<String> findings;
        final RuntimeException error;
        final List<List<Map<String, String>>> seen = new ArrayList<>();

        Cross(List<String> findings, RuntimeException error) {
            this.findings = findings;
            this.error = error;
        }

        public List<String> apply(List<Map<String, String>> summaries) {
            seen.add(summaries);
            if (error != null) throw error;
            return findings;
        }
    }

    private static Cross cross(String... findings) {
        return new Cross(List.of(findings), null);
    }

    private static Map<String, Object> review(List<Map<String, String>> files, Decompose.FilePass pass, Cross cross, int maxLines) {
        Map<String, Object> result = Decompose.reviewChanges(files, pass, cross, maxLines);
        assertNotNull(result, "reviewChanges returned null");
        return result;
    }

    private static Map<String, Object> review(List<Map<String, String>> files, Decompose.FilePass pass, Cross cross) {
        Map<String, Object> result = Decompose.reviewChanges(files, pass, cross);
        assertNotNull(result, "reviewChanges returned null");
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sub(Map<String, Object> m, String key) {
        return (Map<String, Object>) m.get(key);
    }

    private static Map<String, Object> scripted(String finding, String summary) {
        return map("findings", finding.isEmpty() ? List.of() : List.of(finding), "summary", summary);
    }

    @Test
    void m1_eachFileIsReviewedAloneAndTheCrossPassReadsTheirSummaries() {
        Passes passes = new Passes().on("api", scripted("api: no auth", "api calls db.query(id)")).on("db", scripted("", "db.query takes a name")).on("ui", scripted("ui: unused", "ui shows rows"));
        Cross cross = cross("api passes id but db expects a name");
        Map<String, Object> result = review(files(), passes, cross);
        assertEquals(List.of("api.py", "db.py", "ui.py"), passes.calls.stream().map(c -> c.get(0)).toList());
        assertEquals(map("findings", List.of("api: no auth"), "summary", "api calls db.query(id)", "parts", 1), sub(sub(result, "files"), "api.py"));
        assertEquals(List.of("ui: unused"), sub(sub(result, "files"), "ui.py").get("findings"));
        assertEquals(List.of("api passes id but db expects a name"), result.get("cross"));
        assertEquals(List.of(List.of(summary("api.py", "api calls db.query(id)"), summary("db.py", "db.query takes a name"), summary("ui.py", "ui shows rows"))), cross.seen);
        assertTrue(sub(result, "failed").isEmpty() && ((List<?>) result.get("skipped")).isEmpty() && result.get("cross_error") == null);
    }

    @Test
    void e1_aFilePassSeesOnlyItsOwnFileAndTheCrossPassSeesSummariesNeverTheText() {
        Passes passes = new Passes();
        Cross cross = cross();
        review(files(), passes, cross);
        assertEquals(List.of(List.of("api.py", "def get(): ...", 1, 1), List.of("db.py", "def query(): ...", 1, 1), List.of("ui.py", "def show(): ...", 1, 1)), passes.calls);
        assertEquals(1, cross.seen.size());
        List<Map<String, String>> seen = cross.seen.get(0);
        assertEquals(3, seen.size());
        for (Map<String, String> item : seen) assertEquals(List.of("path", "summary"), List.copyOf(item.keySet()));
        assertFalse(seen.toString().contains("def "));
    }

    @Test
    void e2_aLongFileIsReviewedInLabelledPartsAndABlankFileIsSkipped() {
        String longText = String.join("\n", "line 1", "line 2", "line 3", "line 4", "line 5", "line 6", "line 7");
        Passes passes = new Passes();
        Map<String, Object> result = review(List.of(file("big.py", longText), file("empty.py", "  \n \n"), file("small.py", "x = 1")), passes, cross(), 3);
        assertEquals(List.of(List.of("big.py", "line 1\nline 2\nline 3", 1, 3), List.of("big.py", "line 4\nline 5\nline 6", 2, 3), List.of("big.py", "line 7", 3, 3), List.of("small.py", "x = 1", 1, 1)), passes.calls);
        assertEquals(3, sub(sub(result, "files"), "big.py").get("parts"));
        assertEquals("big.py part 1 big.py part 2 big.py part 3", sub(sub(result, "files"), "big.py").get("summary"));
        assertEquals(List.of("empty.py"), result.get("skipped"));
        assertFalse(sub(result, "files").containsKey("empty.py"));
        assertEquals(1, sub(sub(review(List.of(file("a.py", "1\n2\n3")), new Passes(), cross(), 3), "files"), "a.py").get("parts"));
    }

    @Test
    void e3_aFailingFileIsReportedAndLeftOutOfTheCrossPassWhichNeedsTwoFiles() {
        Passes passes = new Passes().on("db", new RuntimeException("model timed out")).on("api", scripted("f1", "api summary")).on("ui", scripted("f2", "ui summary"));
        Cross cross = cross("relation");
        Map<String, Object> result = review(files(), passes, cross);
        assertEquals(map("db.py", "model timed out"), result.get("failed"));
        assertEquals(List.of("api.py", "ui.py"), List.copyOf(sub(result, "files").keySet()));
        assertEquals(List.of(List.of(summary("api.py", "api summary"), summary("ui.py", "ui summary"))), cross.seen);
        Cross lone = cross("never");
        Map<String, Object> one = review(files(), new Passes().on("db", new RuntimeException("boom")).on("ui", new RuntimeException("boom")), lone);
        assertEquals(List.of("api.py"), List.copyOf(sub(one, "files").keySet()));
        assertTrue(lone.seen.isEmpty() && ((List<?>) one.get("cross")).isEmpty());
        assertEquals(List.of("db.py", "ui.py"), List.copyOf(sub(one, "failed").keySet()));
        Map<String, Object> broken = review(files(), new Passes(), new Cross(List.of(), new RuntimeException("cross failed")));
        assertTrue(((List<?>) broken.get("cross")).isEmpty() && "cross failed".equals(broken.get("cross_error")) && sub(broken, "files").size() == 3);
    }

    /** A scripted planner: the replies in order; every call is kept with the steps it was given. */
    private static final class Planner implements BiFunction<String, List<Map<String, String>>, Object> {
        final List<Object> replies;
        final List<String> goals = new ArrayList<>();
        final List<List<Map<String, String>>> histories = new ArrayList<>();

        Planner(Object... replies) {
            this.replies = Arrays.asList(replies);
        }

        public Object apply(String goal, List<Map<String, String>> steps) {
            goals.add(goal);
            histories.add(steps);
            return replies.get(Math.min(goals.size(), replies.size()) - 1);
        }
    }

    private static Map<String, Object> next(String subtask) {
        return map("done", false, "next", subtask);
    }

    private static Map<String, Object> finished(String summary) {
        return map("done", true, "summary", summary);
    }

    private static Map<String, String> step(String subtask, String result) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("subtask", subtask);
        m.put("result", result);
        return m;
    }

    private static Map<String, Object> adapt(Planner planner, List<String> ran, int maxSteps) {
        Map<String, Object> result = Decompose.runAdaptive(planner, subtask -> {
            ran.add(subtask);
            return "did " + subtask;
        }, "map the module", maxSteps);
        assertNotNull(result, "runAdaptive returned null");
        return result;
    }

    private static Map<String, Object> adapt(Planner planner, List<String> ran) {
        Map<String, Object> result = Decompose.runAdaptive(planner, subtask -> {
            ran.add(subtask);
            return "did " + subtask;
        }, "map the module");
        assertNotNull(result, "runAdaptive returned null");
        return result;
    }

    @Test
    void e4_thePlannerIsAskedAgainAfterEachStepWithTheStepsSoFarAndTheLoopEndsWhenItSaysDone() {
        Planner planner = new Planner(next("list the files"), next("read the entry point"), finished("two modules, one entry point"));
        List<String> ran = new ArrayList<>();
        Map<String, Object> result = adapt(planner, ran);
        assertEquals(List.of("list the files", "read the entry point"), ran);
        assertEquals("done", result.get("status"));
        assertEquals("two modules, one entry point", result.get("summary"));
        assertEquals(List.of(step("list the files", "did list the files"), step("read the entry point", "did read the entry point")), result.get("steps"));
        assertEquals(List.of(0, 1, 2), planner.histories.stream().map(List::size).toList());
        assertEquals("map the module", planner.goals.get(0));
        assertEquals(step("read the entry point", "did read the entry point"), planner.histories.get(2).get(1));
        Planner failing = new Planner(next("open the file"), finished("saw the error"));
        Map<String, Object> failed = Decompose.runAdaptive(failing, subtask -> {
            throw new IllegalStateException("no such file");
        }, "map the module");
        assertNotNull(failed, "runAdaptive returned null");
        assertEquals(List.of(step("open the file", "ERROR: no such file")), failed.get("steps"));
        assertEquals(2, failing.histories.size());
        assertEquals("ERROR: no such file", failing.histories.get(1).get(0).get("result"));
    }

    @Test
    void e5_theLoopStopsOnARepeatedSubtaskOrNoNextStepOrAnUnreadableReplyAndCountsTheStepLimitExactly() {
        List<String> ran = new ArrayList<>();
        Map<String, Object> repeated = adapt(new Planner(next("list the files"), next("  List The Files ")), ran);
        assertEquals("stuck", repeated.get("status"));
        assertEquals(List.of("list the files"), ran);
        assertTrue(String.valueOf(repeated.get("reason")).toLowerCase().contains("list the files"));
        Map<String, Object> blank = adapt(new Planner(next("   ")), new ArrayList<>());
        assertEquals("stuck", blank.get("status"));
        assertTrue(((List<?>) blank.get("steps")).isEmpty());
        for (Object reply : new Object[] {"not a plan", map("next", "x"), map("done", "yes"), null}) {
            List<String> none = new ArrayList<>();
            Map<String, Object> bad = adapt(new Planner(reply), none);
            assertEquals("bad_plan", bad.get("status"), reply + " must be a bad plan");
            assertTrue(none.isEmpty());
        }
        Object[] endless = new Object[9];
        for (int n = 0; n < 9; n++) endless[n] = next("step " + (n + 1));
        Planner planner = new Planner(endless);
        List<String> limitedRan = new ArrayList<>();
        Map<String, Object> limited = adapt(planner, limitedRan, 3);
        assertEquals("step_limit", limited.get("status"));
        assertEquals(List.of("step 1", "step 2", "step 3"), limitedRan);
        assertEquals(4, planner.goals.size());
        List<String> lastRan = new ArrayList<>();
        Map<String, Object> last = adapt(new Planner(next("one"), next("two"), finished("finished on the last step")), lastRan, 2);
        assertEquals("done", last.get("status"));
        assertEquals(List.of("one", "two"), lastRan);
    }

    private static Map<String, Object> task(Object stepsKnown, Object items, Object interact) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (stepsKnown != null) m.put("steps_known", stepsKnown);
        if (items != null) m.put("items", items);
        if (interact != null) m.put("items_interact", interact);
        return m;
    }

    @Test
    void e6_theStrategyFollowsWhatIsKnownAboutTheStepsAndWhetherTheItemsInteract() {
        assertEquals("fixed_chain", Decompose.chooseStrategy(task(true, 1, false)));
        assertEquals("fixed_chain", Decompose.chooseStrategy(task(true, 12, false)));
        assertEquals("per_item_then_cross", Decompose.chooseStrategy(task(true, 12, true)));
        assertEquals("fixed_chain", Decompose.chooseStrategy(task(true, 1, true)));
        assertEquals("adaptive", Decompose.chooseStrategy(task(false, 12, true)));
        assertEquals("adaptive", Decompose.chooseStrategy(task(false, 0, null)));
        assertEquals("fixed_chain", Decompose.chooseStrategy(task(true, 3, null)));
        int bad = 0;
        for (Map<String, Object> t : List.of(task(null, 3, null), task(true, null, null), task(true, -1, null), task("yes", 2, null), task(true, 2.5, null))) {
            try {
                Decompose.chooseStrategy(t);
            } catch (IllegalArgumentException expected) {
                bad++;
            }
        }
        assertEquals(5, bad);
    }
}
