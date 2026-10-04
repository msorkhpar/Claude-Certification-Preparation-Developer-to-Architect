import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BatchAndReviewTest {
    @Test
    void theWorstWaitIsAnIntervalPlusTheWindowPlusTheHandling() {
        assertEquals(List.of(30, 32, 25), List.of(BatchAndReview.worstCaseWait(4, 24, 2), BatchAndReview.worstCaseWait(6, 24, 2), BatchAndReview.worstCaseWait(1, 24, 0)));
    }

    @Test
    void aCustomIdIsOneToSixtyFourLettersDigitsHyphensOrUnderscores() {
        assertEquals("invoice-0042_a", BatchAndReview.batchEntry("invoice-0042_a", Map.of()).customId());
        for (String bad : List.of("", "has space", "dot.dot", "x".repeat(65))) assertThrows(IllegalArgumentException.class, () -> BatchAndReview.batchEntry(bad, Map.of()));
        assertEquals("x".repeat(64), BatchAndReview.batchEntry("x".repeat(64), Map.of()).customId());
    }

    @Test
    void streamSpeedAndAZeroMaxTokensAreRefused() {
        for (Map<String, Object> params : List.<Map<String, Object>>of(Map.of("stream", true), Map.of("speed", "fast"), Map.of("max_tokens", 0))) {
            assertThrows(IllegalArgumentException.class, () -> BatchAndReview.batchEntry("a1", params));
        }
        assertEquals(Map.of("stream", false, "max_tokens", 10), BatchAndReview.batchEntry("a1", Map.of("stream", false, "max_tokens", 10)).params());
    }

    @Test
    void resultsArePairedByCustomIdWhateverTheirOrder() {
        BatchAndReview.Pairing p = BatchAndReview.matchResults(List.of("a1", "a2", "a3"),
            List.of(new BatchAndReview.Matched("a2", "expired"), new BatchAndReview.Matched("z9", "succeeded"), new BatchAndReview.Matched("a1", "succeeded")));
        assertEquals(List.of(new BatchAndReview.Matched("a1", "succeeded"), new BatchAndReview.Matched("a2", "expired"), new BatchAndReview.Matched("a3", "missing")), p.matched());
        assertEquals(List.of("z9"), p.unrequested());
    }

    @Test
    void anIndependentReviewRequestLeavesTheGeneratorsReasoningOut() {
        assertTrue(BatchAndReview.reviewRequest("code", "because", false).contains("because"));
        String independent = BatchAndReview.reviewRequest("code", "because", true);
        assertTrue(!independent.contains("because") && independent.contains("<code>code</code>"));
    }
}
