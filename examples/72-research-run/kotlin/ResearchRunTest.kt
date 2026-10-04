import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResearchRunTest {
    private val narrow = listOf("AI in digital art", "AI in graphic design", "AI in photography").map { Task("visual arts", it) }
    private val filmDown = listOf("AI in film", "AI in film production")

    @Test
    fun theCoordinatorFindsTheScopesItsPlanLeavesOutAndAddsThemOnce() {
        assertEquals(listOf("visual arts") to listOf("music", "writing", "film"), coverage(narrow))
        val plan = replan(narrow)
        assertEquals(REQUIRED to listOf<String>(), coverage(plan))
        assertEquals(6, plan.size)
        assertEquals(plan, replan(plan))
    }

    @Test
    fun aFailedSearchReturnsItsTypeQueryPartialResultsAndAlternatives() {
        val error = search("AI in film").error!!
        assertEquals("timeout", error.type)
        assertEquals("AI in film", error.query)
        assertEquals(listOf<Finding>(), error.partial)
        assertEquals(listOf("AI in film production"), error.alternatives)
    }

    @Test
    fun oneRetryUsesTheAlternativeAndASecondFailureKeepsBothQueries() {
        val done = recover(search("AI in film"), listOf())
        assertEquals("ok", done.status)
        assertEquals("AI in film", done.recoveredFrom)
        val still = recover(search("AI in film", filmDown), filmDown)
        assertEquals("error", still.status)
        assertEquals(filmDown, still.error!!.tried)
    }

    @Test
    fun aFailureWithNoAlternativeIsReturnedAsItIsAndNeverAsSuccess() {
        val result = recover(search("AI in music", listOf("AI in music")), listOf("AI in music"))
        assertEquals("error", result.status)
        assertEquals(listOf<String>(), result.error!!.alternatives)
    }

    @Test
    fun theReportIsPartialWheneverAScopeIsNotCoveredAndSaysWhich() {
        val plan = replan(narrow)
        assertEquals("complete", report(research(plan)).status)
        val partial = report(research(plan, filmDown))
        assertEquals("partial", partial.status)
        assertEquals(3, partial.covered)
        assertEquals(1, partial.errors)
        assertEquals(listOf("film not covered: timeout on 'AI in film' and on 'AI in film production'"), partial.notes)
        assertEquals("partial", report(research(narrow)).status)
    }

    @Test
    fun theScopedVerificationToolChecksDatesHereAndSendsTheRestBack() {
        assertEquals("confirmed", verifyFact("date", "survey-a", "2025-02-01"))
        assertEquals("mismatch", verifyFact("date", "survey-a", "2024-01-01"))
        assertEquals("needs_search", verifyFact("statistic", "survey-a", "60%"))
    }
}
