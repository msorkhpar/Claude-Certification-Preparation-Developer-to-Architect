import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val sonnet = "claude-sonnet-5-5"
    val haiku = "claude-haiku-4-5-20251001"

    // What the application wants, turned into the request parameters one model accepts.
    println("sonnet adaptive: ${buildParams(sonnet, 4096, mapOf("thinking" to mapOf("type" to "adaptive"), "effort" to "medium"))}")
    println("haiku budget: ${buildParams(haiku, 4096, mapOf("thinking" to mapOf("type" to "enabled", "budget_tokens" to 2048)))}")

    // A combination the API would answer with a 400 is refused, naming the parameter.
    try {
        println("haiku with effort: ${buildParams(haiku, 4096, mapOf("effort" to "high"))}")
    } catch (err: RejectedRequest) {
        println("refused: ${err.param}")
    }
}
