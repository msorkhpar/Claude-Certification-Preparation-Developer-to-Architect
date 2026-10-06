import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThreeStylesTest {
    @Test
    void theGraphFollowsItsEdgesAndOtherTopicsSkipTheLookup() {
        ThreeStyles.Scripted m = new ThreeStyles.Scripted("Billing", "reply");
        ThreeStyles.GraphRun result = ThreeStyles.runGraph(m, ThreeStyles.TICKET, 0, null);
        assertEquals(List.of("classify", "lookup", "draft"), result.path());
        assertEquals("paid twice on 2026-09-30", result.state().get("invoice"));
        assertEquals(2, m.seen.size());
        ThreeStyles.GraphRun other = ThreeStyles.runGraph(new ThreeStyles.Scripted("other", "hello"), "How do I log in?", 0, null);
        assertEquals(List.of("classify", "draft"), other.path());
        assertFalse(other.state().containsKey("invoice"));
    }

    @Test
    void aGraphResumesFromACheckpointWithoutRepeatingEarlierNodes() {
        ThreeStyles.GraphRun first = ThreeStyles.runGraph(new ThreeStyles.Scripted("billing", "the reply"), ThreeStyles.TICKET, 0, null);
        ThreeStyles.Scripted again = new ThreeStyles.Scripted("the reply");
        ThreeStyles.GraphRun resumed = ThreeStyles.runGraph(again, ThreeStyles.TICKET, 2, new ArrayList<>(first.checkpoints()));
        assertEquals(List.of("draft"), resumed.path());
        assertEquals(1, again.seen.size());
        assertEquals(first.state(), resumed.state());
    }

    @Test
    void theAgentLoopRunsTheToolTheModelPickedAndStopsAtTheStepLimit() throws Exception {
        ThreeStyles.Scripted m = new ThreeStyles.Scripted("{\"tool\": \"lookup_invoice\", \"arg\": \"1042\"}", "{\"final\": \"done\"}");
        assertEquals(new ThreeStyles.AgentRun("done", List.of("lookup_invoice(1042) -> paid twice on 2026-09-30"), null), ThreeStyles.runAgent(m, ThreeStyles.TICKET, 5));
        String call = "{\"tool\": \"lookup_invoice\", \"arg\": \"x\"}";
        ThreeStyles.Scripted endless = new ThreeStyles.Scripted(call, call, call, call);
        ThreeStyles.AgentRun result = ThreeStyles.runAgent(endless, ThreeStyles.TICKET, 4);
        assertNull(result.reply());
        assertEquals("max_steps", result.stopped());
        assertEquals(4, endless.seen.size());
    }

    @Test
    void aTypedReplyIsValidatedAndAMismatchIsFedBackOnce() {
        assertEquals(Map.of("topic", "x", "refund_cents", 5), ThreeStyles.validate("{\"topic\": \"x\", \"refund_cents\": 5}").data());
        assertEquals("field refund_cents must be int", ThreeStyles.validate("{\"topic\": \"x\", \"refund_cents\": true}").error());
        assertEquals("the reply is not JSON", ThreeStyles.validate("nope").error());
        ThreeStyles.Scripted m = new ThreeStyles.Scripted("{\"topic\": \"billing\", \"refund_cents\": \"49\"}", "{\"topic\": \"billing\", \"refund_cents\": 49}");
        ThreeStyles.TypedRun result = ThreeStyles.runTyped(m, ThreeStyles.TICKET, 1);
        assertEquals(Map.of("topic", "billing", "refund_cents", 49), result.data());
        assertEquals(2, result.attempts());
        assertTrue(m.seen.get(1).contains("field refund_cents must be int"));
        assertEquals(2, ThreeStyles.runTyped(new ThreeStyles.Scripted("a", "b"), ThreeStyles.TICKET, 1).attempts());
    }
}
