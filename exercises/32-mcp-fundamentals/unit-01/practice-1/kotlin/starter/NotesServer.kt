// A notes server for the Model Context Protocol, over stdio. See ../../statement.md.
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

fun main() = runBlocking {
    // TODO: name the server "notes" (version "1.0.0"), keep the notes in memory, and register the tools, resources and prompt of the statement.
    val server = Server(Implementation(name = "server", version = "0.0.0"), ServerOptions(capabilities = ServerCapabilities()))
    val session = server.createSession(StdioServerTransport(System.`in`.asSource().buffered(), System.out.asSink().buffered()) { })
    val done = CompletableDeferred<Unit>()
    session.onClose { done.complete(Unit) }
    done.await()
}
