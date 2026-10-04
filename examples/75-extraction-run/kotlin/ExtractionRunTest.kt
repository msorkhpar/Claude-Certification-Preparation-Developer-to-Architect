import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtractionRunTest {
    private fun run(doc: String) = extract(doc, DOCS.getValue(doc).text)

    @Test
    fun aRecordThatIsFineIsValidOnTheFirstAttempt() {
        val result = run("d1")
        assertEquals("valid", result.status)
        assertEquals(1, result.attempts)
        assertEquals(listOf<String>(), result.retried)
    }

    @Test
    fun aSemanticErrorAndAnInventedVendorAreRetriedOnceAndThenFixed() {
        assertEquals(listOf(Err("semantic", "total")), validate(REPLIES.getValue("d2")[0], DOCS.getValue("d2").text))
        assertEquals(listOf(Err("ungrounded", "vendor")), validate(REPLIES.getValue("d3")[0], DOCS.getValue("d3").text))
        for (doc in listOf("d2", "d3")) {
            val result = run(doc)
            assertEquals("valid", result.status)
            assertEquals(2, result.attempts)
        }
    }

    @Test
    fun anAbsentValueIsNeverRetriedAndGoesToReview() {
        val result = run("d4")
        assertEquals("needs_review", result.status)
        assertEquals(1, result.attempts)
        assertEquals(listOf(Err("absent", "total")), result.errors)
    }

    @Test
    fun aFlaggedConflictIsInformationAndGoesToReviewWithoutARetry() {
        val result = run("d5")
        assertEquals(listOf<Err>(), validate(result.record, DOCS.getValue("d5").text))
        assertEquals("needs_review", result.status)
        assertEquals(1, result.attempts)
    }

    @Test
    fun anErrorThatSurvivesTheRetryFailsTheDocumentAfterTwoAttempts() {
        val result = run("d6")
        assertEquals("failed", result.status)
        assertEquals(2, result.attempts)
        assertEquals(listOf("ungrounded"), result.retried)
    }
}
