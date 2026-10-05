import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ControlChainTest {
    private val source = "Water damage is covered up to 5,000 per claim."
    private val reply = Action("draft_reply", "low")
    private val refund = Action("issue_refund", "high")

    private fun answer(confidence: Int, quote: String) = Answer("ok", confidence, quote)

    @Test
    fun aDownScreenHoldsAHighConsequenceActionAndFlagsALowOne() {
        assertEquals("hold: screen down", route(refund, answer(99, source), source, false))
        assertEquals("auto (unscreened)", route(reply, answer(99, source), source, false))
        assertEquals("auto", route(reply, answer(99, source), source, true))
    }

    @Test
    fun aConfidentAnswerThatTheSourceDoesNotSupportIsHeld() {
        val wrong = answer(100, "Water damage is covered up to 8,000 per claim.")
        assertEquals("hold: unsupported", route(reply, wrong, source, true))
        assertEquals("hold: unsupported", route(refund, wrong, source, true))
    }

    @Test
    fun confidenceExactlyAtTheThresholdGoesOutAndOneBelowIsReviewed() {
        assertEquals("auto", route(reply, answer(95, source), source, true))
        assertEquals("review", route(reply, answer(94, source), source, true))
    }

    @Test
    fun aHighConsequenceActionAlwaysReachesAPerson() {
        assertEquals("human", route(refund, answer(100, source), source, true))
    }

    @Test
    fun theAuditRecordHoldsNoContentAndErasureUnlinksOnlyThePerson() {
        val record = auditRecord("r-1", refund, "human", "secret text")
        assertEquals(11, record["chars"])
        assertEquals(false, record["content_stored"])
        assertFalse(record.toString().contains("secret text"))
        val erased = erase(mapOf("<A>" to "p1", "<B>" to "p2", "<C>" to "p1"), "p1")
        assertEquals(mapOf("<B>" to "p2"), erased.kept)
        assertEquals(2, erased.removed)
    }
}
