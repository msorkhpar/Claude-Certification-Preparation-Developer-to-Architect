import static org.junit.jupiter.api.Assertions.*;

import harness.Scripted;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IdentityGateTest {
    private static final String BLOCKED = "BLOCKED identity_required: Verify the customer's identity before this action.";

    @Test
    void theGateLetsOnlyVerificationThroughBeforeACustomerIsVerified() {
        var backend = new IdentityGate.Backend();
        assertEquals(new IdentityGate.Result("process_refund", BLOCKED, true), IdentityGate.gate(backend, "process_refund", Map.of("order_id", "O1", "amount_cents", 1)));
        assertTrue(IdentityGate.gate(backend, "lookup_order", Map.of("order_id", "O1")).isError());
        assertEquals(List.of(), backend.log);
        assertEquals(new IdentityGate.Result("verify_identity", "verified=yes", false), IdentityGate.gate(backend, "verify_identity", Map.of("code", "1234")));
        assertEquals(new IdentityGate.Result("lookup_order", "order_id=O1; total_cents=5000", false), IdentityGate.gate(backend, "lookup_order", Map.of("order_id", "O1")));
    }

    @Test
    void aWrongCodeUnlocksNothing() {
        var backend = new IdentityGate.Backend();
        IdentityGate.gate(backend, "verify_identity", Map.of("code", "0000"));
        assertTrue(IdentityGate.gate(backend, "process_refund", Map.of("order_id", "O1", "amount_cents", 1)).isError());
        assertEquals(List.of("verify_identity"), backend.log);
    }

    @Test
    void theUngatedLoopRefundsBeforeAnyVerificationAndTheGatedOneDoesNot() {
        var plain = new IdentityGate.Backend();
        IdentityGate.run(Scripted.client(IdentityGate.replies(false)).client(), plain, false);
        assertEquals(List.of("process_refund"), plain.log);
        var gated = new IdentityGate.Backend();
        IdentityGate.run(Scripted.client(IdentityGate.replies(true)).client(), gated, true);
        assertEquals(List.of("verify_identity", "lookup_order", "process_refund"), gated.log);
    }
}
