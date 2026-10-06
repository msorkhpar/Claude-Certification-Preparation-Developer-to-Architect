import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A stand-in for the API, like the one the tests use: it answers every request the same way.
    val fakeSend = Send {
        mapOf(
            "content" to listOf(mapOf("type" to "text", "text" to "Paris.")),
            "stop_reason" to "end_turn",
            "usage" to mapOf("input_tokens" to 10L, "output_tokens" to 5L),
        )
    }

    val chat = Conversation(fakeSend, "claude-sonnet-5-5", 64, system = "Be brief.")
    val reply = chat.say("Capital of France?")
    chat.say("Since when?")

    println("reply text: ${reply.text}")
    println("stop reason: ${reply.stopReason} | truncated: ${reply.truncated}")
    println("history size: ${chat.history().size}")
    println("totals: ${chat.totals()}")
}
