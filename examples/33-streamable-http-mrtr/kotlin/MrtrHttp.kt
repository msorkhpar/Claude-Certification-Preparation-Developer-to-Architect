import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpMethod
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttpTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.modelcontextprotocol.kotlin.sdk.shared.TransportSendOptions
import io.modelcontextprotocol.kotlin.sdk.types.BooleanSchema
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ClientCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageRequest
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageResult
import io.modelcontextprotocol.kotlin.sdk.types.ElicitRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ElicitResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCError
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCNotification
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCRequest
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCResponse
import io.modelcontextprotocol.kotlin.sdk.types.Method
import io.modelcontextprotocol.kotlin.sdk.types.RequestId
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.SamplingMessage
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.StopReason
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val log = System.getLogger("mrtr_http")

/**
 * A tool that asks for a person's confirmation and for a model completion, over Streamable HTTP on the loopback interface.
 *
 * The server and the client are `io.modelcontextprotocol:kotlin-sdk` 0.15.0 (the server runs on Ktor's CIO engine), with scripted callbacks
 * in place of a person and a model. This SDK speaks the 2025-11-25 revision, so the flow differs from the Python example next to it: there
 * is an `initialize` handshake and a session id, and the server calls the client in the middle of the request, as a request of its own on
 * the response stream. The log under the program output is what the client's transport sent and received, one JSON-RPC message at a time.
 * Checked on 2026-10-04 against the "Streamable HTTP" page of the MCP specification.
 */
private fun text(text: String, isError: Boolean) = CallToolResult(content = listOf(TextContent(text)), isError = isError)

private fun property(type: String) = buildJsonObject { put("type", type) }

fun buildServer(): Server {
    val server = Server(Implementation("deployer", "1.0.0"), ServerOptions(ServerCapabilities(tools = ServerCapabilities.Tools(false))))
    server.addTool(
        "deploy", "Deploy a service; production needs a person's confirmation.",
        ToolSchema(properties = buildJsonObject { put("service", property("string")); put("env", property("string")) }, required = listOf("service", "env")),
    ) { request ->
        val service = request.arguments!!["service"]!!.jsonPrimitive.content
        val env = request.arguments!!["env"]!!.jsonPrimitive.content
        if (env == "production") {
            val answer = try {
                createElicitation("Deploy $service to production?", ElicitRequestParams.RequestedSchema(properties = mapOf("confirm" to BooleanSchema(title = "Confirm the deployment")), required = listOf("confirm")))
            } catch (e: IllegalStateException) { // the SDK refuses to ask a client that did not declare elicitation
                return@addTool text("Deploying to production needs confirmation, and this client cannot be asked.", true)
            }
            if (answer.action != ElicitResult.Action.Accept || answer.content?.get("confirm")?.jsonPrimitive?.booleanOrNull != true) return@addTool text("Deployment cancelled", false)
        }
        text("Deployed $service to $env", false)
    }
    server.addTool(
        "release_notes", "Write release notes with the client's model.",
        ToolSchema(properties = buildJsonObject { put("service", property("string")) }, required = listOf("service")),
    ) { request ->
        val service = request.arguments!!["service"]!!.jsonPrimitive.content
        val completion = createMessage(CreateMessageRequest(CreateMessageRequestParams(
            maxTokens = 100, messages = listOf(SamplingMessage(Role.User, listOf(TextContent("Write one sentence of release notes for $service.")))))))
        text("$service: ${(completion.content[0] as TextContent).text}", false)
    }
    return server
}

/** The MCP endpoint on 127.0.0.1: Ktor's CIO engine on a free port. */
class Running(val engine: EmbeddedServer<*, *>, val url: String) : AutoCloseable {
    override fun close() = engine.stop(0, 0)
}

suspend fun serveOnLoopback(): Running {
    val engine = embeddedServer(ServerCIO, port = 0, host = "127.0.0.1") { mcpStreamableHttp { buildServer() } }.startSuspend(wait = false)
    return Running(engine, "http://127.0.0.1:${engine.engine.resolvedConnectors().first().port}/mcp")
}

/** One line for a JSON-RPC message that crossed the wire. */
fun describe(message: JSONRPCMessage, fromServer: Boolean): String = when (message) {
    is JSONRPCRequest -> message.method + (message.params?.jsonObject?.get("name")?.jsonPrimitive?.content?.let { " $it" } ?: "") + if (fromServer) " (a request from the server)" else ""
    is JSONRPCResponse -> if (fromServer && message.result is CallToolResult) "complete: " + ((message.result as CallToolResult).content[0] as TextContent).text else "the answer to the server's request"
    is JSONRPCError -> "error ${message.error.code}"
    is JSONRPCNotification -> message.method
    else -> ""
}

