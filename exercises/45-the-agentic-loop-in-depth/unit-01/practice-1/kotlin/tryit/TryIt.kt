import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A scripted model, like the one the tests use: it asks for one tool call, then ends its turn.
    val replies = mutableListOf<Map<String, Any?>>(
        mapOf("stop_reason" to "tool_use", "content" to listOf(mapOf("type" to "text", "text" to "Looking."),
            mapOf("type" to "tool_use", "id" to "t1", "name" to "lookup", "input" to mapOf("n" to 1)))),
        mapOf("stop_reason" to "end_turn", "content" to listOf(mapOf("type" to "text", "text" to "Record 1 found."))))
    val model: Model = { messages ->
        println("model called with ${messages.size} messages")
        if (replies.isEmpty()) mapOf("stop_reason" to "end_turn", "content" to listOf(mapOf("type" to "text", "text" to "script ran out")))
        else replies.removeAt(0)
    }
    val tools: Tools = mapOf("lookup" to { arguments -> "record ${arguments["n"]}" })

    val result = runAgent(model, tools, "find record 1")

    println("status: ${result?.get("status")} | turns: ${result?.get("turns")}")
    println("final text: ${result?.get("text")}")
    println("roles: ${(result?.get("messages") as? List<*>)?.map { (it as Map<*, *>)["role"] }}")
}
