import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.Prompt
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val log = System.getLogger("notes_example")

/**
 * One file, two roles: run with --serve it is an MCP server over stdio; run alone it starts itself as a server and talks to it twice.
 *
 * First through the SDK client (what a host application does), then by hand with raw JSON-RPC lines (what the SDK hides). Everything is
 * local: the server is a child process and the two sides talk through pipes. `io.modelcontextprotocol:kotlin-sdk` 0.15.0, checked on 2026-10-04.
 */

/** The server's state. */
class Notes {
    val titles = mutableListOf<String>()

    /** An expected failure is a result with isError, not an exception: the model reads the message and can retry. */
    fun add(title: String): CallToolResult {
        if (title.isBlank()) return result("title is required", true)
        titles += title.trim()
        System.err.println("saved note ${titles.size}") // stderr is for logs; stdout carries the protocol
        return result("Saved note ${titles.size}: ${title.trim()}", false)
    }

    /** The Kotlin SDK does not validate arguments against the schema, so the one check this tool needs is written out. */
    fun search(query: String, limit: JsonPrimitive?): CallToolResult {
        val max = if (limit == null) 5 else limit.intOrNull ?: return result("limit must be an integer", true)
        val hits = titles.withIndex().filter { query.lowercase() in it.value.lowercase() }.map { "${it.index + 1}. ${it.value}" }
        return result(hits.take(max).joinToString("\n").ifEmpty { "No notes match \"$query\"" }, false)
    }

    fun count() = "${titles.size} note" + if (titles.size == 1) "" else "s"

    fun review(tone: String?) = "Review these notes in a ${tone ?: "brief"} tone:\n" + titles.joinToString("\n") { "- $it" }
}

fun result(text: String, isError: Boolean) = CallToolResult(content = listOf(TextContent(text)), isError = isError)

fun words(result: CallToolResult) = result.content.filterIsInstance<TextContent>().joinToString("") { it.text }

fun py(value: Boolean) = if (value) "True" else "False"

private fun property(type: String) = buildJsonObject { put("type", type) }

fun buildServer(notes: Notes): Server {
    val server = Server(Implementation("notes", "1.0.0"), ServerOptions(ServerCapabilities(
        tools = ServerCapabilities.Tools(false), resources = ServerCapabilities.Resources(false, false), prompts = ServerCapabilities.Prompts(false))))
    server.addTool(
        "add_note", "Save a note.",
        ToolSchema(properties = buildJsonObject { put("title", property("string")); put("text", property("string")) }, required = listOf("title", "text")),
        toolAnnotations = ToolAnnotations(readOnlyHint = false),
    ) { request -> notes.add(request.arguments?.get("title")?.jsonPrimitive?.contentOrNull ?: "") }
    server.addTool(
        "search_notes", "Find notes by a word in the title.",
        ToolSchema(properties = buildJsonObject { put("query", property("string")); put("limit", buildJsonObject { put("type", "integer"); put("default", 5) }) }, required = listOf("query")),
        toolAnnotations = ToolAnnotations(readOnlyHint = true),
    ) { request -> notes.search(request.arguments?.get("query")?.jsonPrimitive?.contentOrNull ?: "", request.arguments?.get("limit")?.jsonPrimitive) }
    server.addResource("notes://count", "count", "How many notes are saved.", "text/plain") { request ->
        ReadResourceResult(listOf(TextResourceContents(notes.count(), request.uri, "text/plain")))
    }
    server.addPrompt("review_notes", null, listOf(PromptArgument("tone", required = false))) { request ->
        GetPromptResult(listOf(PromptMessage(Role.User, TextContent(notes.review(request.arguments?.get("tone"))))))
    }
    return server
}

