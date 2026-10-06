import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CoordinatorTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Function<String, Map<String, Object>> plan(boolean delegate, String answer, String[]... subtasks) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String[] s : subtasks) list.add(map("scope", s[0], "brief", s[1]));
        return question -> map("delegate", delegate, "answer", answer, "subtasks", list);
    }

    private static Function<String, Map<String, Object>> plan(String[]... subtasks) {
        return plan(true, null, subtasks);
    }

    /** Subagents: the report is looked up by the start of the brief; every brief received is kept. */
    private static final class Spokes implements Function<String, String> {
        final Map<String, Object> reports = new LinkedHashMap<>();
        final List<String> briefs = new ArrayList<>();

        Spokes on(String prefix, Object report) {
            reports.put(prefix, report);
            return this;
        }

        public String apply(String brief) {
            briefs.add(brief);
            for (Map.Entry<String, Object> e : reports.entrySet()) {
                if (brief.startsWith(e.getKey())) {
                    if (e.getValue() instanceof RuntimeException ex) throw ex;
                    return (String) e.getValue();
                }
            }
            return fail("no scripted report for " + brief);
        }
    }

    private static final BiFunction<String, List<Map<String, Object>>, List<String>> NO_GAPS = (q, f) -> List.of();
    private static final BiFunction<String, List<Map<String, Object>>, String> ECHO = (q, f) -> String.join(" | ", f.stream().map(x -> (String) x.get("text")).toList());
    private static final String[] CHIPS = {"chips", "chips: find 2024 chip supply news"};
    private static final String[] CARS = {"cars", "cars: find 2024 car output news"};
    private static final String[] RATES = {"rates", "rates: find 2024 interest rates"};

    private static Spokes reports() {
        return new Spokes().on("chips", "chips report").on("cars", "cars report").on("rates", "rates report");
    }

    @Test
    void m1_aHubSendsOneBriefToEachSpokeAndSynthesizesWhatComesBack() {
        Spokes spokes = reports();
        Map<String, Object> r = Coordinator.coordinate(plan(CHIPS, CARS, RATES), spokes, NO_GAPS, ECHO, "How did supply change?");
        assertNotNull(r, "coordinate returned null");
        assertEquals(List.of("complete", "chips report | cars report | rates report", 3, 0), List.of(r.get("status"), r.get("answer"), r.get("subagent_calls"), r.get("rounds")));
        assertEquals(List.of(map("scope", "chips", "text", "chips report"), map("scope", "cars", "text", "cars report"), map("scope", "rates", "text", "rates report")), r.get("findings"));
        assertEquals(List.of(List.of(), List.of(), List.of()), List.of(r.get("failed"), r.get("dropped"), r.get("gaps")));
    }

    @Test
    void e1_aQuestionTheCoordinatorCanAnswerItselfIsAnsweredWithoutAnySubagent() {
        Spokes spokes = new Spokes();
        List<String> asked = new ArrayList<>();
        Map<String, Object> r = Coordinator.coordinate(plan(false, "Paris.", CHIPS, CARS, RATES), spokes, (q, f) -> {
            asked.add(q);
            return List.of();
        }, (q, f) -> {
            asked.add("synth");
            return "x";
        }, "Capital of France?");
        assertNotNull(r, "coordinate returned null");
        assertEquals(List.of("direct", "Paris.", 0, List.of()), List.of(r.get("status"), r.get("answer"), r.get("subagent_calls"), r.get("findings")));
        assertEquals(List.of(0, 0), List.of(spokes.briefs.size(), asked.size()));
    }

    @Test
    void e2_aSubagentSeesItsOwnBriefAndNothingTheOthersFound() {
        Spokes spokes = reports();
        Coordinator.coordinate(plan(CHIPS, CARS, RATES), spokes, NO_GAPS, ECHO, "How did supply change?");
        assertEquals(3, spokes.briefs.size());
        assertEquals(CARS[1], spokes.briefs.get(1));
        assertEquals(RATES[1], spokes.briefs.get(2));
        assertTrue(spokes.briefs.stream().noneMatch(b -> b.contains("report")));
    }

    @Test
    void e3_thePlanIsCleanedOfEmptyBriefsAndDuplicateScopesAndCapped() {
        Spokes spokes = new Spokes().on("a", "a report").on("b", "b report").on("c", "c report");
        Function<String, Map<String, Object>> messy = plan(new String[] {"A", "a: first"}, new String[] {"a ", "a: again"}, new String[] {"B", "   "},
                new String[] {"b", "b: second"}, new String[] {"c", "c: third"}, new String[] {"d", "d: fourth"});
        Map<String, Object> r = Coordinator.coordinate(messy, spokes, NO_GAPS, ECHO, "q", 3, 2);
        assertNotNull(r, "coordinate returned null");
        assertEquals(List.of("a: first", "b: second", "c: third"), spokes.briefs);
        assertEquals(3, r.get("subagent_calls"));
        assertEquals(List.of(map("scope", "a ", "reason", "duplicate scope"), map("scope", "B", "reason", "empty brief"), map("scope", "d", "reason", "over limit")), r.get("dropped"));
        Map<String, Object> empty = Coordinator.coordinate(plan(new String[] {"x", ""}, new String[] {"y", "  "}), new Spokes(), NO_GAPS, ECHO, "q");
        assertNotNull(empty, "coordinate returned null");
        assertEquals(Arrays.asList("failed", 0, null), Arrays.asList(empty.get("status"), empty.get("subagent_calls"), empty.get("answer")));
    }

    @Test
    void e4_aFailingSubagentDoesNotStopTheOthersAndARunWithNoFindingsDoesNotSynthesize() {
        Spokes spokes = new Spokes().on("chips", new IllegalStateException("search offline")).on("cars", "cars report").on("rates", "   ");
        Map<String, Object> r = Coordinator.coordinate(plan(CHIPS, CARS, RATES), spokes, NO_GAPS, ECHO, "q");
        assertNotNull(r, "coordinate returned null");
        assertEquals(List.of("partial", "cars report", 3), List.of(r.get("status"), r.get("answer"), r.get("subagent_calls")));
        assertEquals(List.of(map("scope", "chips", "error", "search offline"), map("scope", "rates", "error", "empty report")), r.get("failed"));
        List<Object> synthesized = new ArrayList<>();
        Map<String, Object> dead = Coordinator.coordinate(plan(CHIPS, CARS, RATES), new Spokes().on("chips", new IllegalStateException("x")).on("cars", new IllegalStateException("y")).on("rates", ""),
                NO_GAPS, (q, f) -> {
                    synthesized.add(f);
                    return "z";
                }, "q");
        assertNotNull(dead, "coordinate returned null");
        assertEquals(Arrays.asList("failed", null, List.of(), 0), Arrays.asList(dead.get("status"), dead.get("answer"), dead.get("findings"), synthesized.size()));
    }

    @Test
    void e5_aReviewSendsOnlyTheGapsBackOutAndStopsWhenNoneAreLeft() {
        Spokes spokes = new Spokes().on("chips", "chips report").on("cars", "cars report").on("Follow", "follow-up report");
        List<List<Object>> reviews = new ArrayList<>();
        BiFunction<String, List<Map<String, Object>>, List<String>> reviewer = (q, findings) -> {
            reviews.add(findings.stream().map(f -> f.get("scope")).toList());
            return reviews.size() == 1 ? List.of("2023 baseline", "  ", "2023 baseline") : List.of();
        };
        Map<String, Object> r = Coordinator.coordinate(plan(CHIPS, CARS), spokes, reviewer, ECHO, "How did supply change?");
        assertNotNull(r, "coordinate returned null");
        assertEquals(List.of(CHIPS[1], CARS[1], "Follow up: 2023 baseline\nQuestion: How did supply change?"), spokes.briefs);
        assertEquals(List.of(List.of("chips", "cars"), List.of("chips", "cars", "2023 baseline")), reviews);
        assertEquals(List.of("complete", 1, 3, List.of()), List.of(r.get("status"), r.get("rounds"), r.get("subagent_calls"), r.get("gaps")));
        assertEquals("chips report | cars report | follow-up report", r.get("answer"));
    }

    @Test
    void e6_theRoundsAreCappedAndTheGapsThatRemainAreReported() {
        Spokes spokes = new Spokes().on("chips", "chips report").on("Follow", "more");
        List<Integer> reviews = new ArrayList<>();
        Map<String, Object> r = Coordinator.coordinate(plan(CHIPS), spokes, (q, findings) -> {
            reviews.add(findings.size());
            return List.of("still missing");
        }, ECHO, "q", 4, 2);
        assertNotNull(r, "coordinate returned null");
        assertEquals(List.of("partial", 2, 3, List.of("still missing")), List.of(r.get("status"), r.get("rounds"), r.get("subagent_calls"), r.get("gaps")));
        assertEquals(List.of(1, 2, 3), reviews);
    }
}
