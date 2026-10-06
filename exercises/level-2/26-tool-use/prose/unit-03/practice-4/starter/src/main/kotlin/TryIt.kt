import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val schema = mapOf("type" to "object", "properties" to mapOf("city" to mapOf("type" to "string")), "required" to listOf("city"))
    val tools = listOf(Tool("get_weather", "Current weather for a city.", schema) { input -> "${input["city"]}: 18 C" })

    // A stand-in for the model, like the one the tests use: it asks for one tool call, then ends its turn.
    val replies = mutableListOf<Map<String, Any?>>(
        mapOf("content" to listOf(mapOf("type" to "text", "text" to "Let me check."),
            mapOf("type" to "tool_use", "id" to "tu_1", "name" to "get_weather", "input" to mapOf("city" to "Oslo"))),
            "stop_reason" to "tool_use"),
        mapOf("content" to listOf(mapOf("type" to "text", "text" to "It is 18 C in Oslo.")), "stop_reason" to "end_turn"),
    )

    val result = runAgent({ _ -> replies.removeAt(0) }, tools, "Weather in Oslo?")

    println("status: ${result["status"]}")
    println("text: ${result["text"]}")
    println("model calls: ${result["turns"]}")
    println("messages: ${Json.stringify(result["messages"])}")
}
