import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val secret = "s3cret"
    val meta = mapOf("io.modelcontextprotocol/protocolVersion" to "2026-07-28",
        "io.modelcontextprotocol/clientCapabilities" to mapOf("elicitation" to emptyMap<String, Any?>(), "sampling" to emptyMap<String, Any?>()))
    val base = mapOf("name" to "deploy", "arguments" to mapOf("service" to "api", "env" to "production"), "_meta" to meta)

    // Round trip 1: the server needs a person's confirmation, so it ends the call with input_required and a signed state.
    val first = callTool(base, secret, "alice", 1000)
    println("first call: ${first["resultType"]} | asks for: ${(first["inputRequests"] as? Map<*, *>)?.keys ?: "[]"}")
    println("state is a string: ${first["requestState"] is String}")

    // Round trip 2: the client retries the same call with the answer and echoes the state back.
    val retry = base + mapOf("inputResponses" to mapOf("confirm" to mapOf("action" to "accept", "content" to mapOf("confirm" to true))),
        "requestState" to first["requestState"])
    val second = callTool(retry, secret, "alice", 1010)
    println("second call: ${second["resultType"]} | asks for: ${(second["inputRequests"] as? Map<*, *>)?.keys ?: "[]"}")
}
