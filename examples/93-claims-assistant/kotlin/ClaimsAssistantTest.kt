import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClaimsAssistantTest {
    private val water = "Water damage is covered up to 5,000 per claim."

    private fun request(text: String, consequence: String = "low", quote: String = water, confidence: Int = 97) = Request("r", text, setOf("policy"), consequence, quote, confidence)

    @Test
    fun identifiersBecomeTokensAndTheSameValueGetsTheSameToken() {
        val t = tokenise("write to a@example.com or a@example.com or b@example.org")
        assertEquals("write to <EMAIL_1> or <EMAIL_1> or <EMAIL_2>", t.sent)
        assertEquals("a@example.com", t.vault["<EMAIL_1>"])
        assertFalse(t.sent.contains("@"))
    }

    @Test
    fun theReadersRightsComeBeforeTheRanking() {
        assertEquals("contract-9", retrieve("partner commission premiums", setOf("contracts"), INDEX)?.id)
        assertNull(retrieve("partner commission premiums", setOf("policy"), INDEX))
    }

    @Test
    fun aTieGoesToTheSmallerIdAndNoOverlapIsNoEvidence() {
        assertEquals("policy-2-old", retrieve("water damage", setOf("policy"), STALE_INDEX)?.id)
        assertEquals("policy-2", retrieve("water damage", setOf("policy"), INDEX)?.id)
        assertNull(retrieve("zzzz yyyy", setOf("policy"), INDEX))
    }

    @Test
    fun aStaleOrUnsupportedAnswerIsHeldAndConfidenceDecidesTheRest() {
        val q = "How much does the policy cover for water damage?"
        assertEquals("hold: stale evidence (policy-2-old v2, current v3)", handle(request(q), STALE_INDEX).trace.outcome)
        assertEquals("hold: unsupported", handle(request(q, quote = "Water damage is covered up to 8,000 per claim."), INDEX).trace.outcome)
        assertEquals("auto", handle(request(q, confidence = 95), INDEX).trace.outcome)
        assertEquals("review", handle(request(q, confidence = 94), INDEX).trace.outcome)
        assertEquals("human", handle(request(q, consequence = "high"), INDEX).trace.outcome)
    }

    @Test
    fun theTraceHoldsIdsAndSizesAndNoText() {
        val h = handle(request("Claims reported from jo@example.com, how many days?", quote = "x"), INDEX)
        assertFalse(h.trace.toString().contains("jo@example.com") || h.sent.contains("@"))
    }

    @Test
    fun aGateProtectsTheCostlySegmentEvenWhenGainsCoverTheLosses() {
        val cases = listOf(Case("a", "refund", true, false), Case("b", "status", false, true))
        assertEquals("no-go: protected segment lost answers: refund", release(cases, setOf("refund")))
        assertEquals("go: lost 1, gained 1", release(cases, setOf()))
        assertEquals("no-go: net loss: lost 1, gained 0", release(listOf(Case("a", "x", true, false)), setOf()))
    }
}