fun withSdkClient(command: List<String>) = runBlocking {
    val child = ProcessBuilder(command).start()
    val client = Client(Implementation("host", "1.0.0"))
    client.connect(StdioClientTransport(child.inputStream.asSource().buffered(), child.outputStream.asSink().buffered(), child.errorStream.asSource().buffered()))
    val info = client.serverVersion!!
    println("server: ${info.name} ${info.version}")
    val caps = client.serverCapabilities!!
    println("declares: " + listOfNotNull("tools".takeIf { caps.tools != null }, "resources".takeIf { caps.resources != null }, "prompts".takeIf { caps.prompts != null }).joinToString(", "))
    for (tool in client.listTools().tools) {
        val required = tool.inputSchema.required.orEmpty()
        val args = tool.inputSchema.properties!!.keys.joinToString(", ") { if (it in required) it else "[$it]" }
        println("tool: ${tool.name}($args) read-only hint ${py(tool.annotations?.readOnlyHint == true)}")
    }
    val ok = client.callTool("add_note", mapOf("title" to "Plan", "text" to "ship it"))
    println("add_note -> '${words(ok)}', isError ${py(ok.isError == true)}")
    val bad = client.callTool("add_note", mapOf("title" to " ", "text" to "x"))
    println("add_note with a blank title -> isError ${py(bad.isError == true)}, says why: ${py("title is required" in words(bad))}")
    println("search_notes -> '${words(client.callTool("search_notes", mapOf("query" to "PLAN")))}'")
    println("resource notes://count -> ${(client.readResource(ReadResourceRequest(io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequestParams("notes://count"))).contents[0] as TextResourceContents).text}")
    val message = client.getPrompt(GetPromptRequest(io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequestParams("review_notes", emptyMap()))).messages[0]
    println("prompt review_notes -> role ${message.role.name.lowercase()}, '${(message.content as TextContent).text.replace("\n", "\\n")}'")
    client.close()
    child.destroy()
}

/** The same server, spoken to line by line: one JSON-RPC message per line on stdin and stdout, nothing else on stdout. */
fun wire(command: List<String>) {
    val child = ProcessBuilder(command).start()
    val logs = StringBuffer()
    val drain = thread { child.errorStream.bufferedReader().forEachLine { logs.append(it).append('\n') } }
    val input = child.outputStream.bufferedWriter()
    val output = child.inputStream.bufferedReader()
    var stray = 0
    fun send(line: String) { input.write(line + "\n"); input.flush() }
    fun receive(id: Int): JsonObject {
        while (true) {
            val line = output.readLine() ?: error("the server closed stdout")
            val message = runCatching { Json.parseToJsonElement(line).jsonObject }.getOrNull()
            if (message == null) { stray++; continue }
            if (message["id"]?.jsonPrimitive?.intOrNull == id) return message
        }
    }
    send("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"wire","version":"1"}}}""")
    val first = receive(1)["result"]!!.jsonObject
    val caps = first["capabilities"]!!.jsonObject.keys.filter { it in listOf("tools", "resources", "prompts") }.sorted().joinToString(", ") { "'$it'" }
    println("-> initialize (id 1)      <- protocol ${first["protocolVersion"]!!.jsonPrimitive.content}, server ${first["serverInfo"]!!.jsonObject["name"]!!.jsonPrimitive.content}, capabilities [$caps]")
    send("""{"jsonrpc":"2.0","method":"notifications/initialized"}""")
    send("""{"jsonrpc":"2.0","id":2,"method":"tools/list"}""")
    println("-> tools/list (id 2)      <- tools [" + receive(2)["result"]!!.jsonObject["tools"]!!.jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.content }.sorted().joinToString(", ") { "'$it'" } + "]")
    send("""{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"add_note","arguments":{"title":"Wire","text":"by hand"}}}""")
    println("-> tools/call (id 3)      <-  " + receive(3)["result"]!!.jsonObject["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
    send("""{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"search_notes","arguments":{"query":"wire","limit":"two"}}}""")
    val answer = receive(4)
    println("-> tools/call with limit 'two' (id 4)   <- " + when {
        answer["result"]?.jsonObject?.get("isError")?.jsonPrimitive?.booleanOrNull == true -> "a result with isError"
        "error" in answer -> "a protocol error"
        else -> "a result"
    })
    input.close()
    if (!child.waitFor(10, TimeUnit.SECONDS)) child.destroyForcibly()
    drain.join(2000)
    println("lines on stdout that were not JSON: $stray")
    println("the server's own log went to stderr: ${py("saved note 1" in logs)}")
}

fun main(args: Array<String>) {
    // kotlin-logging, which the SDK uses, prints one line to stdout when it starts; on a stdio server that line would corrupt the protocol
    System.setProperty("kotlin-logging.logStartupMessage", "false")
    if (args.firstOrNull() == "--serve") {
        runBlocking {
            val server = buildServer(Notes())
            val closed = CompletableDeferred<Unit>()
            val session = server.createSession(StdioServerTransport(System.`in`.asSource().buffered(), System.out.asSink().buffered()))
            session.onClose { closed.complete(Unit) }
            closed.await()
        }
        return
    }
    val command = listOf(File(System.getProperty("java.home"), "bin/java").path, "-cp", System.getProperty("java.class.path"), "NotesExampleKt", "--serve")
    withSdkClient(command)
    println()
    wire(command)
}
