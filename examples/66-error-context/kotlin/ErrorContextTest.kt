import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ErrorContextTest {
    @Test
    fun theGenericStatusLosesThePartialResultAndTheCause() {
        assertEquals("found n1, n2, b1; sources unavailable: papers, filings", generic(OUTCOMES))
    }

    @Test
    fun silentSuppressionReportsAFailedSourceAsASearchThatFoundNothing() {
        assertEquals("found n1, n2, b1; nothing found in: papers, patents, filings", suppress(OUTCOMES))
    }

    @Test
    fun abortingOnTheFirstFailureLosesEveryLaterSource() {
        assertEquals("aborted at papers; found n1, n2", terminate(OUTCOMES))
        assertEquals("found x", terminate(mapOf("a" to Outcome("ok", listOf("x")))))
    }

    @Test
    fun structuredContextKeepsThePartialResultTheEmptyAnswerAndTheWayForward() {
        assertEquals("well supported: news, blogs; partial: papers (timeout, kept p1); no findings: patents; gaps: filings (permission, try: request access)", structured(OUTCOMES))
    }
}
