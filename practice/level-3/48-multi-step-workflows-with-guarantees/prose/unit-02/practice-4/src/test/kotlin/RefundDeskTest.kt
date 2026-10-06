import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RefundDeskTest {
    private fun order(id: String, customer: String, total: Int): MutableMap<String, Any?> =
        linkedMapOf("order_id" to id, "customer_id" to customer, "total_cents" to total, "refunded_cents" to 0)

    /** A scripted backend that keeps a log of every call it receives. */
    private class ScriptedBackend(breakRefund: Boolean) {
        val log = mutableListOf<String>()
        var refunds = 0
        private val table = mapOf("O1" to linkedMapOf<String, Any?>("order_id" to "O1", "customer_id" to "C1", "total_cents" to 5000, "refunded_cents" to 0),
            "O2" to linkedMapOf<String, Any?>("order_id" to "O2", "customer_id" to "C2", "total_cents" to 3000, "refunded_cents" to 0),
            "O9" to linkedMapOf<String, Any?>("order_id" to "O9", "customer_id" to "C1", "total_cents" to 50000, "refunded_cents" to 0))
        val handlers: Backend = mapOf(
            "verify_identity" to { a: Map<String, Any?> -> log.add("verify_identity"); if (a["code"] == "1234") linkedMapOf("verified" to "yes", "customer_id" to "C1") else linkedMapOf("verified" to "no") },
            "lookup_order" to { a: Map<String, Any?> -> log.add("lookup_order"); LinkedHashMap(table.getValue(a["order_id"] as String)) },
            "process_refund" to { a: Map<String, Any?> ->
                log.add("process_refund")
                if (breakRefund) throw IllegalStateException("payment service offline")
                refunds++
                linkedMapOf<String, Any?>("refund_id" to "R$refunds", "amount_cents" to a["amount_cents"])
            },
            "escalate" to { _: Map<String, Any?> -> log.add("escalate"); linkedMapOf<String, Any?>("ticket_id" to "T1") },
        )
    }

    private class Made(breakRefund: Boolean, limit: Int) {
        val backend = ScriptedBackend(breakRefund)
        val desk = RefundDesk(backend.handlers, limit)
    }

    private fun desk() = Made(false, 10000)

    private fun res(d: RefundDesk, name: String, vararg args: Pair<String, Any?>): Map<String, Any?> {
        val out = d.call(name, linkedMapOf(*args))
        assertNotNull(out, "call returned null")
        return out!!
    }

    private fun verified(breakRefund: Boolean = false, limit: Int = 10000): Made {
        val m = Made(breakRefund, limit)
        res(m.desk, "verify_identity", "code" to "1234")
        return m
    }

    private fun blockedResult(content: String, code: String): Map<String, Any?> = mapOf("content" to content, "is_error" to true, "blocked" to code)

    private fun state(d: RefundDesk): Map<String, Any?> {
        val s = d.state()
        assertNotNull(s, "state returned null")
        return s!!
    }

    @Suppress("UNCHECKED_CAST")
    private fun orderOf(state: Map<String, Any?>, id: String): Map<String, Any?>? = (state["orders"] as Map<String, Map<String, Any?>>)[id]

    @Test
    fun m1_aVerifiedCustomerCanLookUpAnOrderAndBeRefundedWithinTheLimit() {
        val m = desk()
        assertEquals(mapOf("content" to "verified=yes; customer_id=C1", "is_error" to false, "blocked" to null), res(m.desk, "verify_identity", "code" to "1234"))
        assertEquals("order_id=O1; customer_id=C1; total_cents=5000; refunded_cents=0", res(m.desk, "lookup_order", "order_id" to "O1")["content"])
        assertEquals(mapOf("content" to "refund_id=R1; amount_cents=2000", "is_error" to false, "blocked" to null), res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to 2000))
        val s = state(m.desk)
        assertEquals(listOf("C1", false, 0, emptyList<Any?>()), listOf(s["customer"], s["locked"], s["failures"], s["blocked"]))
        assertEquals(listOf(mapOf("order_id" to "O1", "amount_cents" to 2000, "refund_id" to "R1")), s["refunds"])
        assertEquals(2000, orderOf(s, "O1")!!["refunded_cents"])
        assertEquals(listOf("verify_identity", "lookup_order", "process_refund"), m.backend.log)
    }

    @Test
    fun e1_aRefundBeforeIdentityIsVerifiedIsBlockedInCodeAndNeverReachesTheBackend() {
        val m = desk()
        assertEquals(blockedResult("BLOCKED identity_required: Verify the customer's identity before this action.", "identity_required"), res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to 500))
        assertEquals(emptyList<String>(), m.backend.log)
        assertEquals(listOf(mapOf("tool" to "process_refund", "code" to "identity_required")), state(m.desk)["blocked"])
        res(m.desk, "verify_identity", "code" to "1234")
        res(m.desk, "lookup_order", "order_id" to "O1")
        assertNull(res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to 500)["blocked"])
        assertEquals("process_refund", m.backend.log.last())
    }

    @Test
    fun e2_anOrderThatBelongsToSomeoneElseIsNeitherShownNorRefundable() {
        val m = desk()
        assertEquals("identity_required", res(m.desk, "lookup_order", "order_id" to "O1")["blocked"])
        assertEquals(emptyList<String>(), m.backend.log)
        res(m.desk, "verify_identity", "code" to "1234")
        val other = res(m.desk, "lookup_order", "order_id" to "O2")
        assertEquals(blockedResult("BLOCKED order_not_owned: That order does not belong to the verified customer.", "order_not_owned"), other)
        assertFalse((other["content"] as String).contains("C2"))
        assertNull(orderOf(state(m.desk), "O2"))
        assertEquals("order_not_checked", res(m.desk, "process_refund", "order_id" to "O2", "amount_cents" to 100)["blocked"])
        assertFalse(m.backend.log.contains("process_refund"))
    }

    @Test
    fun e3_aRefundIsCheckedAgainstTheOrderItsAmountAndWhatIsLeft() {
        val m = verified()
        assertEquals("order_not_checked", res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to 100)["blocked"])
        res(m.desk, "lookup_order", "order_id" to "O1")
        for (bad in listOf<Any?>(0, -5, 12.5, "100", true, null)) {
            assertEquals("bad_amount", res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to bad)["blocked"], bad.toString())
        }
        assertEquals("exceeds_order", res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to 5001)["blocked"])
        assertNull(res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to 3000)["blocked"])
        val out = res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to 2500)
        assertEquals(listOf<Any?>("exceeds_order", "BLOCKED exceeds_order: The amount is more than what is left to refund on the order."), listOf(out["blocked"], out["content"]))
        assertNull(res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to 2000)["blocked"])
        assertEquals(2, m.backend.log.count { it == "process_refund" })
    }

    @Test
    fun e4_aRefundOverTheLimitIsNotExecutedAndBecomesAStructuredHandOff() {
        val m = verified(limit = 10000)
        res(m.desk, "lookup_order", "order_id" to "O9")
        val out = res(m.desk, "process_refund", "order_id" to "O9", "amount_cents" to 25000)
        assertEquals(listOf<Any?>("needs_human", true, "BLOCKED needs_human: Refunds over the limit need a person."), listOf(out["blocked"], out["is_error"], out["content"]))
        assertFalse(m.backend.log.contains("process_refund"))
        assertNull(res(m.desk, "process_refund", "order_id" to "O9", "amount_cents" to 10000)["blocked"])
        assertEquals("needs_human", res(m.desk, "process_refund", "order_id" to "O9", "amount_cents" to 10001)["blocked"])
        val handoff = m.desk.handoff("Customer asks for a 250.00 refund")
        assertNotNull(handoff, "handoff returned null")
        assertEquals(mapOf("customer_id" to "C1", "identity_verified" to true, "reason" to "Customer asks for a 250.00 refund", "orders_checked" to listOf("O9"),
            "refunds_done" to listOf(mapOf("order_id" to "O9", "amount_cents" to 10000, "refund_id" to "R1")),
            "blocked" to listOf(mapOf("tool" to "process_refund", "code" to "needs_human"), mapOf("tool" to "process_refund", "code" to "needs_human")), "recommended_action" to "review_refund"), handoff)
    }

    @Test
    fun e5_aFailedCheckDoesNotUnlockAnythingAndThreeInARowLockTheDesk() {
        val m = desk()
        assertEquals("verified=no", res(m.desk, "verify_identity", "code" to "0000")["content"])
        assertEquals("identity_required", res(m.desk, "lookup_order", "order_id" to "O1")["blocked"])
        res(m.desk, "verify_identity", "code" to "1234")
        res(m.desk, "verify_identity", "code" to "0000")
        assertNull(state(m.desk)["customer"])
        assertEquals("identity_required", res(m.desk, "lookup_order", "order_id" to "O1")["blocked"])
        res(m.desk, "verify_identity", "code" to "0000")
        res(m.desk, "verify_identity", "code" to "0000")
        val s = state(m.desk)
        assertEquals(listOf<Any?>(true, 3), listOf(s["locked"], s["failures"]))
        assertEquals(blockedResult("BLOCKED locked: Too many failed identity checks; escalate to a person.", "locked"), res(m.desk, "verify_identity", "code" to "1234"))
        assertEquals("ticket_id=T1", res(m.desk, "escalate", "reason" to "locked out")["content"])
        val handoff = m.desk.handoff("identity could not be verified")
        assertNotNull(handoff, "handoff returned null")
        assertEquals("verify_identity_manually", handoff!!["recommended_action"])
    }

    @Test
    fun e6_unknownToolsAndBackendErrorsAreReportedAndTheHandOffListsEveryBlock() {
        val m = verified(breakRefund = true)
        res(m.desk, "lookup_order", "order_id" to "O1")
        assertEquals(blockedResult("BLOCKED unknown_tool: Unknown tool: delete_everything", "unknown_tool"), res(m.desk, "delete_everything"))
        assertEquals(mapOf("content" to "payment service offline", "is_error" to true, "blocked" to null), res(m.desk, "process_refund", "order_id" to "O1", "amount_cents" to 500))
        val s = state(m.desk)
        assertEquals(emptyList<Any?>(), s["refunds"])
        assertEquals(0, orderOf(s, "O1")!!["refunded_cents"])
        val handoff = m.desk.handoff("the payment service is down")
        assertNotNull(handoff, "handoff returned null")
        assertEquals(listOf(mapOf("tool" to "delete_everything", "code" to "unknown_tool")), handoff!!["blocked"])
        assertEquals(listOf(emptyList<Any?>(), "review_case", listOf("O1")), listOf(handoff["refunds_done"], handoff["recommended_action"], handoff["orders_checked"]))
    }
}
