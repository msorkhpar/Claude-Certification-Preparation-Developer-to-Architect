import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ResearchRunTest {
    private static final List<ResearchRun.Task> NARROW = List.of("AI in digital art", "AI in graphic design", "AI in photography").stream().map(q -> new ResearchRun.Task("visual arts", q)).toList();
    private static final List<String> FILM_DOWN = List.of("AI in film", "AI in film production");

    @Test
    void theCoordinatorFindsTheScopesItsPlanLeavesOutAndAddsThemOnce() {
        assertEquals(List.of(List.of("visual arts"), List.of("music", "writing", "film")), ResearchRun.coverage(NARROW));
        var plan = ResearchRun.replan(NARROW);
        assertEquals(List.of(ResearchRun.REQUIRED, List.of()), ResearchRun.coverage(plan));
        assertEquals(6, plan.size());
        assertEquals(plan, ResearchRun.replan(plan));
    }

    @Test
    void aFailedSearchReturnsItsTypeQueryPartialResultsAndAlternatives() {
        var error = ResearchRun.search("AI in film").error();
        assertEquals("timeout", error.type());
        assertEquals("AI in film", error.query());
        assertEquals(List.of(), error.partial());
        assertEquals(List.of("AI in film production"), error.alternatives());
    }

    @Test
    void oneRetryUsesTheAlternativeAndASecondFailureKeepsBothQueries() {
        var done = ResearchRun.recover(ResearchRun.search("AI in film"), List.of());
        assertEquals("ok", done.status());
        assertEquals("AI in film", done.recoveredFrom());
        var still = ResearchRun.recover(ResearchRun.search("AI in film", FILM_DOWN), FILM_DOWN);
        assertEquals("error", still.status());
        assertEquals(FILM_DOWN, still.error().tried());
    }

    @Test
    void aFailureWithNoAlternativeIsReturnedAsItIsAndNeverAsSuccess() {
        var result = ResearchRun.recover(ResearchRun.search("AI in music", List.of("AI in music")), List.of("AI in music"));
        assertEquals("error", result.status());
        assertEquals(List.of(), result.error().alternatives());
    }

    @Test
    void theReportIsPartialWheneverAScopeIsNotCoveredAndSaysWhich() {
        var plan = ResearchRun.replan(NARROW);
        assertEquals("complete", ResearchRun.report(ResearchRun.research(plan, List.of())).status());
        var partial = ResearchRun.report(ResearchRun.research(plan, FILM_DOWN));
        assertEquals("partial", partial.status());
        assertEquals(3, partial.covered());
        assertEquals(1, partial.errors());
        assertEquals(List.of("film not covered: timeout on 'AI in film' and on 'AI in film production'"), partial.notes());
        assertEquals("partial", ResearchRun.report(ResearchRun.research(NARROW, List.of())).status());
    }

    @Test
    void theScopedVerificationToolChecksDatesHereAndSendsTheRestBack() {
        assertEquals("confirmed", ResearchRun.verifyFact("date", "survey-a", "2025-02-01"));
        assertEquals("mismatch", ResearchRun.verifyFact("date", "survey-a", "2024-01-01"));
        assertEquals("needs_search", ResearchRun.verifyFact("statistic", "survey-a", "60%"));
    }
}
