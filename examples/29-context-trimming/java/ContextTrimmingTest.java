import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextTrimmingTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void clearingKeepsTheCallsAndShrinksTheConversation() {
        ArrayNode before = ContextTrimming.conversation();
        ArrayNode after = ContextTrimming.clearToolResults(before, 2, "[cleared]");
        assertTrue(ContextTrimming.tokens(after) < ContextTrimming.tokens(before) * 0.6);
        for (int i = 0; i < before.size(); i++) if (before.get(i).get("role").asText().equals("assistant")) assertEquals(before.get(i), after.get(i));
        assertEquals(3, ContextTrimming.toolResults(after).stream().filter(b -> b.get("content").asText().equals("[cleared]")).count());
        assertEquals(ContextTrimming.conversation(), before);
    }

    @Test
    void aCitationThatMatchesItsDocumentPassesAndAChangedOneIsCaught() {
        ArrayNode blocks = JSON.createArrayNode();
        blocks.add(JSON.valueToTree(Map.of("type", "text", "text", "x", "citations", List.of(ContextTrimming.cite(0, 19)))));
        assertEquals(List.of(), ContextTrimming.verify(blocks, List.of(ContextTrimming.POLICY)));
        ((ObjectNode) blocks.get(0).get("citations").get(0)).put("cited_text", "The grass is red.");
        assertEquals(List.of(Map.of("block", 0, "citation", 0, "problem", "text_mismatch")), ContextTrimming.verify(blocks, List.of(ContextTrimming.POLICY)));
    }

    @Test
    void aSourceCitedTwiceGetsOneNumber() {
        String text = ContextTrimming.footnotes(JSON.valueToTree(ContextTrimming.citedReply().get("content")), List.of("Policy"));
        assertEquals(3, text.split("\\[1\\]", -1).length - 1);
        assertEquals(2, text.split("\\[2\\]", -1).length - 1);
        assertTrue(text.endsWith("[2] Policy: \"Water is essential for life.\""));
    }
}
