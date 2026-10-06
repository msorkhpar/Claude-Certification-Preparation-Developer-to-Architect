import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtractorTest {
    private val doc = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business."
    private val schema: Map<String, Any?> = mapOf(
        "type" to "object", "required" to listOf("vendor", "total", "currency", "evidence"),
        "properties" to linkedMapOf(
            "vendor" to mapOf("type" to "string"), "total" to mapOf("type" to "number", "minimum" to 0),
            "currency" to mapOf("type" to "string", "enum" to listOf("USD", "EUR", "GBP")),
            "items" to mapOf("type" to "array", "items" to mapOf("type" to "object", "required" to listOf("name", "qty"),
                "properties" to linkedMapOf("name" to mapOf("type" to "string"), "qty" to mapOf("type" to "integer", "minimum" to 1)))),
            "evidence" to mapOf("type" to "string")),
        "additionalProperties" to false)
    private val good: Map<String, Any?> = linkedMapOf("vendor" to "Acme Tools", "total" to 120.5, "currency" to "EUR", "evidence" to "Total due: 120.50 EUR")

    private fun reply(text: String, stopReason: String = "end_turn"): Map<String, Any?> = mapOf("id" to "msg_illustrative", "type" to "message", "role" to "assistant",
        "model" to "claude-sonnet-5-5", "content" to listOf(mapOf("type" to "text", "text" to text)), "stop_reason" to stopReason, "usage" to mapOf("input_tokens" to 1, "output_tokens" to 1))

    /** A hand-written, illustrative model: returns the next reply and records the messages it was sent. */
    private class Scripted(vararg replies: Map<String, Any?>) : (List<Map<String, Any?>>) -> Map<String, Any?> {
        val replies = replies.toMutableList()
        val seen = mutableListOf<List<Map<String, Any?>>>()
        override fun invoke(messages: List<Map<String, Any?>>): Map<String, Any?> {
            seen += messages.toList()
            return replies.removeAt(0)
        }
    }

    /** The parsed value, or the string ParseError when the text holds no JSON object. */
    private fun parsed(text: String): Any? = try {
        parseJson(text)
    } catch (e: ParseError) {
        "ParseError"
    }

    private fun paths(errors: List<Map<String, Any?>>?): List<Any?> = errors?.map { it["path"] } ?: listOf("no result")
    private fun at(result: Map<String, Any?>?, key: String): Any? = if (result == null) "no result" else result[key]
    private fun content(m: Scripted, call: Int, message: Int) = m.seen[call][message]["content"] as String
    private fun noProblems(errors: List<Map<String, Any?>>?): List<Any?> = errors ?: listOf("no result")

    @Test
    fun m1_aValidReplyIsReturnedAfterOneCall() {
        val m = Scripted(reply(Json.stringify(good)))
        val r = extract(m, doc, schema, 3, listOf("evidence"))
        assertEquals(listOf("ok", good, 1, emptyList<Any?>()), listOf(at(r, "status"), at(r, "value"), at(r, "attempts"), at(r, "errors")))
        assertEquals(1, m.seen.size)
        assertEquals("user", m.seen[0][0]["role"])
        assertTrue(content(m, 0, 0).contains(doc))
    }

    @Test
    fun e1_jsonIsFoundInFencesAndProseAndBadTextIsRetried() {
        assertEquals(good, parsed("Here you go:\n```json\n" + Json.stringify(good) + "\n```\nHope that helps."))
        assertEquals(good, parsed("Sure! " + Json.stringify(good) + " Done."))
        for (bad in listOf("I cannot find an invoice.", "{not json}", "```json\n```")) assertThrows(ParseError::class.java, { parseJson(bad) }, bad)
        val m = Scripted(reply("I think the total is 120.50"), reply(Json.stringify(good)))
        val r = extract(m, doc, schema)
        assertEquals(listOf("ok", 2), listOf(at(r, "status"), at(r, "attempts")))
        assertTrue(content(m, 1, 2).contains("$"))
    }

    @Test
    fun e2_everySchemaViolationIsListedAndSentBackToTheModel() {
        val bad = linkedMapOf<String, Any?>("vendor" to "Acme Tools", "total" to "120.50", "currency" to "usd", "extra" to 1L, "evidence" to "x")
        assertEquals(listOf("\$.total", "\$.currency", "\$.extra"), paths(validate(schema, bad)))
        assertEquals(listOf("\$.vendor", "\$.evidence", "\$.total"), paths(validate(schema, mapOf("total" to -1L, "currency" to "EUR"))))
        val items = good + ("items" to listOf(mapOf("name" to "bolt", "qty" to 2L), mapOf("name" to "nut", "qty" to 0L), mapOf("name" to "gear")))
        assertEquals(listOf("\$.items[1].qty", "\$.items[2].qty"), paths(validate(schema, items)))
        assertEquals(emptyList<Any?>(), noProblems(validate(schema, good)))
        val first = Json.stringify(bad)
        val m = Scripted(reply(first), reply(Json.stringify(good)))
        val r = extract(m, doc, schema)
        assertEquals(listOf("ok", 2), listOf(at(r, "status"), at(r, "attempts")))
        assertEquals(listOf("user", "assistant", "user"), m.seen[1].map { it["role"] })
        assertEquals(first, content(m, 1, 1))
        for (p in listOf("\$.total", "\$.currency", "\$.extra")) assertTrue(content(m, 1, 2).contains(p), p)
    }

    @Test
    fun e3_theNumberOfAttemptsIsBounded() {
        val wrong = reply(Json.stringify(mapOf("vendor" to 1L)))
        val m = Scripted(wrong, wrong, wrong, wrong, wrong)
        val r = extract(m, doc, schema, 3)
        assertEquals(listOf("failed", 3), listOf(at(r, "status"), at(r, "attempts")))
        assertNull(if (r == null) "no result" else r["value"])
        assertEquals(3, m.seen.size)
        @Suppress("UNCHECKED_CAST") assertTrue(paths(r?.get("errors") as List<Map<String, Any?>>?).contains("\$.vendor"))
        val once = Scripted(wrong, wrong, wrong)
        assertEquals("failed", at(extract(once, doc, schema, 1), "status"))
        assertEquals(1, once.seen.size)
    }

    @Test
    fun e4_aRefusalOrACutOffReplyIsNotRetried() {
        val refusal = Scripted(reply("I can't help with that.", "refusal"), reply(Json.stringify(good)))
        var r = extract(refusal, doc, schema)
        assertEquals(listOf("refused", 1), listOf(at(r, "status"), at(r, "attempts")))
        assertNull(if (r == null) "no result" else r["value"])
        assertEquals(1, refusal.seen.size)
        val cut = Scripted(reply("{\"vendor\": \"Acme", "max_tokens"), reply(Json.stringify(good)))
        r = extract(cut, doc, schema)
        assertEquals(listOf("truncated", 1), listOf(at(r, "status"), at(r, "attempts")))
        assertEquals(1, cut.seen.size)
    }

    @Test
    fun e5_aQuoteThatIsNotInTheDocumentIsRejected() {
        val invented = good + ("evidence" to "Total due: 999.00 USD")
        val m = Scripted(reply(Json.stringify(invented)), reply(Json.stringify(good)))
        val r = extract(m, doc, schema, 3, listOf("evidence"))
        assertEquals(listOf("ok", 2), listOf(at(r, "status"), at(r, "attempts")))
        assertTrue(content(m, 1, 2).contains("\$.evidence"))
        val unchecked = Scripted(reply(Json.stringify(invented)))
        assertEquals("ok", at(extract(unchecked, doc, schema), "status"))
    }

    @Test
    fun e6_typesAreExactBooleansAreNotNumbersAndIntegersHaveNoFraction() {
        assertEquals(listOf("$"), paths(validate(mapOf("type" to "integer"), true)))
        assertEquals(listOf("$"), paths(validate(mapOf("type" to "number"), false)))
        assertEquals(listOf("$"), paths(validate(mapOf("type" to "integer"), 2.5)))
        assertEquals(emptyList<Any?>(), noProblems(validate(mapOf("type" to "integer"), 2.0)))
        assertEquals(emptyList<Any?>(), noProblems(validate(mapOf("type" to "number"), 3L)))
        assertEquals(listOf("$"), paths(validate(mapOf("type" to "string"), null)))
        assertEquals(listOf("$[1]"), paths(validate(mapOf("type" to "array", "items" to mapOf("type" to "boolean")), listOf(true, 1L))))
    }
}
