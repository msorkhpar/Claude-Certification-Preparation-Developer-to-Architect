import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClaimsAssistantTest {
    private static final String WATER = "Water damage is covered up to 5,000 per claim.";

    private static ClaimsAssistant.Request request(String text, String consequence, String quote, int confidence) {
        return new ClaimsAssistant.Request("r", text, Set.of("policy"), consequence, quote, confidence);
    }

    private static ClaimsAssistant.Request request(String text) {
        return request(text, "low", WATER, 97);
    }

    @Test
    void identifiersBecomeTokensAndTheSameValueGetsTheSameToken() {
        ClaimsAssistant.Tokenised t = ClaimsAssistant.tokenise("write to a@example.com or a@example.com or b@example.org");
        assertEquals("write to <EMAIL_1> or <EMAIL_1> or <EMAIL_2>", t.sent());
        assertEquals("a@example.com", t.vault().get("<EMAIL_1>"));
        assertFalse(t.sent().contains("@"));
    }

    @Test
    void theReadersRightsComeBeforeTheRanking() {
        assertEquals("contract-9", ClaimsAssistant.retrieve("partner commission premiums", Set.of("contracts"), ClaimsAssistant.INDEX).id());
        assertNull(ClaimsAssistant.retrieve("partner commission premiums", Set.of("policy"), ClaimsAssistant.INDEX));
    }

    @Test
    void aTieGoesToTheSmallerIdAndNoOverlapIsNoEvidence() {
        assertEquals("policy-2-old", ClaimsAssistant.retrieve("water damage", Set.of("policy"), ClaimsAssistant.STALE_INDEX).id());
        assertEquals("policy-2", ClaimsAssistant.retrieve("water damage", Set.of("policy"), ClaimsAssistant.INDEX).id());
        assertNull(ClaimsAssistant.retrieve("zzzz yyyy", Set.of("policy"), ClaimsAssistant.INDEX));
    }

    @Test
    void aStaleOrUnsupportedAnswerIsHeldAndConfidenceDecidesTheRest() {
        String q = "How much does the policy cover for water damage?";
        assertEquals("hold: stale evidence (policy-2-old v2, current v3)", ClaimsAssistant.handle(request(q), ClaimsAssistant.STALE_INDEX).trace().outcome());
        assertEquals("hold: unsupported", ClaimsAssistant.handle(request(q, "low", "Water damage is covered up to 8,000 per claim.", 97), ClaimsAssistant.INDEX).trace().outcome());
        assertEquals("auto", ClaimsAssistant.handle(request(q, "low", WATER, 95), ClaimsAssistant.INDEX).trace().outcome());
        assertEquals("review", ClaimsAssistant.handle(request(q, "low", WATER, 94), ClaimsAssistant.INDEX).trace().outcome());
        assertEquals("human", ClaimsAssistant.handle(request(q, "high", WATER, 97), ClaimsAssistant.INDEX).trace().outcome());
    }

    @Test
    void theTraceHoldsIdsAndSizesAndNoText() {
        ClaimsAssistant.Handled h = ClaimsAssistant.handle(request("Claims reported from jo@example.com, how many days?", "low", "x", 97), ClaimsAssistant.INDEX);
        assertFalse(h.trace().toString().contains("jo@example.com") || h.sent().contains("@"));
    }

    @Test
    void aGateProtectsTheCostlySegmentEvenWhenGainsCoverTheLosses() {
        List<ClaimsAssistant.Case> cases = List.of(new ClaimsAssistant.Case("a", "refund", true, false), new ClaimsAssistant.Case("b", "status", false, true));
        assertEquals("no-go: protected segment lost answers: refund", ClaimsAssistant.release(cases, Set.of("refund")));
        assertEquals("go: lost 1, gained 1", ClaimsAssistant.release(cases, Set.of()));
        assertEquals("no-go: net loss: lost 1, gained 0", ClaimsAssistant.release(List.of(new ClaimsAssistant.Case("a", "x", true, false)), Set.of()));
    }
}
