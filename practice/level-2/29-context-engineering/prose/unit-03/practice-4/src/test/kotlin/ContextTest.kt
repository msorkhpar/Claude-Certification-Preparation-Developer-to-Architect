import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContextTest {
    private fun user(text: String): Map<String, Any?> = mapOf("role" to "user", "content" to text)
    private fun said(text: String): Map<String, Any?> = mapOf("role" to "assistant", "content" to listOf(mapOf("type" to "text", "text" to text)))
    private fun call(id: String, name: String, vararg input: Pair<String, Any?>): Map<String, Any?> =
        mapOf("role" to "assistant", "content" to listOf(mapOf("type" to "tool_use", "id" to id, "name" to name, "input" to linkedMapOf(*input))))
    private fun result(id: String, text: String, error: Boolean = false): Map<String, Any?> {
        val block = linkedMapOf<String, Any?>("type" to "tool_result", "tool_use_id" to id, "content" to text)
        if (error) block["is_error"] = true
        return mapOf("role" to "user", "content" to listOf(block))
    }

    @Suppress("UNCHECKED_CAST")
    private fun blocksOf(messages: List<Map<String, Any?>>?): List<Map<String, Any?>> =
        (messages ?: emptyList()).flatMap { if (it["content"] is List<*>) it["content"] as List<Map<String, Any?>> else emptyList() }

    private fun resultBlocks(messages: List<Map<String, Any?>>?) = blocksOf(messages).filter { it["type"] == "tool_result" }

    /** Tool results without their call, and calls without their result. */
    private fun orphans(messages: List<Map<String, Any?>>?): List<Any?> {
        val calls = blocksOf(messages).filter { it["type"] == "tool_use" }.map { it["id"] }.toSet()
        val answered = resultBlocks(messages).map { it["tool_use_id"] }.toSet()
        return ((calls - answered) + (answered - calls)).map { it.toString() }.sorted()
    }

    @Suppress("UNCHECKED_CAST")
    private fun firstText(m: Map<String, Any?>): String {
        val content = m["content"] ?: return "no result"
        return if (content is String) content else (content as List<Map<String, Any?>>)[0]["text"] as String
    }

    private fun orEmpty(messages: List<Map<String, Any?>>?): List<Map<String, Any?>> = messages ?: listOf(mapOf("no" to "result"))

    private fun conversation() = listOf(user("find the invoice"), call("t1", "search", "q" to "invoice"), result("t1", "A".repeat(400)), call("t2", "read", "doc" to "seven"), result("t2", "B".repeat(400), true),
        call("t3", "search", "q" to "total"), result("t3", "C".repeat(400)), call("t4", "calc", "expr" to "1+1"), result("t4", "D".repeat(40)), said("The total is 2."))

    private fun turnsConversation() = listOf(user("first question"), said("first answer " + "x".repeat(80)),
        user("second question"), call("a1", "search"), result("a1", "R".repeat(300)), said("second answer"),
        user("third question"), said("third answer " + "y".repeat(60)),
        user("fourth question"), call("a2", "search"), result("a2", "S".repeat(100)), said("fourth answer"))

    private class Summariser(vararg texts: String) : (List<Map<String, Any?>>) -> String {
        val texts = texts.toMutableList()
        val calls = mutableListOf<List<Map<String, Any?>>>()
        @Suppress("UNCHECKED_CAST")
        override fun invoke(messages: List<Map<String, Any?>>): String {
            calls += Json.parse(Json.stringify(messages)) as List<Map<String, Any?>>
            return texts.removeAt(0)
        }
    }

    @Test
    fun m1_anOverBudgetConversationBecomesASummaryAndTheNewestTurn() {
        val messages = turnsConversation()
        val budget = countTokens(messages) / 2
        val s = Summariser("The user asked three things.")
        val done = orEmpty(compact(messages, budget, s, 1))
        assertEquals(1, s.calls.size)
        assertEquals(messages.subList(0, 8), s.calls[0])
        assertEquals(listOf("user", "assistant", "user", "assistant"), done.map { it["role"] })
        assertEquals(listOf(mapOf("type" to "text", "text" to "<summary>\nThe user asked three things.\n</summary>"), mapOf("type" to "text", "text" to "fourth question")), done[0]["content"])
        assertEquals(messages.subList(9, 12), done.drop(1))
        assertTrue(countTokens(done) <= budget)
    }

    @Test
    fun e1_oldToolResultsAreClearedButTheirCallsAndFlagsStay() {
        val messages = conversation()
        val before = conversation()
        val cleared = orEmpty(clearToolResults(messages, 2))
        assertEquals(before, messages)
        assertEquals(listOf("t1/true/false", "t2/true/true", "t3/false/false", "t4/false/false"),
            resultBlocks(cleared).map { "${it["tool_use_id"]}/${it["content"] == "[cleared]"}/${it["is_error"] ?: false}" })
        assertEquals(messages.filter { it["role"] == "assistant" }, cleared.filter { it["role"] == "assistant" })
        assertEquals(listOf("gone", "gone", "gone", "gone"), resultBlocks(clearToolResults(messages, 0, emptyList(), "gone")).map { it["content"] })
        assertEquals(listOf("t1/true", "t2/false", "t3/false", "t4/false"), resultBlocks(clearToolResults(messages, 2, listOf("read"))).map { "${it["tool_use_id"]}/${it["content"] == "[cleared]"}" })
    }

    @Test
    fun e2_theWindowDropsWholeTurnsAndNeverSplitsAToolCallFromItsResult() {
        val messages = turnsConversation()
        val total = countTokens(messages)
        var budget = total
        while (budget > 0) {
            val kept = orEmpty(window(messages, budget))
            assertEquals(emptyList<Any?>(), orphans(kept), "budget $budget")
            assertTrue(listOf("first question", "second question", "third question", "fourth question").contains(firstText(kept[0])), "budget $budget")
            budget -= 7
        }
        val two = orEmpty(window(messages, countTokens(messages.subList(6, 12)) + 1))
        assertEquals(listOf("third question", "fourth question"), two.filter { it["role"] == "user" && it["content"] is String }.map { it["content"] })
        assertEquals(messages, orEmpty(window(messages, total)))
        assertEquals(messages.subList(8, 12), orEmpty(window(messages, 1)))
        val expected = messages.subList(0, 2) + messages.subList(8, 12)
        assertEquals(expected, orEmpty(window(messages, countTokens(expected) + 1, true)))
    }

    @Test
    fun e3_aConversationWithinBudgetOrWithNothingOlderIsLeftAlone() {
        val messages = turnsConversation()
        val s = Summariser("unused")
        assertEquals(messages, orEmpty(compact(messages, countTokens(messages), s)))
        val single = listOf(user("one long question " + "z".repeat(400)), said("answer"))
        assertEquals(single, orEmpty(compact(single, 5, s)))
        assertEquals(messages, orEmpty(compact(messages, 10_000, s, 2)))
        assertEquals(0, s.calls.size)
    }

    @Test
    fun e4_aSecondCompactionFoldsTheEarlierSummaryIntoTheNewOne() {
        val messages = turnsConversation()
        val s = Summariser("SUMMARY ONE", "SUMMARY TWO")
        val first = orEmpty(compact(messages, 60, s, 1))
        val grown = first + listOf(said("noted"), user("fifth question " + "q".repeat(200)), said("fifth answer " + "w".repeat(200)))
        val second = orEmpty(compact(grown, 60, s, 1))
        assertEquals(2, s.calls.size)
        val older = if (s.calls.size > 1) s.calls[1] else emptyList()
        assertTrue(blocksOf(older).any { it["text"].toString().startsWith("<summary>\nSUMMARY ONE") })
        val summaries = blocksOf(second).filter { it["type"] == "text" && (it["text"] as String).startsWith("<summary>") }.map { it["text"] }
        assertEquals(listOf("<summary>\nSUMMARY TWO\n</summary>"), summaries)
    }

    private val docs: List<Map<String, Any?>> = listOf(mapOf("title" to "Policy", "text" to "The grass is green. The sky is blue."), mapOf("title" to "Notes", "text" to "Water is essential for life."))

    private fun cite(doc: Int, start: Int, end: Int, cited: String): Map<String, Any?> =
        mapOf("type" to "char_location", "cited_text" to cited, "document_index" to doc, "start_char_index" to start, "end_char_index" to end)

    private fun textBlock(text: String, vararg citations: Map<String, Any?>): Map<String, Any?> =
        if (citations.isEmpty()) mapOf("type" to "text", "text" to text) else mapOf("type" to "text", "text" to text, "citations" to citations.toList())

    @Test
    fun e5_aCitationThatDoesNotMatchItsDocumentIsReported() {
        val good = textBlock("Grass is green.", cite(0, 0, 19, "The grass is green."))
        assertEquals(emptyList<Map<String, Any?>>(), orEmpty(verifyCitations(listOf(good, textBlock("No source.")), docs)))
        val page = mapOf("type" to "page_location", "cited_text" to "The", "document_index" to 0, "start_page_number" to 1, "end_page_number" to 2)
        val block = textBlock("x", cite(0, 0, 19, "The grass is green."), cite(0, 0, 19, "The grass is red."), cite(5, 0, 3, "The"), cite(0, 20, 99, "The sky is blue."), cite(1, 3, 3, ""), page)
        val got = orEmpty(verifyCitations(listOf(block), docs))
        assertEquals(listOf("0/1/text_mismatch", "0/2/unknown_document", "0/3/bad_range", "0/4/bad_range", "0/5/unsupported_type"), got.map { "${it["block"]}/${it["citation"]}/${it["problem"]}" })
        assertEquals(emptyList<Map<String, Any?>>(), orEmpty(verifyCitations(listOf(textBlock("x", cite(0, 20, 36, "The sky is blue."))), docs)))
    }

    @Test
    fun e6_footnotesNumberEachDistinctSourceOnceInOrderOfAppearance() {
        val blocks = listOf(textBlock("Grass is green. ", cite(0, 0, 19, "The grass is green.")), textBlock("Water matters. ", cite(1, 0, 28, "Water is essential for life.")),
            textBlock("Again, green.", cite(0, 0, 19, "The grass is green.")), textBlock(" No source here."))
        assertEquals("Grass is green. [1]Water matters. [2]Again, green.[1] No source here.\n\nSources:\n[1] Policy: \"The grass is green.\"\n[2] Notes: \"Water is essential for life.\"", footnotes(blocks, docs))
        assertEquals("Plain.", footnotes(listOf(textBlock("Plain.")), docs))
    }
}
