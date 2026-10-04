import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ControlChainTest {
    private static final String SOURCE = "Water damage is covered up to 5,000 per claim.";
    private static final ControlChain.Action REPLY = new ControlChain.Action("draft_reply", "low");
    private static final ControlChain.Action REFUND = new ControlChain.Action("issue_refund", "high");

    private static ControlChain.Answer answer(int confidence, String quote) {
        return new ControlChain.Answer("ok", confidence, quote);
    }

    @Test
    void aDownScreenHoldsAHighConsequenceActionAndFlagsALowOne() {
        assertEquals("hold: screen down", ControlChain.route(REFUND, answer(99, SOURCE), SOURCE, false));
        assertEquals("auto (unscreened)", ControlChain.route(REPLY, answer(99, SOURCE), SOURCE, false));
        assertEquals("auto", ControlChain.route(REPLY, answer(99, SOURCE), SOURCE, true));
    }

    @Test
    void aConfidentAnswerThatTheSourceDoesNotSupportIsHeld() {
        ControlChain.Answer wrong = answer(100, "Water damage is covered up to 8,000 per claim.");
        assertEquals("hold: unsupported", ControlChain.route(REPLY, wrong, SOURCE, true));
        assertEquals("hold: unsupported", ControlChain.route(REFUND, wrong, SOURCE, true));
    }

    @Test
    void confidenceExactlyAtTheThresholdGoesOutAndOneBelowIsReviewed() {
        assertEquals("auto", ControlChain.route(REPLY, answer(95, SOURCE), SOURCE, true));
        assertEquals("review", ControlChain.route(REPLY, answer(94, SOURCE), SOURCE, true));
    }

    @Test
    void aHighConsequenceActionAlwaysReachesAPerson() {
        assertEquals("human", ControlChain.route(REFUND, answer(100, SOURCE), SOURCE, true));
    }

    @Test
    void theAuditRecordHoldsNoContentAndErasureUnlinksOnlyThePerson() {
        Map<String, Object> record = ControlChain.auditRecord("r-1", REFUND, "human", "secret text");
        assertEquals(11, record.get("chars"));
        assertEquals(false, record.get("content_stored"));
        assertFalse(record.toString().contains("secret text"));
        Map<String, String> vault = new LinkedHashMap<>();
        vault.put("<A>", "p1");
        vault.put("<B>", "p2");
        vault.put("<C>", "p1");
        ControlChain.Erased erased = ControlChain.erase(vault, "p1");
        assertEquals(Map.of("<B>", "p2"), erased.kept());
        assertEquals(2, erased.removed());
    }
}
