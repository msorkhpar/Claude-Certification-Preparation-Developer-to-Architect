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

    /** Refuse a bad note: the error message, or null when the (stripped) title and text are fine. */
    static String noteError(String title, String text) {
        if (title.isEmpty()) return "title is required";
        if (text.isEmpty()) return "text is required";
        if (text.length() > MAX_TEXT) return "text is too long (max " + MAX_TEXT + ")";
        return null;
    }

    /** Refuse a bad search: the error message, or null when the (stripped) query and the limit are fine. */
    static String searchError(String query, int limit) {
        if (query.isEmpty()) return "query is required";
        if (limit < 1 || limit > 20) return "limit must be between 1 and 20";
        return null;
    }

    /** The lines {id}. {title} of the notes whose title or text contains the query, in any letter case, in id order. */
    static List<String> findHits(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        for (int i = 0; i < NOTES.size(); i++) {
            Note n = NOTES.get(i);
            if (n.title().toLowerCase(Locale.ROOT).contains(needle) || n.text().toLowerCase(Locale.ROOT).contains(needle)) hits.add((i + 1) + ". " + n.title());
        }
        return hits;
    }

    /** The answer of a search: at most limit hit lines joined by newlines, or the no-match sentence. */
    static String formatHits(List<String> hits, int limit, String query) {
        return hits.isEmpty() ? "No notes match \"" + query + "\"" : String.join("\n", hits.subList(0, Math.min(limit, hits.size())));
    }

    /** The text of the count resource: 0 notes, 1 note, 2 notes. */
    static String countText(int count) {
        return count + " note" + (count == 1 ? "" : "s");
    }

    /** The text of one note, or an McpError "No note {id}" when the id is not a whole number of an existing note. */
    static String noteText(String id) {
        if (!id.matches("\\d+") || Integer.parseInt(id) < 1 || Integer.parseInt(id) > NOTES.size()) throw McpError.builder(-32602).message("No note " + id).build();
        Note n = NOTES.get(Integer.parseInt(id) - 1);
        return n.title() + "\n\n" + n.text();
    }

    /** The text of the review prompt. */
    static String reviewText(String tone) {
        if (NOTES.isEmpty()) return "There are no notes to review.";
        return "Review these notes in a " + tone + " tone:\n" + String.join("\n", NOTES.stream().map(n -> "- " + n.title()).toList());
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
