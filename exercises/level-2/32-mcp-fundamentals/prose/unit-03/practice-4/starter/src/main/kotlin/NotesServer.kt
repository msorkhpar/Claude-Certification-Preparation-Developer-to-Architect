// A notes server for the Model Context Protocol, over stdio. See ../../statement.md.
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val log = System.getLogger("notes_server")

const val MAX_TEXT = 500

data class Note(val title: String, val text: String)

val notes = mutableListOf<Note>() // the id of a note is its position, counting from 1

fun ok(text: String) = CallToolResult(content = listOf(TextContent(text)), isError = false)

fun fail(message: String) = CallToolResult(content = listOf(TextContent(message)), isError = true)

// GAP 1 of 9 (unlocks e2 and e7): refuse a bad note.
// Receives the title and the text, both already stripped. Returns the error message, or null when they are fine: "title is required" for an empty
// title, "text is required" for an empty text and "text is too long (max $MAX_TEXT)" for a text longer than MAX_TEXT, checked in that order.
// Example: noteError("", "x") -> "title is required"; noteError("T", "x") -> null
fun noteError(title: String, text: String): String? {
    return null
}

// GAP 2 of 9 (unlocks e2): refuse a bad search.
// Receives the stripped query and the limit. Returns the error message, or null when they are fine: "query is required" for an empty query and
// "limit must be between 1 and 20" for a limit outside 1 to 20, in that order.
// Example: searchError("x", 21) -> "limit must be between 1 and 20"
fun searchError(query: String, limit: Int): String? {
    return null
}

// GAP 3 of 9 (unlocks m1 and e3): the notes that match a search.
// Receives the stripped query. Returns the lines "{id}. {title}" of the notes (ids count from 1) whose title or text contains the query in any
// letter case, in id order.
// Example: with notes ("Alpha", "x") and ("beta", "ALPHA again"), findHits("alpha") -> ["1. Alpha", "2. beta"]
fun findHits(query: String): List<String> {
    return emptyList()
}

// GAP 4 of 9 (unlocks e3): the answer of a search.
// Receives the hit lines, the limit and the stripped query. Returns at most `limit` lines joined by newlines; with no hits the sentence
// No notes match "<query>".
// Example: formatHits(["1. A", "2. B"], 1, "a") -> "1. A"; formatHits([], 5, "zeta") -> No notes match "zeta"
fun formatHits(hits: List<String>, limit: Int, query: String): String {
    return ""
}

// GAP 5 of 9 (unlocks e5): the text of the count resource.
// Receives the number of notes. Returns "0 notes", "1 note", "2 notes" and so on.
// Example: countText(1) -> "1 note"
fun countText(count: Int): String {
    return ""
}

// GAP 6 of 9 (unlocks m1 and e5): the text of one note.
// Receives the id from the URI as a string. Returns the title, an empty line, then the text. An id that is not a whole number of an existing note
// ("0", "3" of two notes, "abc") throws IllegalArgumentException("No note $id").
// Example: with one note ("Plan", "ship it"), noteText("1") -> "Plan\n\nship it"
fun noteText(id: String): String {
    return ""
}

// GAP 7 of 9 (unlocks e6): the text of the review prompt.
// With no notes it is "There are no notes to review."; otherwise "Review these notes in a <tone> tone:" and one line "- <title>" per note,
// each after a newline.
// Example: with one note titled "Plan", reviewText("brief") -> "Review these notes in a brief tone:\n- Plan"
fun reviewText(tone: String): String {
    return ""
}

// GAP 8 of 9 (unlocks e1): how many hits a search returns when the caller gives no limit.
// Takes nothing; returns that number, which is also the default the tool's input schema advertises.
// Example: defaultLimit() -> 5
fun defaultLimit(): Int {
    return 0
}

// GAP 9 of 9 (unlocks e4): the annotations that tell a client search_notes only reads.
// Takes nothing; returns ToolAnnotations with readOnlyHint = true.
// Example: searchAnnotations()?.readOnlyHint -> true
fun searchAnnotations(): ToolAnnotations? {
    return null
}

fun addNote(args: JsonObject?): CallToolResult {
    log.log(System.Logger.Level.DEBUG, "addNote input: {0}", args)
    val title = args?.get("title")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    val text = args?.get("text")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    noteError(title, text)?.let { return fail(it) }
    notes.add(Note(title, text))
    return ok("Saved note ${notes.size}: $title")
}

fun searchNotes(args: JsonObject?): CallToolResult {
    val query = args?.get("query")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    val limit = args?.get("limit")?.jsonPrimitive?.intOrNull ?: defaultLimit()
    searchError(query, limit)?.let { return fail(it) }
    return ok(formatHits(findHits(query), limit, query))
}

fun main() = runBlocking {
    val server = Server(
        Implementation(name = "notes", version = "1.0.0"),
        ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false),
                resources = ServerCapabilities.Resources(subscribe = false, listChanged = false),
                prompts = ServerCapabilities.Prompts(listChanged = false),
            ),
        ),
    )
    server.addTool(
        name = "add_note",
        description = "Save a note with a title and a text.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("title", buildJsonObject { put("type", "string") })
                put("text", buildJsonObject { put("type", "string") })
            },
            required = listOf("title", "text"),
        ),
        toolAnnotations = ToolAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false),
    ) { request -> addNote(request.arguments) }
    server.addTool(
        name = "search_notes",
        description = "Find notes whose title or text contains the query.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject { put("type", "string") })
                put("limit", buildJsonObject { put("type", "integer"); put("default", defaultLimit()) })
            },
            required = listOf("query"),
        ),
        toolAnnotations = searchAnnotations(),
    ) { request -> searchNotes(request.arguments) }
    server.addResource(uri = "notes://count", name = "count", description = "How many notes there are.", mimeType = "text/plain") { request ->
        ReadResourceResult(listOf(TextResourceContents(text = countText(notes.size), uri = request.uri, mimeType = "text/plain")))
    }
    server.addResourceTemplate(uriTemplate = "notes://note/{id}", name = "note", description = "One note by id.", mimeType = "text/plain") { request, variables ->
        val id = variables["id"].orEmpty()
        ReadResourceResult(listOf(TextResourceContents(text = noteText(id), uri = request.uri, mimeType = "text/plain")))
    }
    server.addPrompt(name = "review_notes", description = "Ask for a review of the notes.", arguments = listOf(PromptArgument(name = "tone", required = false))) { request ->
        val tone = request.arguments?.get("tone") ?: "brief"
        val text = reviewText(tone)
        GetPromptResult(messages = listOf(PromptMessage(role = Role.User, content = TextContent(text))))
    }
    val session = server.createSession(StdioServerTransport(System.`in`.asSource().buffered(), System.out.asSink().buffered()) { })
    val done = CompletableDeferred<Unit>()
    session.onClose { done.complete(Unit) }
    done.await()
}
