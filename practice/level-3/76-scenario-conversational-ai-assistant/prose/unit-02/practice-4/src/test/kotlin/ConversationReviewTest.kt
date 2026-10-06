import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConversationReviewTest {
    private val policy = Policy(12, 10, 80, 3)

    private fun conv(segment: String = "billing", turns: Int = 5, resolved: Boolean = true, handoff: String = "none", needed: Boolean = false, repeated: Boolean = false, risk: Boolean = false) =
        Conversation("c", segment, turns, resolved, handoff, needed, repeated, risk)

    private fun handed(segment: String = "billing") = conv(segment, resolved = false, handoff = "requested", needed = true)

    private fun safe() = conv("safety", resolved = false, handoff = "safety", needed = true, risk = true)

    private fun many(n: Int, c: Conversation) = List(n) { c }

    private fun seg(segment: String, n: Int, resolved: Int, percent: Int, weak: Boolean) = Segment(segment, n, resolved, percent, weak)

    @Test
    fun m1_aMixedBatchGetsEveryCountTheSegmentsAndAVerdict() {
        val batch = many(3, conv()) + listOf(handed(), conv("smalltalk"), conv("smalltalk", turns = 15), safe(), conv(repeated = true))
        assertEquals(Report(8, 6, 75, 0, 1, 13, false, 0, 0, listOf(seg("billing", 5, 4, 80, false), seg("safety", 1, 0, 0, false), seg("smalltalk", 2, 2, 100, false)), "hold", "repeats"),
            review(batch, policy))
    }

    @Test
    fun e1_anEmptyBatchHasZeroFiguresAndIsHeldForLackOfData() {
        assertEquals(Report(0, 0, 0, 0, 0, 0, true, 0, 0, listOf(), "hold", "no_data"), review(listOf(), policy))
    }

    @Test
    fun e2_aSafetySignalCountsAsMissedUnlessItWentToAPersonAsASafetyHandOff() {
        val batch = many(5, conv()) + listOf(conv("safety", resolved = false, handoff = "requested", needed = true, risk = true), conv("safety", resolved = false, risk = true), safe())
        val report = review(batch, policy)
        assertEquals(listOf(2, "hold", "safety"), listOf(report.safetyMissed, report.verdict, report.reason))
        assertEquals(0, review(many(5, conv()) + listOf(safe()), policy).safetyMissed)
    }

    @Test
    fun e3_aConversationIsOverlongOnlyAboveTheTurnLimitAndOnlyWhenNobodyTookOver() {
        assertEquals(0, review(listOf(conv(turns = 12)), policy).overlong)
        assertEquals(1, review(listOf(conv(turns = 13)), policy).overlong)
        assertEquals(0, review(listOf(conv(turns = 30, resolved = false, handoff = "stalled", needed = true)), policy).overlong)
    }

    @Test
    fun e4_repeatedQuestionsAreAcceptableAtExactlyTheLimitAndNotAboveIt() {
        val at = review(many(9, conv()) + many(1, conv(repeated = true)), policy)
        assertEquals(listOf(10, true, "ship"), listOf(at.repeatPct, at.repeatOk, at.verdict))
        val over = review(many(8, conv()) + many(2, conv(repeated = true)), policy)
        assertEquals(listOf(20, false, "hold", "repeats"), listOf(over.repeatPct, over.repeatOk, over.verdict, over.reason))
    }

    @Test
    fun e5_aSegmentIsWeakOnlyBelowTheResolutionFloorAndNotAtIt() {
        val at = review(many(8, conv()) + many(2, handed()), policy)
        assertEquals(listOf(seg("billing", 10, 8, 80, false)), at.segments)
        assertEquals("none", at.reason)
        val below = review(many(7, conv()) + many(3, handed()), policy)
        assertEquals(listOf(seg("billing", 10, 7, 70, true)), below.segments)
        assertEquals("weak_segment", below.reason)
    }

    @Test
    fun e6_aSegmentNeedsTheMinimumNumberOfConversationsBeforeItCanBeCalledWeak() {
        val two = review(many(2, handed("refunds")), policy)
        assertEquals(listOf(seg("refunds", 2, 0, 0, false)), two.segments)
        val three = review(many(3, handed("refunds")), policy)
        assertEquals(listOf(seg("refunds", 3, 0, 0, true)), three.segments)
        assertEquals("weak_segment", three.reason)
    }

    @Test
    fun e7_aHandOffIsOverEscalationOnlyWhenNoPersonWasNeededAndNoSafetySignalWasPresent() {
        val batch = listOf(conv(handoff = "requested"), handed(), conv(handoff = "safety", risk = true), conv(needed = true), conv(resolved = false, handoff = "stalled"))
        val report = review(batch, policy)
        assertEquals(listOf(2, 1), listOf(report.overEscalated, report.underEscalated))
    }

    @Test
    fun e8_onlyConversationsTheAssistantSettledAloneCountAsResolved() {
        val report = review(listOf(conv(), conv(handoff = "requested", needed = true), conv(resolved = false)), policy)
        assertEquals(listOf(1, 33), listOf(report.resolved, report.resolvedPct))
    }

    @Test
    fun e9_percentagesAreWholeNumbersRoundedHalfUp() {
        val report = review(many(1, conv()) + many(7, handed()), policy)
        assertEquals(listOf(13, 13), listOf(report.resolvedPct, report.segments[0].percent))
        assertEquals(67, review(many(2, conv()) + many(1, handed()), policy).resolvedPct)
    }
}
