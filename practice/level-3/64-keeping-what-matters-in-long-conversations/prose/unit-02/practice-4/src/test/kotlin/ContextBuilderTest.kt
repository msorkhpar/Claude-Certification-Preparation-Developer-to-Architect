import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContextBuilderTest {
    private fun <T : Any> got(value: T?): T {
        assertNotNull(value, "the function returned nothing")
        return value!!
    }

    private fun order() = linkedMapOf("order_id" to "A-1042", "purchase_date" to "2026-09-02", "items" to "2 x kettle", "return_window" to "30 days", "warehouse_bin" to "R7-22", "carrier_hash" to "9f3c", "refund_amount" to "$129.50")

    private fun fact(customer: String, name: String, value: String) = FactEntry(customer, name, value, "2026-09-02")

    private fun msg(role: String, kind: String, id: String, text: String) = Message(role, kind, id, text)

    @Test
    fun m1_trimmingKeepsOnlyTheNamedFieldsWithTheirExactValuesInTheNamedOrder() {
        assertEquals(listOf("refund_amount=$129.50", "order_id=A-1042"), got(trimRecord(order(), listOf("refund_amount", "order_id"))).map { "${it.key}=${it.value}" })
        assertFalse("warehouse_bin" in got(trimRecord(order(), listOf("order_id", "items"))))
    }

    @Test
    fun e1_aFieldThatTheRecordDoesNotHaveIsSkipped() {
        assertEquals(mapOf("order_id" to "A-1042"), trimRecord(order(), listOf("order_id", "tracking_url")))
        assertEquals(emptyMap<String, String>(), trimRecord(emptyMap(), listOf("order_id")))
    }

    @Test
    fun e2_aNewerFactReplacesTheOldOneAndTheOldValueIsKeptAsHistory() {
        val start = got(updateFacts(emptyMap(), "address", "12 Oak St", "2026-08-01"))
        assertEquals(mapOf("address" to Fact("12 Oak St", "2026-08-01", emptyList())), start)
        val later = got(updateFacts(start, "address", "9 Elm Rd", "2026-09-10"))
        assertEquals(Fact("9 Elm Rd", "2026-09-10", listOf("12 Oak St@2026-08-01")), later["address"])
        assertEquals(Fact("12 Oak St", "2026-08-01", emptyList()), start["address"])
        assertEquals(Fact("9 Elm Rd", "2026-08-01", listOf("12 Oak St@2026-08-01")), got(updateFacts(start, "address", "9 Elm Rd", "2026-08-01"))["address"])
    }

    @Test
    fun e3_anOlderFactThatArrivesLateDoesNotReplaceTheCurrentOne() {
        val current = got(updateFacts(emptyMap(), "address", "9 Elm Rd", "2026-09-10"))
        val after = got(updateFacts(current, "address", "12 Oak St", "2026-08-01"))
        assertEquals(Fact("9 Elm Rd", "2026-09-10", listOf("12 Oak St@2026-08-01")), after["address"])
    }

    @Test
    fun e4_theCaseFactsOfAnotherCustomerNeverEnterTheContext() {
        val text = got(buildContext("c1", listOf(fact("c1", "refund", "$129.50"), fact("c2", "refund", "$20.00")), "summary", emptyList()))
        assertTrue("$129.50" in text && "$20.00" !in text)
        assertFalse("## Case facts" in got(buildContext("c3", listOf(fact("c1", "refund", "$129.50")), "summary", emptyList())))
    }

    @Test
    fun e5_theContextPutsCaseFactsFirstThenTheSummaryThenTheRecentMessages() {
        val text = buildContext("c1", listOf(fact("c1", "refund", "$129.50"), fact("c1", "order", "A-1042")), "They want the money back.", listOf(msg("user", "text", "", "Any news?")))
        assertEquals("## Case facts\nrefund: $129.50 (as of 2026-09-02)\norder: A-1042 (as of 2026-09-02)\n\n## Summary so far\nThey want the money back.\n\n## Recent messages\nuser: Any news?", text)
    }

    @Test
    fun e6_aSummaryThatLosesAnExactValueIsReported() {
        val facts = listOf(fact("c1", "refund", "$129.50"), fact("c1", "deadline", "2026-09-30"), fact("c1", "order", "A-1042"))
        assertEquals(listOf("refund", "deadline"), missingFromSummary("Refund of about $130 for order A-1042, due end of month.", facts))
        assertEquals(emptyList<String>(), missingFromSummary("$129.50, 2026-09-30, A-1042", facts))
    }

    @Test
    fun e7_theWindowDropsTheOldestMessagesAndKeepsAToolCallWithItsResult() {
        val messages = listOf(msg("user", "text", "", "x".repeat(40)), msg("assistant", "tool_use", "t1", "y".repeat(40)), msg("user", "tool_result", "t1", "z".repeat(40)), msg("assistant", "text", "", "done....."))
        assertEquals(listOf("d"), got(window(messages, 14)).map { it.text.substring(0, 1) })
        assertEquals(listOf("tool_use", "tool_result", "text"), got(window(messages, 23)).map { it.kind })
        assertEquals(4, got(window(messages, 1000)).size)
        assertEquals(emptyList<Message>(), window(messages, 0))
    }
}
