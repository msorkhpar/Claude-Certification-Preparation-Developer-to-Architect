import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConversationReviewTest {
    private static final ConversationReview.Policy POLICY = new ConversationReview.Policy(12, 10, 80, 3);

    private static ConversationReview.Conversation conv(String segment, int turns, boolean resolved, String handoff, boolean needed, boolean repeated, boolean risk) {
        return new ConversationReview.Conversation("c", segment, turns, resolved, handoff, needed, repeated, risk);
    }

    private static ConversationReview.Conversation conv() {
        return conv("billing", 5, true, "none", false, false, false);
    }

    private static ConversationReview.Conversation conv(String segment) {
        return conv(segment, 5, true, "none", false, false, false);
    }

    /** A conversation a person took over, in the given segment. */
    private static ConversationReview.Conversation handed(String segment) {
        return conv(segment, 5, false, "requested", true, false, false);
    }

    private static ConversationReview.Conversation safe() {
        return conv("safety", 5, false, "safety", true, false, true);
    }

    private static List<ConversationReview.Conversation> many(int n, ConversationReview.Conversation c) {
        List<ConversationReview.Conversation> all = new ArrayList<>();
        for (int i = 0; i < n; i++) all.add(c);
        return all;
    }

    @SafeVarargs
    private static List<ConversationReview.Conversation> join(List<ConversationReview.Conversation>... parts) {
        List<ConversationReview.Conversation> all = new ArrayList<>();
        for (var p : parts) all.addAll(p);
        return all;
    }

    private static ConversationReview.Segment seg(String segment, int n, int resolved, int percent, boolean weak) {
        return new ConversationReview.Segment(segment, n, resolved, percent, weak);
    }

    @Test
    void m1_aMixedBatchGetsEveryCountTheSegmentsAndAVerdict() {
        var batch = join(many(3, conv()), List.of(handed("billing"), conv("smalltalk"), conv("smalltalk", 15, true, "none", false, false, false), safe(),
            conv("billing", 5, true, "none", false, true, false)));
        assertEquals(new ConversationReview.Report(8, 6, 75, 0, 1, 13, false, 0, 0,
            List.of(seg("billing", 5, 4, 80, false), seg("safety", 1, 0, 0, false), seg("smalltalk", 2, 2, 100, false)), "hold", "repeats"),
            ConversationReview.review(batch, POLICY));
    }

    @Test
    void e1_anEmptyBatchHasZeroFiguresAndIsHeldForLackOfData() {
        assertEquals(new ConversationReview.Report(0, 0, 0, 0, 0, 0, true, 0, 0, List.of(), "hold", "no_data"), ConversationReview.review(List.of(), POLICY));
    }

    @Test
    void e2_aSafetySignalCountsAsMissedUnlessItWentToAPersonAsASafetyHandOff() {
        var batch = join(many(5, conv()), List.of(conv("safety", 5, false, "requested", true, false, true), conv("safety", 5, false, "none", false, false, true), safe()));
        var report = ConversationReview.review(batch, POLICY);
        assertEquals(2, report.safetyMissed());
        assertEquals("hold", report.verdict());
        assertEquals("safety", report.reason());
        assertEquals(0, ConversationReview.review(join(many(5, conv()), List.of(safe())), POLICY).safetyMissed());
    }

    @Test
    void e3_aConversationIsOverlongOnlyAboveTheTurnLimitAndOnlyWhenNobodyTookOver() {
        assertEquals(0, ConversationReview.review(List.of(conv("billing", 12, true, "none", false, false, false)), POLICY).overlong());
        assertEquals(1, ConversationReview.review(List.of(conv("billing", 13, true, "none", false, false, false)), POLICY).overlong());
        assertEquals(0, ConversationReview.review(List.of(conv("billing", 30, false, "stalled", true, false, false)), POLICY).overlong());
    }

    @Test
    void e4_repeatedQuestionsAreAcceptableAtExactlyTheLimitAndNotAboveIt() {
        var rep = conv("billing", 5, true, "none", false, true, false);
        var at = ConversationReview.review(join(many(9, conv()), many(1, rep)), POLICY);
        assertEquals(10, at.repeatPct());
        assertTrue(at.repeatOk());
        assertEquals("ship", at.verdict());
        var over = ConversationReview.review(join(many(8, conv()), many(2, rep)), POLICY);
        assertEquals(20, over.repeatPct());
        assertFalse(over.repeatOk());
        assertEquals("hold", over.verdict());
        assertEquals("repeats", over.reason());
    }

    @Test
    void e5_aSegmentIsWeakOnlyBelowTheResolutionFloorAndNotAtIt() {
        var at = ConversationReview.review(join(many(8, conv()), many(2, handed("billing"))), POLICY);
        assertEquals(List.of(seg("billing", 10, 8, 80, false)), at.segments());
        assertEquals("none", at.reason());
        var below = ConversationReview.review(join(many(7, conv()), many(3, handed("billing"))), POLICY);
        assertEquals(List.of(seg("billing", 10, 7, 70, true)), below.segments());
        assertEquals("weak_segment", below.reason());
    }

    @Test
    void e6_aSegmentNeedsTheMinimumNumberOfConversationsBeforeItCanBeCalledWeak() {
        var two = ConversationReview.review(many(2, handed("refunds")), POLICY);
        assertEquals(List.of(seg("refunds", 2, 0, 0, false)), two.segments());
        var three = ConversationReview.review(many(3, handed("refunds")), POLICY);
        assertEquals(List.of(seg("refunds", 3, 0, 0, true)), three.segments());
        assertEquals("weak_segment", three.reason());
    }

    @Test
    void e7_aHandOffIsOverEscalationOnlyWhenNoPersonWasNeededAndNoSafetySignalWasPresent() {
        var batch = List.of(conv("billing", 5, true, "requested", false, false, false), handed("billing"), conv("billing", 5, true, "safety", false, false, true),
            conv("billing", 5, true, "none", true, false, false), conv("billing", 5, false, "stalled", false, false, false));
        var report = ConversationReview.review(batch, POLICY);
        assertEquals(2, report.overEscalated());
        assertEquals(1, report.underEscalated());
    }

    @Test
    void e8_onlyConversationsTheAssistantSettledAloneCountAsResolved() {
        var report = ConversationReview.review(List.of(conv(), conv("billing", 5, true, "requested", true, false, false), conv("billing", 5, false, "none", false, false, false)), POLICY);
        assertEquals(1, report.resolved());
        assertEquals(33, report.resolvedPct());
    }

    @Test
    void e9_percentagesAreWholeNumbersRoundedHalfUp() {
        var report = ConversationReview.review(join(many(1, conv()), many(7, handed("billing"))), POLICY);
        assertEquals(13, report.resolvedPct());
        assertEquals(13, report.segments().get(0).percent());
        assertEquals(67, ConversationReview.review(join(many(2, conv()), many(1, handed("billing"))), POLICY).resolvedPct());
    }
}
