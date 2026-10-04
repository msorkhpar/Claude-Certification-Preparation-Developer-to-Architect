import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReviewSpecTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> crit(Object... over) {
        Map<String, Object> c = map("id", "bug", "report", "A comment whose claimed behaviour contradicts what the code does.", "skip", "Minor style, naming and patterns the codebase already uses.",
                "severity", map("high", "A null dereference on a request path, such as user.profile.name when user may be None.", "low", "A misleading variable name."));
        for (int i = 0; i < over.length; i += 2) c.put((String) over[i], over[i + 1]);
        return c;
    }

    private static final Map<String, Object> REPORT = map("verdict", "report", "category", "bug", "code", "total = price * qty  # sum of the line items",
            "reason", "The comment says the line sums items, the code multiplies one price by a quantity.");
    private static final Map<String, Object> SKIP = map("verdict", "skip", "code", "for i in range(n):  # loop", "reason", "The comment is terse and accurate; at most a style matter.");

    private static Map<String, Object> with(Map<String, Object> base, Object... over) {
        Map<String, Object> m = new LinkedHashMap<>(base);
        for (int i = 0; i < over.length; i += 2) m.put((String) over[i], over[i + 1]);
        return m;
    }

    private static Map<String, Object> spec(List<Map<String, Object>> criteria, List<Map<String, Object>> examples) {
        return map("criteria", criteria, "examples", examples);
    }

    private static String refused(Map<String, Object> spec) {
        try {
            ReviewSpec.buildReviewPrompt(spec, "+ x = 1");
        } catch (IllegalArgumentException error) {
            return error.getMessage();
        }
        return fail("the specification was accepted: " + spec);
    }

    private static String built(Map<String, Object> spec) {
        String result = ReviewSpec.buildReviewPrompt(spec, "+ x = 1");
        assertNotNull(result, "buildReviewPrompt returned nothing");
        return result;
    }

    @Test
    void m1_thePromptPutsCriteriaFirstThenExamplesThenTheDiffLast() {
        String expected = String.join("\n", "<criteria>", "<criterion id=\"bug\">", "Report: A comment whose claimed behaviour contradicts what the code does.",
                "Skip: Minor style, naming and patterns the codebase already uses.", "Severity high: A null dereference on a request path, such as user.profile.name when user may be None.",
                "Severity low: A misleading variable name.", "</criterion>", "</criteria>", "<examples>", "<example verdict=\"report\" category=\"bug\">",
                "<code>total = price * qty  # sum of the line items</code>", "<reason>The comment says the line sums items, the code multiplies one price by a quantity.</reason>", "</example>",
                "<example verdict=\"skip\">", "<code>for i in range(n):  # loop</code>", "<reason>The comment is terse and accurate; at most a style matter.</reason>", "</example>",
                "</examples>", "<diff>", "+ x = 1", "</diff>");
        assertEquals(expected, built(spec(List.of(crit()), List.of(REPORT, SKIP))));
    }

    @Test
    void e1_vagueCriteriaAreRefusedInBothTheReportAndTheSkipText() {
        for (String phrase : List.of("Be conservative and flag only what matters.", "Only report high-confidence findings.", "Report it when you are sure.", "Flag only important problems.", "Use your judgment about what matters.")) {
            for (String key : List.of("report", "skip")) {
                String message = refused(spec(List.of(crit(key, phrase)), List.of(REPORT, SKIP)));
                assertTrue(message.contains("vague"), key + ": '" + phrase + "' was refused for another reason: " + message);
            }
        }
    }

    @Test
    void e2_aCriterionNeedsReportSkipAndAConcreteSeverityExampleForHighAndLow() {
        refused(spec(List.of(), List.of(REPORT, SKIP)));
        List<Map<String, Object>> bad = List.of(crit("report", ""), crit("skip", "  "), crit("skip", null), crit("severity", map("high", "A null dereference.")), crit("severity", map("low", "A misleading name.")),
                crit("severity", map("high", "x", "low", " ")), crit("severity", null));
        for (Map<String, Object> c : bad) refused(spec(List.of(c), List.of(REPORT, SKIP)));
        assertTrue(built(spec(List.of(crit()), List.of(REPORT, SKIP))).contains("<criterion id=\"bug\">"));
    }

    @Test
    void e3_twoToFourExamplesWithAReportAndASkipEachCarryingAReason() {
        List<Map<String, Object>> criteria = List.of(crit());
        refused(spec(criteria, List.of(REPORT)));
        refused(spec(criteria, List.of(REPORT, SKIP, REPORT, SKIP, REPORT)));
        refused(spec(criteria, List.of(REPORT, with(REPORT))));
        refused(spec(criteria, List.of(SKIP, with(SKIP))));
        refused(spec(criteria, List.of(REPORT, with(SKIP, "verdict", "maybe"))));
        refused(spec(criteria, List.of(REPORT, with(SKIP, "reason", " "))));
        refused(spec(criteria, List.of(with(REPORT, "category", "performance"), SKIP)));
        List<Map<String, Object>> all = List.of(REPORT, SKIP, REPORT, SKIP);
        for (int count = 2; count <= 4; count++) {
            String prompt = built(spec(criteria, all.subList(0, count)));
            assertEquals(count, prompt.split("<example ", -1).length - 1);
        }
    }

    private static List<Map<String, Object>> findings(String category, int accepted, int dismissed, String pattern) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < accepted; i++) out.add(map("category", category, "verdict", "accepted", "detected_pattern", pattern));
        for (int i = 0; i < dismissed; i++) out.add(map("category", category, "verdict", "dismissed", "detected_pattern", pattern));
        return out;
    }

    @SafeVarargs
    private static List<Map<String, Object>> join(List<Map<String, Object>>... parts) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (List<Map<String, Object>> p : parts) out.addAll(p);
        return out;
    }

    private static Map<String, Object> report(List<Map<String, Object>> data) {
        Map<String, Object> result = ReviewSpec.categoryReport(data);
        assertNotNull(result, "categoryReport returned nothing");
        return result;
    }

    @SuppressWarnings("unchecked")
    @Test
    void e4_aCategoryWithEnoughReviewsAndLowPrecisionIsDisabled() {
        List<Map<String, Object>> data = join(findings("bug", 8, 2, "p"), findings("style", 2, 4, "p"), findings("naming", 0, 4, "p"), findings("docs", 3, 3, "p"));
        Map<String, Object> report = report(data);
        assertEquals(List.of("style"), report.get("disable"));
        Map<String, Object> brief = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : ((Map<String, Object>) report.get("categories")).entrySet()) {
            Map<String, Object> v = (Map<String, Object>) e.getValue();
            brief.put(e.getKey(), List.of(v.get("reviewed"), v.get("precision"), v.get("disable")));
        }
        assertEquals(map("bug", List.of(10, 0.8, false), "style", List.of(6, 0.33, true), "naming", List.of(4, 0.0, false), "docs", List.of(6, 0.5, false)), brief);
        assertEquals(List.of("naming", "style"), ReviewSpec.categoryReport(data, 4, 0.5).get("disable"));
        assertEquals(List.of("bug", "docs", "style"), ReviewSpec.categoryReport(data, 5, 0.9).get("disable"));
        assertEquals(map("categories", map(), "disable", List.of()), report(List.of()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void e5_theMostDismissedPatternsAreListedByCountThenNameAndCappedAtThree() {
        List<Map<String, Object>> data = join(findings("style", 0, 3, "line-length"), findings("style", 0, 2, "import-order"), findings("style", 0, 1, "quote-style"),
                findings("style", 0, 1, "brace-style"), findings("style", 5, 0, "accepted-only"));
        Map<String, Object> style = (Map<String, Object>) ((Map<String, Object>) report(data).get("categories")).get("style");
        assertEquals(List.of(List.of("line-length", 3), List.of("import-order", 2), List.of("brace-style", 1)), style.get("top_dismissed"));
        Map<String, Object> bug = (Map<String, Object>) ((Map<String, Object>) report(findings("bug", 3, 0, "p")).get("categories")).get("bug");
        assertEquals(List.of(), bug.get("top_dismissed"));
    }

    private static final List<String> REQUIRED = List.of("repo", "branch", "reviewer");

    private static Map<String, Object> step(Map<String, Object> request, Map<String, String> defaults, boolean attended) {
        Map<String, Object> result = ReviewSpec.nextStep(request, REQUIRED, defaults, attended);
        assertNotNull(result, "nextStep returned nothing");
        return result;
    }

    @Test
    void e6_anAttendedRunAsksOnlyWhatItCannotAssumeAndStatesItsAssumptions() {
        Map<String, Object> request = map("repo", "api", "branch", "", "reviewer", null);
        assertEquals(map("action", "ask", "ask", List.of("reviewer"), "assumptions", map("branch", "main")), step(request, Map.of("branch", "main"), true));
        assertEquals(map("action", "proceed", "ask", List.of(), "assumptions", map("branch", "main")), step(map("repo", "api", "branch", " ", "reviewer", "ana"), Map.of("branch", "main"), true));
        assertEquals(map("action", "proceed", "ask", List.of(), "assumptions", map()), step(map("repo", "api", "branch", "dev", "reviewer", "ana"), Map.of("branch", "main"), true));
        assertEquals(map("action", "ask", "ask", List.of("repo", "branch", "reviewer"), "assumptions", map()), step(map(), Map.of(), true));
    }

    @Test
    void e7_anUnattendedRunNeverAsksItStatesAssumptionsOrStops() {
        Map<String, Object> request = map("repo", "api", "branch", "", "reviewer", null);
        assertEquals(map("action", "stop", "ask", List.of(), "assumptions", map("branch", "main")), step(request, Map.of("branch", "main"), false));
        assertEquals(map("action", "proceed", "ask", List.of(), "assumptions", map("branch", "main")), step(map("repo", "api", "branch", "", "reviewer", "ana"), Map.of("branch", "main"), false));
        assertEquals(map("action", "proceed", "ask", List.of(), "assumptions", map()), step(map("repo", "api", "branch", "dev", "reviewer", "ana"), Map.of(), false));
    }
}
