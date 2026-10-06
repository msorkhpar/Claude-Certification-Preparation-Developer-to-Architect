import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    fun failure(code: Int, kind: String = "api_error") =
        Response(code, emptyMap(), mapOf("type" to "error", "error" to mapOf("type" to kind, "message" to "x")))

    // A scripted send(), like the one the tests use: overloaded twice, then a good reply.
    val replies = mutableListOf(failure(529, "overloaded_error"), failure(503), Response(200, emptyMap(), mapOf("type" to "message")))
    var calls = 0
    val send = { calls++; replies.removeAt(0) }

    // The sleep is injected, so nothing really waits: it just records the delays asked for.
    val waits = mutableListOf<Double>()
    try {
        val result = callWithRetry(send, { waits.add(it) })
        println("final status: ${result.status}")
    } catch (err: CallFailed) {
        println("gave up: ${err.message}")
    }
    println("calls made: $calls")
    println("waits requested: $waits")
}
