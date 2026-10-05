// A notes server for the Model Context Protocol, over stdio. See ../../statement.md.
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotesServer {
    private static final System.Logger LOG = System.getLogger(NotesServer.class.getName());
    static final int MAX_TEXT = 500;
    record Note(String title, String text) {}
    static final List<Note> NOTES = new ArrayList<>(); // the id of a note is its position, counting from 1

    static CallToolResult ok(String text) {
        return CallToolResult.builder().addTextContent(text).isError(false).build();
    }

    static CallToolResult fail(String message) {
        return CallToolResult.builder().addTextContent(message).isError(true).build();
    }

    // GAP 1 of 7 (unlocks e2 and e7): refuse a bad note.
    // Receives the title and the text, both already stripped. Returns the error message, or null when they are fine: "title is required" for an empty
    // title, "text is required" for an empty text and "text is too long (max " + MAX_TEXT + ")" for a text longer than MAX_TEXT, checked in that order.
    // Example: noteError("", "x") -> "title is required"; noteError("T", "x") -> null
    static String noteError(String title, String text) {
        return null;
    }

    // GAP 2 of 7 (unlocks e2): refuse a bad search.
    // Receives the stripped query and the limit. Returns the error message, or null when they are fine: "query is required" for an empty query and
    // "limit must be between 1 and 20" for a limit outside 1 to 20, in that order.
    // Example: searchError("x", 21) -> "limit must be between 1 and 20"
    static String searchError(String query, int limit) {
        return null;
    }

    // GAP 3 of 7 (unlocks m1 and e3): the notes that match a search.
    // Receives the stripped query. Returns the lines "{id}. {title}" of the notes in NOTES (ids count from 1) whose title or text contains the query
    // in any letter case, in id order.
    // Example: with notes ("Alpha", "x") and ("beta", "ALPHA again"), findHits("alpha") -> ["1. Alpha", "2. beta"]
    static List<String> findHits(String query) {
        return new ArrayList<>();
    }

    // GAP 4 of 7 (unlocks e3): the answer of a search.
    // Receives the hit lines, the limit and the stripped query. Returns at most limit lines joined by newlines; with no hits the sentence
    // No notes match "<query>".
    // Example: formatHits(["1. A", "2. B"], 1, "a") -> "1. A"; formatHits([], 5, "zeta") -> No notes match "zeta"
    static String formatHits(List<String> hits, int limit, String query) {
        return "";
    }

    // GAP 5 of 7 (unlocks e5): the text of the count resource.
    // Receives the number of notes. Returns "0 notes", "1 note", "2 notes" and so on.
    // Example: countText(1) -> "1 note"
    static String countText(int count) {
        return "";
    }

    // GAP 6 of 7 (unlocks m1 and e5): the text of one note.
    // Receives the id from the URI as a string. Returns the title, an empty line, then the text. An id that is not a whole number of an existing note
    // ("0", "3" of two notes, "abc") throws McpError.builder(-32602).message("No note " + id).build().
    // Example: with one note ("Plan", "ship it"), noteText("1") -> "Plan\n\nship it"
    static String noteText(String id) {
        return "";
    }

    // GAP 7 of 7 (unlocks e6): the text of the review prompt.
    // With no notes it is "There are no notes to review."; otherwise "Review these notes in a <tone> tone:" and one line "- <title>" per note,
    // each after a newline.
    // Example: with one note titled "Plan", reviewText("brief") -> "Review these notes in a brief tone:\n- Plan"
    static String reviewText(String tone) {
        return "";
    }

    static CallToolResult addNote(Map<String, Object> args) {
        LOG.log(System.Logger.Level.DEBUG, "addNote input: {0}", args);
        String title = String.valueOf(args.getOrDefault("title", "")).strip(), text = String.valueOf(args.getOrDefault("text", "")).strip();
        String error = noteError(title, text);
        if (error != null) return fail(error);
        NOTES.add(new Note(title, text));
        return ok("Saved note " + NOTES.size() + ": " + title);
    }

    static CallToolResult searchNotes(Map<String, Object> args) {
        String query = String.valueOf(args.getOrDefault("query", "")).strip();
        int limit = ((Number) args.getOrDefault("limit", 5)).intValue();
        String error = searchError(query, limit);
        if (error != null) return fail(error);
        return ok(formatHits(findHits(query), limit, query));
    }

    public static void main(String[] args) throws InterruptedException {
        McpJsonMapper json = McpJsonDefaults.getMapper();
        Tool add = Tool.builder().name("add_note").description("Save a note with a title and a text.")
                .inputSchema(json, "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"},\"text\":{\"type\":\"string\"}},\"required\":[\"title\",\"text\"]}")
                .annotations(ToolAnnotations.builder().readOnlyHint(false).destructiveHint(false).idempotentHint(false).build()).build();
        Tool search = Tool.builder().name("search_notes").description("Find notes whose title or text contains the query.")
                .inputSchema(json, "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"},\"limit\":{\"type\":\"integer\",\"default\":5}},\"required\":[\"query\"]}")
                .annotations(ToolAnnotations.builder().readOnlyHint(true).build()).build();
        McpSchema.Resource count = McpSchema.Resource.builder().uri("notes://count").name("count").description("How many notes there are.").mimeType("text/plain").build();
        McpSchema.ResourceTemplate noteTemplate = McpSchema.ResourceTemplate.builder().uriTemplate("notes://note/{id}").name("note").description("One note by id.").mimeType("text/plain").build();
        McpSchema.Prompt review = new McpSchema.Prompt("review_notes", "Ask for a review of the notes.", List.of(new McpSchema.PromptArgument("tone", null, false)));

        McpServer.sync(new StdioServerTransportProvider(json))
                .serverInfo("notes", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).resources(false, false).prompts(false).build())
                .tools(new SyncToolSpecification(add, (exchange, request) -> addNote(request.arguments())),
                        new SyncToolSpecification(search, (exchange, request) -> searchNotes(request.arguments())))
                .resources(new SyncResourceSpecification(count, (exchange, request) -> new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(request.uri(), "text/plain", countText(NOTES.size()))))))
                .resourceTemplates(new SyncResourceTemplateSpecification(noteTemplate, (exchange, request) -> {
                    String id = request.uri().substring("notes://note/".length());
                    return new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents(request.uri(), "text/plain", noteText(id))));
                }))
                .prompts(new SyncPromptSpecification(review, (exchange, request) -> {
                    Object tone = request.arguments() == null ? null : request.arguments().get("tone");
                    String text = reviewText(tone == null ? "brief" : String.valueOf(tone));
                    return new McpSchema.GetPromptResult(null, List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(text))));
                }))
                .build();
        Thread.currentThread().join(); // the transport reads stdin on its own threads; keep the process alive until the client closes it
    }
}
