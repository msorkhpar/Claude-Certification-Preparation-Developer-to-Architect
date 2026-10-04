import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import harness.Scripted.map
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContextTrimmingTest {
    private val json = ObjectMapper()

    @Test
    fun clearingKeepsTheCallsAndShrinksTheConversation() {
        val before = conversation()
        val after = clearToolResults(before, 2)
        assertTrue(tokens(after) < tokens(before) * 0.6)
        for (i in 0 until before.size()) if (before[i]["role"].asText() == "assistant") assertEquals(before[i], after[i])
        assertEquals(3, toolResults(after).count { it["content"].asText() == "[cleared]" })
        assertEquals(conversation(), before)
    }

    @Test
    fun aCitationThatMatchesItsDocumentPassesAndAChangedOneIsCaught() {
        val blocks = json.createArrayNode().add(json.valueToTree<JsonNode>(map("type", "text", "text", "x", "citations", listOf(cite(0, 19)))))
        assertEquals(emptyList<Map<String, Any>>(), verify(blocks, listOf(POLICY)))
        (blocks[0]["citations"][0] as ObjectNode).put("cited_text", "The grass is red.")
        assertEquals(listOf(map("block", 0, "citation", 0, "problem", "text_mismatch")), verify(blocks, listOf(POLICY)))
    }

    @Test
    fun aSourceCitedTwiceGetsOneNumber() {
        val text = footnotes(json.valueToTree(citedReply()["content"]), listOf("Policy"))
        assertEquals(3, text.split("[1]").size - 1)
        assertEquals(2, text.split("[2]").size - 1)
        assertTrue(text.endsWith("[2] Policy: \"Water is essential for life.\""))
    }
}
