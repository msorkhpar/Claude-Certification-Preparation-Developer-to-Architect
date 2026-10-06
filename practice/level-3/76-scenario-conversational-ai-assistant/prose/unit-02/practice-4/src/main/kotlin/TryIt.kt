import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val policy = Policy(12, 10, 80, 3)
    // A batch of conversations: billing mostly resolved, a safety case handed off, one overlong smalltalk.
    val batch = List(3) { Conversation("c", "billing", 5, true, "none", false, false, false) } + listOf(
        Conversation("c", "billing", 5, false, "requested", true, false, false),
        Conversation("c", "smalltalk", 5, true, "none", false, false, false),
        Conversation("c", "smalltalk", 15, true, "none", false, false, false),
        Conversation("c", "safety", 5, false, "safety", true, false, true),
        Conversation("c", "billing", 5, true, "none", false, true, false))
    println(review(batch, policy))
}
