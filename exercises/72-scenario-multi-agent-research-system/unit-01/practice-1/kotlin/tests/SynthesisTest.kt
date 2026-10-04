import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SynthesisTest {
    private fun f(claim: String, value: String, source: String, date: String = "2025-01-01") = Finding(claim, value, source, date)

    private fun ok(scope: String, vararg findings: Finding) = Result(scope, "ok", findings.toList(), null)

    private fun err(scope: String, query: String, partial: List<Finding> = listOf(), alternatives: List<String> = listOf()) =
        Result(scope, "error", listOf(), Failure("timeout", query, partial, alternatives))

    private fun src(source: String) = Source(source, "2025-01-01")

    @Test
    fun m1_aFullRunWithAgreementReturnsClaimsWithEverySourceAndACompleteStatus() {
        val report = synthesize(listOf("a", "b"), listOf(ok("a", f("X", "1", "s1"), f("Y", "2", "s2")), ok("b", f("X", "1", "s3"))))
        assertEquals(Report("complete", listOf("a", "b"), listOf(), listOf(),
            listOf(Claim("X", "1", listOf(src("s1"), src("s3")), false), Claim("Y", "2", listOf(src("s2")), false)), listOf(), listOf(), "all scopes covered"), report)
    }

    @Test
    fun e1_noResultsLeaveEveryScopeAGapAndSayNotResearched() {
        val report = synthesize(listOf("a", "b"), listOf())
        assertEquals("partial", report.status)
        assertEquals(listOf<String>(), report.covered)
        assertEquals(listOf("a", "b"), report.gaps)
        assertEquals(listOf<Claim>(), report.claims)
        assertEquals("not covered: a (not researched), b (not researched)", report.note)
    }

    @Test
    fun e2_aScopeThePlanNeverCoveredAndAScopeWhoseSearchFailedReadDifferentlyInTheNote() {
        val report = synthesize(listOf("a", "b", "c"), listOf(ok("a", f("X", "1", "s1")), err("b", "query b", alternatives = listOf("query b2"))))
        assertEquals("partial", report.status)
        assertEquals(listOf("b", "c"), report.gaps)
        assertEquals("not covered: b (timeout on 'query b'), c (not researched)", report.note)
        assertEquals(listOf(Unresolved("b", "timeout", "query b", listOf("query b2"))), report.errors)
    }

    @Test
    fun e3_twoValuesForOneClaimAreAConflictThatNamesBothSourcesAndNoClaim() {
        val report = synthesize(listOf("a", "b"), listOf(ok("a", f("X", "12", "s1", "2024-03-01")), ok("b", f("X", "14", "s2", "2025-01-15"), f("X", "14", "s2", "2025-01-15"))))
        assertEquals(listOf<Claim>(), report.claims)
        assertEquals(listOf(Conflict("X", listOf(Observed("12", "s1", "2024-03-01"), Observed("14", "s2", "2025-01-15")))), report.conflicts)
        assertEquals("complete", report.status)
    }

    @Test
    fun e4_partialResultsAreKeptAndFlaggedButDoNotCoverTheScopeAndARetryThatWorkedClearsTheError() {
        val report = synthesize(listOf("a", "b"), listOf(ok("a", f("X", "1", "s1")), err("b", "qb", listOf(f("Z", "9", "s9")), listOf("qb2"))))
        assertEquals(listOf(Claim("X", "1", listOf(src("s1")), false), Claim("Z", "9", listOf(src("s9")), true)), report.claims)
        assertEquals(listOf("a"), report.covered)
        assertEquals(listOf("b"), report.gaps)
        assertEquals(listOf("b"), report.partial)
        val healed = synthesize(listOf("a", "b"), listOf(ok("a", f("X", "1", "s1")), err("b", "qb", alternatives = listOf("qb2")), ok("b", f("Z", "9", "s9"))))
        assertEquals(listOf<Unresolved>(), healed.errors)
        assertEquals("complete", healed.status)
        assertEquals(listOf("a", "b"), healed.covered)
        val mixed = synthesize(listOf("a"), listOf(err("a", "qa", listOf(f("X", "1", "s1"))), ok("a", f("X", "1", "s2"))))
        assertFalse(mixed.claims[0].partial)
    }

    @Test
    fun e5_aResultWithNoFindingsDoesNotCoverItsScope() {
        val report = synthesize(listOf("a", "b"), listOf(ok("a", f("X", "1", "s1")), ok("b")))
        assertEquals(listOf("a"), report.covered)
        assertEquals(listOf("b"), report.gaps)
        assertEquals("partial", report.status)
        assertEquals("not covered: b (no findings)", report.note)
    }

    @Test
    fun e6_claimsSourcesAndScopesComeOutInAFixedOrderWithoutDuplicates() {
        val report = synthesize(listOf("b", "a"), listOf(ok("a", f("Y", "2", "s2"), f("X", "1", "s9")), ok("b", f("X", "1", "s1"), f("X", "1", "s9"))))
        assertEquals(listOf("b", "a"), report.covered)
        assertEquals(listOf("X", "Y"), report.claims.map { it.claim })
        assertEquals(listOf(src("s9"), src("s1")), report.claims[0].sources)
    }
}
