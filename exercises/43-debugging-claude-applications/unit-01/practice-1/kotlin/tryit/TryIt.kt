import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A trace is the list of what happened: the request, then what came back.
    val request = mapOf("kind" to "request", "model" to "claude-sonnet-5-5", "max_tokens" to 1024,
        "tools" to listOf("get_weather"), "last_user_blocks" to listOf("text"))

    for ((status, errorType) in listOf(401 to "authentication_error", 529 to "overloaded_error")) {
        val error = mapOf("kind" to "error", "status" to status, "error_type" to errorType, "message" to "m")
        val d = Diagnose.diagnose(listOf(request, error))
        println("HTTP $status: type=${d["type"]} origin=${d["origin"]} recovery=${d["recovery"]}")
    }
}
