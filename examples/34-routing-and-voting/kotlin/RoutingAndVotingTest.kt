import java.time.Duration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RoutingAndVotingTest {
    private val desk = listOf("billing specialist" to "I see two charges.", "front desk" to "Open 9 to 5.")

    @Test
    fun aLabelPicksTheModelAndAnUnknownLabelTakesTheDefault() {
        val rig = scripted(listOf("Classify" to "Billing.") + desk, 2)
        val billing = route(rig.client(), "charged twice").join()
        assertEquals(Triple("billing", STRONG, false), Triple(billing.label, billing.model, billing.fallback))
        assertEquals(CHEAP, rig.http().requests[0]["model"].asText())
        val odd = route(scripted(listOf("Classify" to "refunds?") + desk, 2).client(), "hello").join()
        assertEquals(Routed("refunds?", true, CHEAP, "Open 9 to 5."), odd)
    }

    @Test
    fun sectioningRunsBothCallsTogetherAndDropsTheAnswerWhenTheScreenBlocks() {
        val rig = scripted(listOf("Answer the question" to "Fine.", "Screen the question" to "block"), 2, Duration.ofMillis(20))
        assertEquals(Guarded("block", null), guarded(rig.client(), "q").join())
        assertEquals(2, rig.http().maxInFlight())
    }

    @Test
    fun votingCountsTheReviewsAgainstTheThreshold() {
        val two = disagreeing(Duration.ofMillis(10))
        val flagged = vote(two.client(), "code", 2).join()
        val notFlagged = vote(disagreeing(Duration.ofMillis(10)).client(), "code", 3).join()
        assertTrue(flagged.flagged)
        assertFalse(notFlagged.flagged)
        assertEquals(3, two.http().maxInFlight())
        assertEquals(mapOf("SAFE" to 1, "VULNERABLE" to 2), flagged.votes)
    }
}
