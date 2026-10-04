import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SupportDeskTest {
    private static final SupportDesk.Call BEN = SupportDesk.BEN;

    private static SupportDesk.Call lookup(String id) {
        return SupportDesk.call("lookup_order", "order_id", id);
    }

    @Test
    void nothingRunsBeforeTheCustomerIsIdentifiedAndTheBackendSeesNoCall() {
        var desk = new SupportDesk.Desk();
        var result = desk.call("lookup_order", Map.of("order_id", "O1"));
        assertEquals("identity_required", result.code());
        assertTrue(result.message().contains("get_customer"));
        assertEquals(List.of(), desk.backend);
    }

    @Test
    void twoMatchingCustomersAreAQuestionAndNeverAGuess() {
        var run = SupportDesk.run(List.of(SupportDesk.call("get_customer", "query", "Ana Silva")));
        assertNull(run.getKey().customer);
        assertEquals("asked", run.getValue());
        assertEquals(List.of("ambiguous_match"), run.getKey().refused);
    }

    @Test
    void anOrderOfAnotherCustomerIsRefusedWithoutNamingItsOwner() {
        var desk = new SupportDesk.Desk();
        desk.call("get_customer", Map.of("query", "ana@example.com"));
        var result = desk.call("lookup_order", Map.of("order_id", "O1"));
        assertEquals("order_not_owned", result.code());
        assertFalse(result.message().contains("C3"));
        assertEquals(List.of(), desk.checked);
    }

    @Test
    void aRefundAtTheLimitRunsAndOneCentAboveNeedsAPerson() {
        var desk = new SupportDesk.Desk();
        desk.call(BEN.tool(), BEN.args());
        desk.flaky.clear();
        desk.call("lookup_order", Map.of("order_id", "O2"));
        assertTrue(desk.call("process_refund", Map.of("order_id", "O2", "amount_cents", SupportDesk.LIMIT)).ok());
        assertEquals("needs_human", desk.call("process_refund", Map.of("order_id", "O2", "amount_cents", SupportDesk.LIMIT + 1)).code());
        assertEquals(List.of("O2:10000"), desk.refunds);
    }

    @Test
    void aTransientFaultIsRetriedOnceAndAPermanentOneIsNot() {
        var run = SupportDesk.run(List.of(BEN, lookup("O2")));
        assertEquals(1, run.getKey().retries);
        assertEquals(List.of("O2"), run.getKey().checked);
        assertEquals("resolved", run.getValue());
        assertEquals(0, SupportDesk.run(List.of(BEN, lookup("O9"))).getKey().retries);
    }

    @Test
    void theSameCallThreeTimesInARowEscalatesAndDifferentCallsDoNot() {
        List<SupportDesk.Call> script = new ArrayList<>(List.of(BEN));
        for (int i = 0; i < SupportDesk.STALL; i++) script.add(lookup("O9"));
        var run = SupportDesk.run(script);
        assertEquals("escalated", run.getValue());
        assertEquals("stalled", run.getKey().escalation.trigger());
        assertEquals("resolved", SupportDesk.run(List.of(BEN, lookup("O9"), lookup("O8"), lookup("O9"))).getValue());
    }

    @Test
    void aPersonCanAlwaysBeReachedAndTheRecordComesFromTheDesksState() {
        var first = SupportDesk.run(List.of(SupportDesk.call("escalate_to_human", "trigger", "customer_request", "reason", "wants a person")));
        assertEquals("escalated", first.getValue());
        assertFalse(first.getKey().escalation.verified());
        var desk = SupportDesk.run(List.of(BEN, lookup("O2"), SupportDesk.call("process_refund", "order_id", "O2", "amount_cents", 25000),
            SupportDesk.call("escalate_to_human", "trigger", "needs_human", "reason", "x"))).getKey();
        var e = desk.escalation;
        assertEquals("C3", e.customer());
        assertEquals(List.of("O2"), e.orders());
        assertEquals(List.of(), e.refunds());
        assertEquals(List.of("needs_human"), e.refused());
    }
}
