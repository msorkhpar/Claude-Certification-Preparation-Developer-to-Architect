import harness.Scripted
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IdentityGateTest {
    private val blocked = "BLOCKED identity_required: Verify the customer's identity before this action."

    @Test
    fun theGateLetsOnlyVerificationThroughBeforeACustomerIsVerified() {
        val backend = Backend()
        assertEquals(Result("process_refund", blocked, true), gate(backend, "process_refund", mapOf("order_id" to "O1", "amount_cents" to 1)))
        assertTrue(gate(backend, "lookup_order", mapOf("order_id" to "O1")).isError)
        assertEquals(emptyList<String>(), backend.log)
        assertEquals(Result("verify_identity", "verified=yes", false), gate(backend, "verify_identity", mapOf("code" to "1234")))
        assertEquals(Result("lookup_order", "order_id=O1; total_cents=5000", false), gate(backend, "lookup_order", mapOf("order_id" to "O1")))
    }

    @Test
    fun aWrongCodeUnlocksNothing() {
        val backend = Backend()
        gate(backend, "verify_identity", mapOf("code" to "0000"))
        assertTrue(gate(backend, "process_refund", mapOf("order_id" to "O1", "amount_cents" to 1)).isError)
        assertEquals(listOf("verify_identity"), backend.log)
    }

    @Test
    fun theUngatedLoopRefundsBeforeAnyVerificationAndTheGatedOneDoesNot() {
        val plain = Backend()
        run(Scripted.client(*replies(false)).client(), plain, false)
        assertEquals(listOf("process_refund"), plain.log)
        val gated = Backend()
        run(Scripted.client(*replies(true)).client(), gated, true)
        assertEquals(listOf("verify_identity", "lookup_order", "process_refund"), gated.log)
    }
}
