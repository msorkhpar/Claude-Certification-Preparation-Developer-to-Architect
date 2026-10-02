import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.server.Server

fun main() {
    println(Anth.describe()); println(Mcp.names())
    println(Server::class.qualifiedName)
    println(Implementation::class.qualifiedName)
}
