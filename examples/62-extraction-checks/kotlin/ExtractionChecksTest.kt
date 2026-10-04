import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtractionChecksTest {
    private val wrong = Invoice(listOf(100.0, 20.5), 130.0, "Total due: 130.00 EUR")
    private val right = Invoice(listOf(100.0, 20.5, 9.5), 130.0, "Total due: 130.00 EUR")

    @Test
    fun aRequiredFieldGetsFilledAndANullableOneStaysNull() {
        assertEquals("PO-0000", scriptedValue("no order here", false))
        assertNull(scriptedValue("no order here", true))
        assertEquals("4471", scriptedValue("Order PO 4471 shipped", false))
    }

    @Test
    fun theChecksFindAWrongSumAndAQuotationThatIsNotInTheDocument() {
        assertEquals(emptyList<String>(), check(right, DOC))
        assertEquals(listOf("total: the items add up to 120.5, not 130.0"), check(wrong, DOC))
        assertEquals(listOf("evidence: this quotation is not in the document"), check(right.copy(evidence = "Total due: 130.00 USD"), DOC))
    }

    @Test
    fun aRetryCarriesTheDocumentTheFailedAnswerAndTheProblems() {
        val result = extract(DOC, listOf(wrong, right))
        assertEquals("valid", result.status)
        assertEquals(2, result.attempts)
        assertEquals(1, result.feedback.size)
        val text = result.feedback[0]
        assertTrue(DOC in text)
        assertTrue("\"total\": 130.0" in text)
        assertTrue("- total: the items add up to 120.5, not 130.0" in text)
        assertEquals("failed", extract(DOC, listOf(wrong, wrong), maxRetries = 1).status)
        assertEquals("failed", extract(DOC, listOf(wrong, right), maxRetries = 0).status)
    }

    @Test
    fun accuracyOnValidatedRecordsAloneHidesTheFailures() {
        val outcomes = List(5) { "valid" to true } + listOf("valid" to false) + List(4) { "failed" to false }
        assertEquals(mapOf("validated_only" to 0.83, "all_documents" to 0.5), accuracy(outcomes))
        assertEquals(mapOf("validated_only" to 0.0, "all_documents" to 0.0), accuracy(emptyList()))
    }

    @Test
    fun forcedChoiceIsUsedWhereAcceptedAndAutoWithAReplyCheckElsewhere() {
        val two = listOf("a", "b")
        assertEquals(mapOf("tool_choice" to "any", "check_reply" to false), requestChoice("claude-haiku-4-5", two))
        assertEquals(mapOf("tool_choice" to "tool:a", "check_reply" to false), requestChoice("claude-haiku-4-5", listOf("a")))
        assertEquals(mapOf("tool_choice" to "auto", "check_reply" to true), requestChoice("claude-opus-5-5", two))
    }
}
