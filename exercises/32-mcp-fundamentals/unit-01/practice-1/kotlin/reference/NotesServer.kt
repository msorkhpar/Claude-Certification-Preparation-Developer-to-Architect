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

/** Refuse a bad note: the error message, or null when the (stripped) title and text are fine. */
fun noteError(title: String, text: String): String? {
    if (title.isEmpty()) return "title is required"
    if (text.isEmpty()) return "text is required"
    if (text.length > MAX_TEXT) return "text is too long (max $MAX_TEXT)"
    return null
}

/** Refuse a bad search: the error message, or null when the (stripped) query and the limit are fine. */
fun searchError(query: String, limit: Int): String? {
    if (query.isEmpty()) return "query is required"
    if (limit < 1 || limit > 20) return "limit must be between 1 and 20"
    return null
}

/** The lines `{id}. {title}` of the notes whose title or text contains the query, in any letter case, in id order. */
fun findHits(query: String): List<String> {
    val needle = query.lowercase()
    return notes.withIndex().filter { (_, n) -> n.title.lowercase().contains(needle) || n.text.lowercase().contains(needle) }.map { (i, n) -> "${i + 1}. ${n.title}" }
}

/** The answer of a search: at most `limit` hit lines joined by newlines, or the no-match sentence. */
fun formatHits(hits: List<String>, limit: Int, query: String): String =
    if (hits.isEmpty()) "No notes match \"$query\"" else hits.take(limit).joinToString("\n")

/** The text of the count resource: 0 notes, 1 note, 2 notes. */
fun countText(count: Int): String = "$count note" + (if (count == 1) "" else "s")

/** The text of one note, or an IllegalArgumentException "No note {id}" when the id is not a whole number of an existing note. */
fun noteText(id: String): String {
    val n = id.toIntOrNull()?.takeIf { id.all(Char::isDigit) && it in 1..notes.size }?.let { notes[it - 1] } ?: throw IllegalArgumentException("No note $id")
    return "${n.title}\n\n${n.text}"
}

/** The text of the review prompt. */
fun reviewText(tone: String): String =
    if (notes.isEmpty()) "There are no notes to review." else "Review these notes in a $tone tone:\n" + notes.joinToString("\n") { "- ${it.title}" }

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
    val limit = args?.get("limit")?.jsonPrimitive?.intOrNull ?: 5
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
                put("limit", buildJsonObject { put("type", "integer"); put("default", 5) })
            },
            required = listOf("query"),
        ),
        toolAnnotations = ToolAnnotations(readOnlyHint = true),
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
