import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SynthesisTest {
    private static Synthesis.Finding f(String claim, String value, String source) {
        return new Synthesis.Finding(claim, value, source, "2025-01-01");
    }

    private static Synthesis.Finding f(String claim, String value, String source, String date) {
        return new Synthesis.Finding(claim, value, source, date);
    }

    private static Synthesis.Result ok(String scope, Synthesis.Finding... findings) {
        return new Synthesis.Result(scope, "ok", List.of(findings), null);
    }

    private static Synthesis.Result err(String scope, String query, List<Synthesis.Finding> partial, List<String> alternatives) {
        return new Synthesis.Result(scope, "error", List.of(), new Synthesis.Failure("timeout", query, partial, alternatives));
    }

    private static Synthesis.Source src(String source) {
        return new Synthesis.Source(source, "2025-01-01");
    }

    @Test
    void m1_aFullRunWithAgreementReturnsClaimsWithEverySourceAndACompleteStatus() {
        var report = Synthesis.synthesize(List.of("a", "b"), List.of(ok("a", f("X", "1", "s1"), f("Y", "2", "s2")), ok("b", f("X", "1", "s3"))));
        assertEquals(new Synthesis.Report("complete", List.of("a", "b"), List.of(), List.of(),
            List.of(new Synthesis.Claim("X", "1", List.of(src("s1"), src("s3")), false), new Synthesis.Claim("Y", "2", List.of(src("s2")), false)), List.of(), List.of(), "all scopes covered"), report);
    }

    @Test
    void e1_noResultsLeaveEveryScopeAGapAndSayNotResearched() {
        var report = Synthesis.synthesize(List.of("a", "b"), List.of());
        assertEquals("partial", report.status());
        assertEquals(List.of(), report.covered());
        assertEquals(List.of("a", "b"), report.gaps());
        assertEquals(List.of(), report.claims());
        assertEquals("not covered: a (not researched), b (not researched)", report.note());
    }

    @Test
    void e2_aScopeThePlanNeverCoveredAndAScopeWhoseSearchFailedReadDifferentlyInTheNote() {
        var report = Synthesis.synthesize(List.of("a", "b", "c"), List.of(ok("a", f("X", "1", "s1")), err("b", "query b", List.of(), List.of("query b2"))));
        assertEquals("partial", report.status());
        assertEquals(List.of("b", "c"), report.gaps());
        assertEquals("not covered: b (timeout on 'query b'), c (not researched)", report.note());
        assertEquals(List.of(new Synthesis.Unresolved("b", "timeout", "query b", List.of("query b2"))), report.errors());
    }

    @Test
    void e3_twoValuesForOneClaimAreAConflictThatNamesBothSourcesAndNoClaim() {
        var report = Synthesis.synthesize(List.of("a", "b"), List.of(ok("a", f("X", "12", "s1", "2024-03-01")), ok("b", f("X", "14", "s2", "2025-01-15"), f("X", "14", "s2", "2025-01-15"))));
        assertEquals(List.of(), report.claims());
        assertEquals(List.of(new Synthesis.Conflict("X", List.of(new Synthesis.Observed("12", "s1", "2024-03-01"), new Synthesis.Observed("14", "s2", "2025-01-15")))), report.conflicts());
        assertEquals("complete", report.status());
    }

    @Test
    void e4_partialResultsAreKeptAndFlaggedButDoNotCoverTheScopeAndARetryThatWorkedClearsTheError() {
        var report = Synthesis.synthesize(List.of("a", "b"), List.of(ok("a", f("X", "1", "s1")), err("b", "qb", List.of(f("Z", "9", "s9")), List.of("qb2"))));
        assertEquals(List.of(new Synthesis.Claim("X", "1", List.of(src("s1")), false), new Synthesis.Claim("Z", "9", List.of(src("s9")), true)), report.claims());
        assertEquals(List.of("a"), report.covered());
        assertEquals(List.of("b"), report.gaps());
        assertEquals(List.of("b"), report.partial());
        var healed = Synthesis.synthesize(List.of("a", "b"), List.of(ok("a", f("X", "1", "s1")), err("b", "qb", List.of(), List.of("qb2")), ok("b", f("Z", "9", "s9"))));
        assertEquals(List.of(), healed.errors());
        assertEquals("complete", healed.status());
        assertEquals(List.of("a", "b"), healed.covered());
        var mixed = Synthesis.synthesize(List.of("a"), List.of(err("a", "qa", List.of(f("X", "1", "s1")), List.of()), ok("a", f("X", "1", "s2"))));
        assertFalse(mixed.claims().get(0).partial());
    }

    @Test
    void e5_aResultWithNoFindingsDoesNotCoverItsScope() {
        var report = Synthesis.synthesize(List.of("a", "b"), List.of(ok("a", f("X", "1", "s1")), ok("b")));
        assertEquals(List.of("a"), report.covered());
        assertEquals(List.of("b"), report.gaps());
        assertEquals("partial", report.status());
        assertEquals("not covered: b (no findings)", report.note());
    }

    @Test
    void e6_claimsSourcesAndScopesComeOutInAFixedOrderWithoutDuplicates() {
        var report = Synthesis.synthesize(List.of("b", "a"), List.of(ok("a", f("Y", "2", "s2"), f("X", "1", "s9")), ok("b", f("X", "1", "s1"), f("X", "1", "s9"))));
        assertEquals(List.of("b", "a"), report.covered());
        assertEquals(List.of("X", "Y"), report.claims().stream().map(Synthesis.Claim::claim).toList());
        assertEquals(List.of(src("s9"), src("s1")), report.claims().get(0).sources());
    }
}
