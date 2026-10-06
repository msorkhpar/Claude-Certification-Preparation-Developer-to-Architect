import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

class ErrorFlowTest {
    private static <T> T got(T value) {
        assertNotNull(value, "the method returned nothing");
        return value;
    }

    private static ErrorFlow.Reply ok(String... items) {
        return new ErrorFlow.Reply("ok", null, List.of(items), List.of());
    }

    private static ErrorFlow.Reply err(String type, String... partial) {
        return new ErrorFlow.Reply("error", type, List.of(), List.of(partial));
    }

    private static final class Script implements BiFunction<String, Integer, ErrorFlow.Reply> {
        final List<ErrorFlow.Reply> replies;
        final List<String> calls = new ArrayList<>();

        Script(ErrorFlow.Reply... replies) {
            this.replies = List.of(replies);
        }

        @Override
        public ErrorFlow.Reply apply(String query, Integer attempt) {
            calls.add(query + "#" + attempt);
            return replies.get(Math.min(attempt, replies.size()) - 1);
        }
    }

    private static ErrorFlow.Outcome success(int attempts, String... items) {
        return new ErrorFlow.Outcome("success", List.of(items), attempts, null, null, List.of(), List.of());
    }

    private static ErrorFlow.Outcome empty() {
        return new ErrorFlow.Outcome("empty", List.of(), 1, null, null, List.of(), List.of());
    }

    private static ErrorFlow.Outcome failed(String kind, int attempts, List<String> partial, List<String> alternatives, String query) {
        return new ErrorFlow.Outcome("failed", List.of(), attempts, kind, query, partial, alternatives);
    }

    private static Map<String, ErrorFlow.Outcome> ordered(Object... kv) {
        Map<String, ErrorFlow.Outcome> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (ErrorFlow.Outcome) kv[i + 1]);
        return m;
    }

    @Test
    void m1_aTransientFailureIsRetriedLocallyAndTheSuccessIsReportedWithItsAttempts() {
        Script s = new Script(err("timeout"), ok("a", "b"));
        assertEquals(success(2, "a", "b"), ErrorFlow.searchWithRecovery("q", s));
        assertEquals(List.of("q#1", "q#2"), s.calls);
    }

    @Test
    void e1_aValidEmptyResultIsASuccessWithNoFindingsAndNeverAnError() {
        Script s = new Script(ok());
        assertEquals(empty(), ErrorFlow.searchWithRecovery("q", s));
        assertEquals(1, s.calls.size());
    }

    @Test
    void e2_aPermissionOrInvalidQueryErrorIsNotRetriedAndCarriesWhatWasAttemptedAndItsAlternatives() {
        Script s = new Script(err("permission"), ok("never reached"));
        assertEquals(failed("permission", 1, List.of(), List.of("request access", "use a public source"), "q"), ErrorFlow.searchWithRecovery("q", s));
        assertEquals(1, s.calls.size());
        assertEquals(failed("invalid_query", 1, List.of(), List.of("rewrite the query"), "q"), ErrorFlow.searchWithRecovery("q", new Script(err("invalid_query"))));
        assertEquals(List.of(), got(ErrorFlow.searchWithRecovery("q", new Script(err("weird")))).alternatives());
    }

    @Test
    void e3_aFailureThatSurvivesTheRetriesCarriesThePartialResultsOfTheLastAttempt() {
        Script s = new Script(err("timeout", "x"), err("timeout", "x", "y"));
        assertEquals(failed("timeout", 2, List.of("x", "y"), List.of("retry later", "try a narrower query"), "q"), ErrorFlow.searchWithRecovery("q", s));
        Script t = new Script(err("unavailable"));
        assertEquals(3, got(ErrorFlow.searchWithRecovery("q", t, 3)).attempts());
        assertEquals(3, t.calls.size());
    }

    @Test
    void e4_theCoordinatorUsesPartialResultsTriesAnAlternativeOrFlagsAGapAndNeverStopsTheRun() {
        Map<String, ErrorFlow.Outcome> results = ordered("a", success(1, "x"), "b", empty(), "c", failed("timeout", 2, List.of("p"), List.of("retry later"), "q"),
            "d", failed("unavailable", 2, List.of(), List.of("use a cached source"), "q"), "e", failed("weird", 1, List.of(), List.of(), "q"));
        assertEquals(List.of(new ErrorFlow.Step("a", "use"), new ErrorFlow.Step("b", "no_findings"), new ErrorFlow.Step("c", "use_partial"), new ErrorFlow.Step("d", "try_alternative"), new ErrorFlow.Step("e", "flag_gap")),
            ErrorFlow.coordinatorPlan(results));
        assertEquals(List.of(), ErrorFlow.coordinatorPlan(Map.of()));
    }

    @Test
    void e5_theCoverageNoteSeparatesSupportedTopicsFromGapsAndNamesTheCause() {
        Map<String, ErrorFlow.Outcome> results = ordered("news", success(1, "x"), "patents", empty(), "papers", failed("timeout", 2, List.of("p"), List.of(), "q-papers"), "filings", failed("permission", 1, List.of(), List.of(), "q-filings"));
        assertEquals("Well-supported: news\nPartial: papers (timeout)\nNo findings: patents\nGaps: filings (permission: q-filings)", ErrorFlow.coverageNote(results, List.of("news", "papers", "patents", "filings")));
        assertEquals("Well-supported: news", ErrorFlow.coverageNote(ordered("news", success(1, "x")), List.of("news")));
    }

    @Test
    void e6_aTopicWithNoResultIsAGapThatWasNotSearched() {
        assertEquals("Well-supported: news\nGaps: blogs (not searched)", ErrorFlow.coverageNote(ordered("news", success(1, "x")), List.of("news", "blogs")));
        assertEquals("", ErrorFlow.coverageNote(Map.of(), List.of()));
    }
}
