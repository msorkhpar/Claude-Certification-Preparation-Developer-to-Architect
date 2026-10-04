import static org.junit.jupiter.api.Assertions.*;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotesExampleTest {
    private static McpSchema.CallToolResult call(SyncToolSpecification tool, Map<String, Object> arguments) {
        return tool.callHandler().apply(null, new McpSchema.CallToolRequest(tool.tool().name(), arguments, null));
    }

    @Test
    void aBlankTitleIsAToolErrorAndDoesNotUseAnId() {
        NotesExample.Notes notes = new NotesExample.Notes();
        McpSchema.CallToolResult bad = call(notes.addNote(), Map.of("title", " ", "text", "x"));
        McpSchema.CallToolResult good = call(notes.addNote(), Map.of("title", "A", "text", "x"));
        assertTrue(bad.isError());
        assertTrue(NotesExample.words(bad).contains("title is required"));
        assertFalse(good.isError());
        assertEquals("Saved note 1: A", NotesExample.words(good));
    }

    @Test
    void searchIsCaseInsensitiveAndSaysWhenNothingMatches() {
        NotesExample.Notes notes = new NotesExample.Notes();
        call(notes.addNote(), Map.of("title", "Plan", "text", "x"));
        assertEquals("1. Plan", NotesExample.words(call(notes.searchNotes(), Map.of("query", "PLAN"))));
        assertEquals("No notes match \"zzz\"", NotesExample.words(call(notes.searchNotes(), Map.of("query", "zzz"))));
    }

    @Test
    void theCountResourceAndThePromptFollowTheNotes() {
        NotesExample.Notes notes = new NotesExample.Notes();
        var count = notes.count();
        var read = new McpSchema.ReadResourceRequest("notes://count");
        String before = ((McpSchema.TextResourceContents) count.readHandler().apply(null, read).contents().get(0)).text();
        call(notes.addNote(), Map.of("title", "A", "text", "x"));
        String after = ((McpSchema.TextResourceContents) count.readHandler().apply(null, read).contents().get(0)).text();
        var prompt = notes.reviewNotes().promptHandler().apply(null, new McpSchema.GetPromptRequest("review_notes", Map.of("tone", "formal"), null));
        assertEquals("0 notes", before);
        assertEquals("1 note", after);
        assertEquals("Review these notes in a formal tone:\n- A", ((McpSchema.TextContent) prompt.messages().get(0).content()).text());
    }

    @Test
    void theSchemaMarksOnlyTheQueryAsRequired() {
        Map<String, Object> schema = new NotesExample.Notes().searchNotes().tool().inputSchema();
        assertEquals(java.util.List.of("query"), schema.get("required"));
        assertEquals(java.util.List.of("query", "limit"), java.util.List.copyOf(((Map<?, ?>) schema.get("properties")).keySet()));
    }
}
