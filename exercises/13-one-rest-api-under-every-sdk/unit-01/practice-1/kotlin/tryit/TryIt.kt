import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A stand-in transport, like the one the tests use: it records the request and answers with a fixed reply.
    val sent = mutableListOf<Request>()
    val transport = Transport { request ->
        sent.add(request)
        Response(200, emptyMap(), """{"content":[{"type":"text","text":"Paris."}],"stop_reason":"end_turn","usage":{"input_tokens":9,"output_tokens":3}}""")
    }

    val messages = listOf(mapOf<String, Any?>("role" to "user", "content" to "Capital of France?"))
    val message = sendMessages(transport, "sk-test-0123456789abcdef", "claude-sonnet-5-5", messages, 64, "Be brief.")

    println("requests sent: ${sent.size}")
    println("method and url: ${sent.firstOrNull()?.let { it.method + " " + it.url }}")
    println("header names: ${sent.firstOrNull()?.headers?.keys?.sorted()}")
    println("reply text: ${textOf(message)}")

    // An error reply becomes an ApiError.
    try {
        sendMessages({ Response(429, mapOf("request-id" to "req_1"),
            """{"type":"error","error":{"type":"rate_limit_error","message":"Rate limited"}}""") },
            "sk-test-0123456789abcdef", "claude-sonnet-5-5", messages, 64)
    } catch (err: ApiError) {
        println("api error: ${err.status} ${err.errorType} ${err.requestId}")
    }
}
