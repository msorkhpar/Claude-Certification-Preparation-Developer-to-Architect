import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    fun user(text: String): Map<String, Any?> = mapOf("role" to "user", "content" to text)
    fun said(text: String): Map<String, Any?> = mapOf("role" to "assistant", "content" to listOf(mapOf("type" to "text", "text" to text)))
    fun call(id: String, name: String): Map<String, Any?> =
        mapOf("role" to "assistant", "content" to listOf(mapOf("type" to "tool_use", "id" to id, "name" to name, "input" to emptyMap<String, Any?>())))
    fun result(id: String, text: String): Map<String, Any?> =
        mapOf("role" to "user", "content" to listOf(mapOf("type" to "tool_result", "tool_use_id" to id, "content" to text)))

    // A conversation of four turns, like the one the tests compact.
    val messages = listOf(
        user("first question"), said("first answer " + "x".repeat(80)),
        user("second question"), call("a1", "search"), result("a1", "R".repeat(300)), said("second answer"),
        user("third question"), said("third answer " + "y".repeat(60)),
        user("fourth question"), call("a2", "search"), result("a2", "S".repeat(100)), said("fourth answer"),
    )

    // A stand-in for the summariser model: it is told what to fold away and answers with a fixed text.
    val summarise = { older: List<Map<String, Any?>> ->
        println("summariser asked to fold ${older.size} messages")
        "The user asked three things."
    }

    val budget = countTokens(messages) / 2
    val done = compact(messages, budget, summarise, 1)

    println("tokens before: ${countTokens(messages)} | budget: $budget")
    println("messages before and after: ${messages.size} -> ${done.size}")
    println("roles after: ${done.map { it["role"] }}")
    println("first message: ${done.firstOrNull()?.get("content")}")
}
