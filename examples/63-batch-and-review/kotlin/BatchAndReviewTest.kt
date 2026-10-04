import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BatchAndReviewTest {
    @Test
    fun theWorstWaitIsAnIntervalPlusTheWindowPlusTheHandling() {
        assertEquals(listOf(30, 32, 25), listOf(worstCaseWait(4), worstCaseWait(6), worstCaseWait(1, 24, 0)))
    }

    @Test
    fun aCustomIdIsOneToSixtyFourLettersDigitsHyphensOrUnderscores() {
        assertEquals("invoice-0042_a", batchEntry("invoice-0042_a", emptyMap()).customId)
        for (bad in listOf("", "has space", "dot.dot", "x".repeat(65))) assertThrows(IllegalArgumentException::class.java) { batchEntry(bad, emptyMap()) }
        assertEquals("x".repeat(64), batchEntry("x".repeat(64), emptyMap()).customId)
    }

    @Test
    fun streamSpeedAndAZeroMaxTokensAreRefused() {
        for (params in listOf(mapOf<String, Any>("stream" to true), mapOf("speed" to "fast"), mapOf("max_tokens" to 0))) {
            assertThrows(IllegalArgumentException::class.java) { batchEntry("a1", params) }
        }
        assertEquals(mapOf("stream" to false, "max_tokens" to 10), batchEntry("a1", mapOf("stream" to false, "max_tokens" to 10)).params)
    }

    @Test
    fun resultsArePairedByCustomIdWhateverTheirOrder() {
        val p = matchResults(listOf("a1", "a2", "a3"), listOf(Matched("a2", "expired"), Matched("z9", "succeeded"), Matched("a1", "succeeded")))
        assertEquals(listOf(Matched("a1", "succeeded"), Matched("a2", "expired"), Matched("a3", "missing")), p.matched)
        assertEquals(listOf("z9"), p.unrequested)
    }

    @Test
    fun anIndependentReviewRequestLeavesTheGeneratorsReasoningOut() {
        assertTrue("because" in reviewRequest("code", "because", false))
        val independent = reviewRequest("code", "because", true)
        assertTrue("because" !in independent && "<code>code</code>" in independent)
    }
}
