import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtractionTest {
    private val doc = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 120.50 EUR\nThank you."
    private val docNoVendor = "Items: 10.00\nTotal due: 10.00 USD"

    private fun good(): Map<String, Any?> = linkedMapOf("vendor" to "Acme Tools", "currency" to "EUR", "currency_detail" to null, "line_items" to listOf(100.0, 20.5), "stated_total" to 120.5,
        "calculated_total" to 120.5, "conflict_detected" to false, "provenance" to mapOf("vendor" to "Invoice from Acme Tools", "currency" to "120.50 EUR", "stated_total" to "Total due: 120.50 EUR"))

    private fun record(vararg over: Pair<String, Any?>): Map<String, Any?> = LinkedHashMap(good()).also { it.putAll(over) }

    private class Scripted(val records: List<Map<String, Any?>>) {
        val calls = mutableListOf<Map<String, Any?>>()
        fun call(document: String, feedback: Map<String, Any?>?): Map<String, Any?> {
            calls += mapOf("document" to document, "feedback" to feedback)
            return records[minOf(calls.size - 1, records.size - 1)]
        }
    }

    private fun run(s: Scripted, document: String, required: List<String> = emptyList(), maxRetries: Int = 2): Map<String, Any?> {
        val result = extractDocument(document, s::call, required, maxRetries)
        assertNotNull(result, "extractDocument returned nothing")
        return result!!
    }

    private fun kinds(errors: List<Map<String, Any?>>?): List<String> {
        assertNotNull(errors, "validate returned nothing")
        return errors!!.map { "${it["kind"]}:${it["field"]}" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun errorsOf(call: Map<String, Any?>): List<Map<String, Any?>> = (call["feedback"] as Map<String, Any?>)["errors"] as List<Map<String, Any?>>

    @Test
    fun m1_aDocumentWithEveryValuePresentAndQuotedComesBackValidOnTheFirstAttempt() {
        val s = Scripted(listOf(good()))
        assertEquals(mapOf("status" to "valid", "record" to good(), "attempts" to 1, "errors" to emptyList<Any?>()), run(s, doc))
        assertEquals(listOf(mapOf("document" to doc, "feedback" to null)), s.calls)
    }

    @Test
    fun e1_aValueTheDocumentDoesNotGiveIsNullAndNeedsNoQuoteWhileAnInventedValueFailsAsUngrounded() {
        val noVendor = record("vendor" to null, "currency" to "USD", "line_items" to listOf(10.0), "stated_total" to 10.0, "calculated_total" to 10.0, "provenance" to mapOf("currency" to "10.00 USD", "stated_total" to "Total due: 10.00 USD"))
        assertEquals(emptyList<String>(), kinds(validate(noVendor, docNoVendor)))
        val invented = record("currency" to "USD", "line_items" to listOf(10.0), "stated_total" to 10.0, "calculated_total" to 10.0,
            "provenance" to mapOf("vendor" to "Invoice from Acme Tools", "currency" to "10.00 USD", "stated_total" to "Total due: 10.00 USD"))
        assertEquals(listOf("ungrounded:vendor"), kinds(validate(invented, docNoVendor)))
        assertEquals(listOf("ungrounded:stated_total"), kinds(validate(record("provenance" to mapOf("vendor" to "Invoice from Acme Tools", "currency" to "120.50 EUR")), doc)))
        assertEquals(listOf("ungrounded:vendor"), kinds(validate(record("provenance" to mapOf("vendor" to "", "currency" to "120.50 EUR", "stated_total" to "Total due: 120.50 EUR")), doc)))
    }

    @Test
    fun e2_aRetryCarriesTheOriginalDocumentTheFailedRecordAndOnlyTheErrorsASecondLookCanFix() {
        val bad = record("vendor" to "Acme Corp", "provenance" to mapOf("vendor" to "Invoice from Acme Corp", "currency" to "120.50 EUR", "stated_total" to "Total due: 120.50 EUR"))
        val s = Scripted(listOf(bad, good()))
        val result = run(s, doc, listOf("vendor", "currency"))
        assertEquals("valid", result["status"])
        assertEquals(2, result["attempts"])
        assertEquals(2, s.calls.size)
        assertEquals(mapOf("document" to doc, "feedback" to null), s.calls[0])
        assertEquals(doc, s.calls[1]["document"])
        @Suppress("UNCHECKED_CAST")
        val feedback = s.calls[1]["feedback"] as Map<String, Any?>
        assertEquals(bad, feedback["previous"])
        assertEquals(listOf("ungrounded:vendor"), kinds(errorsOf(s.calls[1])))
        assertTrue(errorsOf(s.calls[1]).all { (it["message"] as String).isNotEmpty() })
        val mixed = record("vendor" to "Acme Corp", "currency" to "unclear", "provenance" to mapOf("vendor" to "Invoice from Acme Corp", "stated_total" to "Total due: 120.50 EUR"))
        val t = Scripted(listOf(mixed, good()))
        run(t, doc, listOf("currency"))
        assertEquals(listOf("ungrounded:vendor"), kinds(errorsOf(t.calls[1])), "the absent currency is not something a second look can fix")
    }

    @Test
    fun e3_aRequiredValueThatTheModelReportsAsAbsentIsNotRetriedAndGoesToReview() {
        val nothing = record("stated_total" to null, "provenance" to mapOf("vendor" to "Invoice from Acme Tools", "currency" to "120.50 EUR"))
        val s = Scripted(listOf(nothing, good()))
        val result = run(s, doc, listOf("stated_total"))
        assertEquals("needs_review", result["status"])
        assertEquals(1, result["attempts"])
        assertEquals(1, s.calls.size)
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("absent:stated_total"), kinds(result["errors"] as List<Map<String, Any?>>))
        assertEquals("valid", run(Scripted(listOf(nothing)), doc)["status"], "a value that is not required may stay null")
    }

    @Test
    fun e4_retriesStopAfterTheLimitAndTheDocumentIsMarkedFailed() {
        val bad = record("currency" to "dollars")
        val s = Scripted(listOf(bad))
        var result = run(s, doc)
        assertEquals("failed", result["status"])
        assertEquals(3, result["attempts"])
        assertEquals(3, s.calls.size)
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("syntax:currency"), kinds(result["errors"] as List<Map<String, Any?>>))
        result = run(Scripted(listOf(bad)), doc, maxRetries = 0)
        assertEquals("failed", result["status"])
        assertEquals(1, result["attempts"])
        result = run(Scripted(listOf(bad, good())), doc, maxRetries = 1)
        assertEquals("valid", result["status"])
        assertEquals(2, result["attempts"])
        assertEquals("failed", run(Scripted(listOf(mapOf("vendor" to "x"))), doc)["status"])
    }

    @Test
    fun e5_aTotalThatDiffersFromTheLineItemsIsASemanticErrorAndAFlaggedConflictGoesToReviewWithoutARetry() {
        assertEquals(listOf("semantic:calculated_total"), kinds(validate(record("calculated_total" to 100.0, "stated_total" to 100.0), doc)))
        assertEquals(listOf("semantic:stated_total"), kinds(validate(record("stated_total" to 130.0), doc)))
        val flagged = record("stated_total" to 130.0, "conflict_detected" to true)
        assertEquals(emptyList<String>(), kinds(validate(flagged, doc)), "a conflict the model flagged is information, not an error")
        val s = Scripted(listOf(flagged))
        val result = run(s, doc)
        assertEquals("needs_review", result["status"])
        assertEquals(1, result["attempts"])
        assertEquals(emptyList<Any?>(), result["errors"])
        assertEquals(1, s.calls.size)
    }

    @Test
    fun e6_currencyTakesUnclearAndOtherWithADetailAndRejectsAnythingElseAsASyntaxError() {
        val noCurrency = record("currency" to "unclear", "provenance" to mapOf("vendor" to "Invoice from Acme Tools", "stated_total" to "Total due: 120.50 EUR"))
        assertEquals(emptyList<String>(), kinds(validate(noCurrency, doc)))
        val chf = "Invoice from Acme Tools.\nTotal due: 120.50 CHF"
        val other = record("currency" to "other", "currency_detail" to "CHF", "provenance" to mapOf("vendor" to "Invoice from Acme Tools", "currency" to "120.50 CHF", "stated_total" to "Total due: 120.50 CHF"))
        assertEquals(emptyList<String>(), kinds(validate(other, chf)))
        assertEquals(listOf("syntax:currency_detail"), kinds(validate(other + ("currency_detail" to null), chf)))
        assertEquals(listOf("syntax:currency_detail"), kinds(validate(other + ("currency_detail" to "  "), chf)))
        assertEquals(listOf("syntax:currency"), kinds(validate(record("currency" to "dollars"), doc)))
        assertEquals(listOf("syntax:line_items"), kinds(validate(record("line_items" to "100"), doc)))
        assertEquals(listOf("syntax:provenance"), kinds(validate(good() - "provenance", doc)))
    }

    private fun chunk(vararg over: Pair<String, Any?>): Map<String, Any?> = linkedMapOf<String, Any?>("vendor" to null, "currency" to "unclear", "currency_detail" to null, "line_items" to emptyList<Double>(),
        "stated_total" to null, "calculated_total" to 0, "conflict_detected" to false, "provenance" to emptyMap<String, Any?>()).also { it.putAll(over) }

    @Test
    fun e7_chunkResultsMergeByKeepingTheFirstValueAndRecordingAConflictWhenTwoChunksDisagree() {
        val one = chunk("vendor" to "Acme Tools", "line_items" to listOf(100.0), "provenance" to mapOf("vendor" to "Invoice from Acme Tools"))
        val two = chunk("currency" to "EUR", "line_items" to listOf(20.5), "stated_total" to 120.5, "provenance" to mapOf("currency" to "120.50 EUR", "stated_total" to "Total due: 120.50 EUR"))
        val three = chunk("vendor" to "Acme Tool Ltd", "stated_total" to 120.5, "provenance" to mapOf("vendor" to "Acme Tool Ltd", "stated_total" to "Total due: 120.50 EUR"))
        val merged = mergeChunks(listOf(one, two, three))
        assertNotNull(merged, "mergeChunks returned nothing")
        assertEquals(mapOf("vendor" to "Acme Tools", "currency" to "EUR", "currency_detail" to null, "line_items" to listOf(100.0, 20.5), "stated_total" to 120.5, "calculated_total" to 120.5, "conflict_detected" to true,
            "provenance" to mapOf("vendor" to "Invoice from Acme Tools", "currency" to "120.50 EUR", "stated_total" to "Total due: 120.50 EUR"), "conflicts" to listOf("vendor")), merged)
        val clean = mergeChunks(listOf(one, two))!!
        assertEquals(emptyList<Any?>(), clean["conflicts"])
        assertEquals(false, clean["conflict_detected"])
        assertEquals("Acme Tools", clean["vendor"])
        assertNull(mergeChunks(listOf(chunk(), chunk()))!!["vendor"])
        assertEquals(true, mergeChunks(listOf(chunk("conflict_detected" to true)))!!["conflict_detected"])
    }

    @Test
    fun e8_accuracyCountsEveryDocumentAndNotOnlyTheValidatedOnes() {
        val labels = (0 until 10).associate { "d$it" to mapOf<String, Any?>("vendor" to "V$it", "stated_total" to it.toDouble()) }
        val results = linkedMapOf<String, Map<String, Any?>>()
        for (i in 0 until 5) results["d$i"] = mapOf("status" to "valid", "record" to mapOf("vendor" to "V$i", "stated_total" to i.toDouble()))
        results["d5"] = mapOf("status" to "valid", "record" to mapOf("vendor" to "wrong", "stated_total" to 5.0))
        results["d6"] = mapOf("status" to "needs_review", "record" to mapOf("vendor" to "V6", "stated_total" to 6.0))
        results["d7"] = mapOf("status" to "needs_review", "record" to mapOf("vendor" to "V7", "stated_total" to 7.0))
        results["d8"] = mapOf("status" to "failed", "record" to mapOf("vendor" to "V8", "stated_total" to 8.0))
        val report = accuracy(results, labels)
        assertNotNull(report, "accuracy returned nothing")
        assertEquals(mapOf("all_documents" to 0.5, "validated_only" to 0.83, "validated" to 6, "total" to 10), report)
        assertEquals(mapOf("all_documents" to 0.0, "validated_only" to 0.0, "validated" to 0, "total" to 0), accuracy(emptyMap(), emptyMap()))
    }

    private fun choice(toolChoice: Map<String, Any?>, verify: Boolean): Map<String, Any?> = mapOf("tool_choice" to toolChoice, "strict" to true, "verify_reply" to verify)

    @Test
    fun e9_theRequestForcesAToolWhereTheModelAllowsItAndFallsBackToAutoWithAReplyCheckWhereItDoesNot() {
        val two = listOf("extract_invoice", "extract_receipt")
        assertEquals(choice(mapOf("type" to "any"), false), requestChoice("claude-haiku-4-5", two))
        assertEquals(choice(mapOf("type" to "tool", "name" to "extract_invoice"), false), requestChoice("claude-haiku-4-5", listOf("extract_invoice")))
        assertEquals(choice(mapOf("type" to "tool", "name" to "extract_metadata"), false), requestChoice("claude-haiku-4-5", two, "extract_metadata"))
        for (model in listOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1")) {
            assertEquals(choice(mapOf("type" to "auto"), true), requestChoice(model, two))
            assertEquals(choice(mapOf("type" to "auto"), true), requestChoice(model, two, "extract_metadata"))
        }
    }
}