/** What the client's transport carried, in the order it happened, and whether every request after the first carried the session id. */
class WireLog {
    val lines = CopyOnWriteArrayList<String>()
    private val callIds = java.util.concurrent.ConcurrentHashMap.newKeySet<RequestId>()
    @Volatile var sawInitialize = false
    @Volatile var sessionOnEvery = true
    private val first = AtomicBoolean(true)

    /** Wraps a transport so that each tools/call, each request from the server and each response is logged as it passes. */
    fun around(inner: Transport) = object : Transport by inner {
        override fun onMessage(block: suspend (JSONRPCMessage) -> Unit) = inner.onMessage { message ->
            if (message is JSONRPCRequest || (message is JSONRPCResponse && message.id in callIds)) lines += "<- ${describe(message, true)}"
            block(message)
        }

        override suspend fun send(message: JSONRPCMessage, options: TransportSendOptions?) {
            if (message is JSONRPCRequest && message.method == Method.Defined.Initialize.value) sawInitialize = true
            if (message is JSONRPCRequest && message.method == Method.Defined.ToolsCall.value) {
                callIds += message.id
                lines += "-> ${describe(message, false)}"
            } else if (message is JSONRPCResponse) {
                lines += "-> ${describe(message, false)}"
            }
            inner.send(message, options)
        }
    }

    private val eventStreamOpen = CompletableDeferred<Unit>()

    /** Looks at each HTTP request the client is about to send: the first is the initialize, the others must carry a session id. */
    fun attach(http: HttpClient) {
        http.plugin(HttpSend).intercept { request ->
            if (!first.getAndSet(false) && request.headers["mcp-session-id"] == null) sessionOnEvery = false
            val call = execute(request)
            if (request.method == HttpMethod.Get) eventStreamOpen.complete(Unit) // the server has answered the client's request for its event stream
            call
        }
    }

    /**
     * This server answers each call with plain JSON, so a request that it makes in the middle of a call can only travel on the client's
     * standalone event stream (a GET that the client opens on its own after the handshake). A caller waits for that stream before the first call.
     */
    suspend fun awaitEventStream() = withTimeout(10_000) { eventStreamOpen.await() }
}

suspend fun connect(url: String, log: WireLog, person: ((ElicitRequestParams) -> ElicitResult)?, model: ((CreateMessageRequest) -> CreateMessageResult)?): Client {
    val http = HttpClient(ClientCIO) {
        install(SSE)
        install(HttpTimeout) { requestTimeoutMillis = 120_000 } // the engine's default of 15 seconds also applies to a stream that stays open
    }
    log.attach(http)
    val capabilities = ClientCapabilities(
        sampling = if (model != null) ClientCapabilities.Sampling() else null,
        elicitation = if (person != null) ClientCapabilities.Elicitation(form = JsonObject(emptyMap())) else null)
    val client = Client(Implementation("host", "1.0.0"), ClientOptions(capabilities = capabilities))
    if (person != null) client.setElicitationHandler { request -> person(request.params) }
    if (model != null) client.setRequestHandler<CreateMessageRequest>(Method.Defined.SamplingCreateMessage) { request, _ -> model(request) }
    client.connect(log.around(http.mcpStreamableHttpTransport(url)))
    log.awaitEventStream()
    return client
}

fun textOf(result: CallToolResult) = (result.content[0] as TextContent).text

fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
  System.setProperty("kotlin-logging.logStartupMessage", "false") // the SDK's logging library would otherwise print one line to stdout
  runBlocking {
    val log = WireLog()
    val person = { request: ElicitRequestParams ->
        println("the person is asked: ${request.message}")
        ElicitResult(ElicitResult.Action.Accept, buildJsonObject { put("confirm", true) })
    }
    val model = { request: CreateMessageRequest ->
        println("the client's model is asked: ${(request.params.messages[0].content[0] as TextContent).text}")
        CreateMessageResult(Role.Assistant, listOf(TextContent("Checkout is faster.")), "scripted", StopReason.EndTurn)
    }
    serveOnLoopback().use { server ->
        val client = connect(server.url, log, person, model)
        for ((name, arguments) in listOf("deploy" to mapOf("service" to "api", "env" to "production"), "deploy" to mapOf("service" to "api", "env" to "staging"), "release_notes" to mapOf("service" to "api"))) {
            log.lines.clear()
            val result = client.callTool(name, arguments)
            println("$name${if ("env" in arguments) " " + arguments["env"] else ""} -> ${textOf(result)}")
            log.lines.forEach { println("   $it") }
        }
        println("the first request was initialize: ${py(log.sawInitialize)} and every later request carried a session id: ${py(log.sessionOnEvery)}")
        client.close()
    }
    serveOnLoopback().use { second ->
        val bare = connect(second.url, WireLog(), null, null)
        val result = bare.callTool("deploy", mapOf("service" to "api", "env" to "production"))
        println("a client that cannot be asked -> ${if (result.isError == true) "a tool error" else "a result"} that says so: ${py("cannot be asked" in textOf(result))}")
        bare.close()
    }
  }
  exitProcess(0)
}
