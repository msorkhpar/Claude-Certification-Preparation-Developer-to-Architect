import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ErrorFlowTest {
    private fun <T : Any> got(value: T?): T {
        assertNotNull(value, "the function returned nothing")
        return value!!
    }

    private fun ok(vararg items: String) = Reply("ok", items = items.toList())

    private fun err(type: String, vararg partial: String) = Reply("error", type, partial = partial.toList())

    private class Script(private vararg val replies: Reply) {
        val calls = mutableListOf<String>()
        val call: (String, Int) -> Reply = { query, attempt ->
            calls += "$query#$attempt"
            replies[minOf(attempt, replies.size) - 1]
        }
    }

    private fun success(attempts: Int, vararg items: String) = Outcome("success", items.toList(), attempts)

    private fun empty() = Outcome("empty", emptyList(), 1)

    private fun failed(kind: String, attempts: Int, partial: List<String>, alternatives: List<String>, query: String) = Outcome("failed", emptyList(), attempts, kind, query, partial, alternatives)

    @Test
    fun m1_aTransientFailureIsRetriedLocallyAndTheSuccessIsReportedWithItsAttempts() {
        val s = Script(err("timeout"), ok("a", "b"))
        assertEquals(success(2, "a", "b"), searchWithRecovery("q", call = s.call))
        assertEquals(listOf("q#1", "q#2"), s.calls)
    }

    @Test
    fun e1_aValidEmptyResultIsASuccessWithNoFindingsAndNeverAnError() {
        val s = Script(ok())
        assertEquals(empty(), searchWithRecovery("q", call = s.call))
        assertEquals(1, s.calls.size)
    }

    @Test
    fun e2_aPermissionOrInvalidQueryErrorIsNotRetriedAndCarriesWhatWasAttemptedAndItsAlternatives() {
        val s = Script(err("permission"), ok("never reached"))
        assertEquals(failed("permission", 1, emptyList(), listOf("request access", "use a public source"), "q"), searchWithRecovery("q", call = s.call))
        assertEquals(1, s.calls.size)
        assertEquals(failed("invalid_query", 1, emptyList(), listOf("rewrite the query"), "q"), searchWithRecovery("q", call = Script(err("invalid_query")).call))
        assertEquals(emptyList<String>(), got(searchWithRecovery("q", call = Script(err("weird")).call)).alternatives)
    }

    @Test
    fun e3_aFailureThatSurvivesTheRetriesCarriesThePartialResultsOfTheLastAttempt() {
        val s = Script(err("timeout", "x"), err("timeout", "x", "y"))
        assertEquals(failed("timeout", 2, listOf("x", "y"), listOf("retry later", "try a narrower query"), "q"), searchWithRecovery("q", call = s.call))
        val t = Script(err("unavailable"))
        assertEquals(3, got(searchWithRecovery("q", 3, t.call)).attempts)
        assertEquals(3, t.calls.size)
    }

    @Test
    fun e4_theCoordinatorUsesPartialResultsTriesAnAlternativeOrFlagsAGapAndNeverStopsTheRun() {
        val results = linkedMapOf("a" to success(1, "x"), "b" to empty(), "c" to failed("timeout", 2, listOf("p"), listOf("retry later"), "q"),
            "d" to failed("unavailable", 2, emptyList(), listOf("use a cached source"), "q"), "e" to failed("weird", 1, emptyList(), emptyList(), "q"))
        assertEquals(listOf(Step("a", "use"), Step("b", "no_findings"), Step("c", "use_partial"), Step("d", "try_alternative"), Step("e", "flag_gap")), coordinatorPlan(results))
        assertEquals(emptyList<Step>(), coordinatorPlan(emptyMap()))
    }

    @Test
    fun e5_theCoverageNoteSeparatesSupportedTopicsFromGapsAndNamesTheCause() {
        val results = linkedMapOf("news" to success(1, "x"), "patents" to empty(), "papers" to failed("timeout", 2, listOf("p"), emptyList(), "q-papers"), "filings" to failed("permission", 1, emptyList(), emptyList(), "q-filings"))
        assertEquals("Well-supported: news\nPartial: papers (timeout)\nNo findings: patents\nGaps: filings (permission: q-filings)", coverageNote(results, listOf("news", "papers", "patents", "filings")))
        assertEquals("Well-supported: news", coverageNote(mapOf("news" to success(1, "x")), listOf("news")))
    }

    @Test
    fun e6_aTopicWithNoResultIsAGapThatWasNotSearched() {
        assertEquals("Well-supported: news\nGaps: blogs (not searched)", coverageNote(mapOf("news" to success(1, "x")), listOf("news", "blogs")))
        assertEquals("", coverageNote(emptyMap(), emptyList()))
    }
}
