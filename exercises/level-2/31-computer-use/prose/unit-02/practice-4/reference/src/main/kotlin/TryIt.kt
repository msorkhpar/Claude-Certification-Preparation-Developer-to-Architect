import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    fun use(id: String, name: String, vararg input: Pair<String, Any?>): Map<String, Any?> =
        mapOf("type" to "tool_use", "id" to id, "name" to name, "toolset_name" to "computer", "input" to linkedMapOf(*input))

    // A toy screen in memory: a search box and a payment button.
    val screen: MutableMap<String, Any?> = linkedMapOf(
        "width" to 1920, "height" to 1080, "cursor" to mutableListOf<Any?>(0, 0), "typed" to "", "log" to mutableListOf<Any?>(),
        "elements" to listOf(mapOf("id" to "search", "x" to 1000, "y" to 200, "w" to 400, "h" to 40, "risk" to "none"),
            mapOf("id" to "pay", "x" to 800, "y" to 600, "w" to 200, "h" to 60, "risk" to "payment")))

    // The model is a function that returns scripted replies in order, like the one the tests use.
    val replies = mutableListOf<Map<String, Any?>>(
        mapOf("content" to listOf(use("t1", "screenshot")), "stop_reason" to "tool_use"),
        mapOf("content" to listOf(use("t2", "left_click", "coordinate" to listOf(894, 164)), use("t3", "type", "text" to "weather"), use("t4", "screenshot")), "stop_reason" to "tool_use"),
        mapOf("content" to listOf(mapOf("type" to "text", "text" to "Typed it.")), "stop_reason" to "end_turn"))
    val requests = mutableListOf<Int>()
    val ask: Ask = { request ->
        requests += (request["messages"] as List<*>).size
        // when the script runs out, the model just ends the conversation
    if (replies.isEmpty()) mapOf("content" to listOf(mapOf("type" to "text", "text" to "script ran out")), "stop_reason" to "end_turn") else replies.removeAt(0)
    }

    val result = runComputerLoop(ask, screen)

    println("status and turns: ${result["status"]} ${result["turns"]}")
    println("messages in each request: $requests")
    println("actions performed on the screen: ${screen["log"]}")
    println("typed text: ${screen["typed"]}")
}
