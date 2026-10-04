import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SupportDeskTest {
    private fun lookup(id: String) = call("lookup_order", "order_id" to id)

    @Test
    fun nothingRunsBeforeTheCustomerIsIdentifiedAndTheBackendSeesNoCall() {
        val desk = Desk()
        val result = desk.call("lookup_order", mapOf("order_id" to "O1"))
        assertEquals("identity_required", result.code)
        assertTrue(result.message!!.contains("get_customer"))
        assertEquals(listOf<String>(), desk.backend)
    }

    @Test
    fun twoMatchingCustomersAreAQuestionAndNeverAGuess() {
        val (desk, outcome) = run(listOf(call("get_customer", "query" to "Ana Silva")))
        assertNull(desk.customer)
        assertEquals("asked", outcome)
        assertEquals(listOf("ambiguous_match"), desk.refused)
    }

    @Test
    fun anOrderOfAnotherCustomerIsRefusedWithoutNamingItsOwner() {
        val desk = Desk()
        desk.call("get_customer", mapOf("query" to "ana@example.com"))
        val result = desk.call("lookup_order", mapOf("order_id" to "O1"))
        assertEquals("order_not_owned", result.code)
        assertFalse(result.message!!.contains("C3"))
        assertEquals(listOf<String>(), desk.checked)
    }

    @Test
    fun aRefundAtTheLimitRunsAndOneCentAboveNeedsAPerson() {
        val desk = Desk()
        desk.call(BEN.tool, BEN.args)
        desk.flaky.clear()
        desk.call("lookup_order", mapOf("order_id" to "O2"))
        assertTrue(desk.call("process_refund", mapOf("order_id" to "O2", "amount_cents" to LIMIT)).ok)
        assertEquals("needs_human", desk.call("process_refund", mapOf("order_id" to "O2", "amount_cents" to LIMIT + 1)).code)
        assertEquals(listOf("O2:10000"), desk.refunds)
    }

    @Test
    fun aTransientFaultIsRetriedOnceAndAPermanentOneIsNot() {
        val (desk, outcome) = run(listOf(BEN, lookup("O2")))
        assertEquals(1, desk.retries)
        assertEquals(listOf("O2"), desk.checked)
        assertEquals("resolved", outcome)
        assertEquals(0, run(listOf(BEN, lookup("O9"))).first.retries)
    }

    @Test
    fun theSameCallThreeTimesInARowEscalatesAndDifferentCallsDoNot() {
        val (desk, outcome) = run(listOf(BEN) + List(STALL) { lookup("O9") })
        assertEquals("escalated", outcome)
        assertEquals("stalled", desk.escalation!!.trigger)
        assertEquals("resolved", run(listOf(BEN, lookup("O9"), lookup("O8"), lookup("O9"))).second)
    }

    @Test
    fun aPersonCanAlwaysBeReachedAndTheRecordComesFromTheDesksState() {
        val (first, outcome) = run(listOf(call("escalate_to_human", "trigger" to "customer_request", "reason" to "wants a person")))
        assertEquals("escalated", outcome)
        assertFalse(first.escalation!!.verified)
        val desk = run(listOf(BEN, lookup("O2"), call("process_refund", "order_id" to "O2", "amount_cents" to 25000), call("escalate_to_human", "trigger" to "needs_human", "reason" to "x"))).first
        val e = desk.escalation!!
        assertEquals("C3", e.customer)
        assertEquals(listOf("O2"), e.orders)
        assertEquals(listOf<String>(), e.refunds)
        assertEquals(listOf("needs_human"), e.refused)
    }
}
