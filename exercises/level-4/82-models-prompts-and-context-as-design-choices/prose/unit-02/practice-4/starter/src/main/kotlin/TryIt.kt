import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Prompt modules: static ones first (they can be cached), then the changing ones by priority.
    val role = mapOf("name" to "role", "static" to true, "text" to "r".repeat(400))
    val policy = mapOf("name" to "policy", "static" to true, "text" to "p".repeat(1648))
    val history = mapOf("name" to "history", "static" to false, "priority" to 1, "text" to "h".repeat(200))
    val question = mapOf("name" to "question", "static" to false, "priority" to 9, "text" to "Q: {q}")

    val prompt = assemble(listOf(question, role, history, policy), mapOf("q" to "hello"), 10_000)
    println("prompt: $prompt")

    // With a small budget the lowest-priority changing module is dropped.
    val small = assemble(listOf(question, role, history, policy), mapOf("q" to "hello"), 520)
    println("dropped with a budget of 520: ${small?.get("dropped")}")

    // The cheapest model that meets the tier and the latency.
    val models = listOf(mapOf("name" to "haiku", "tier" to 1, "latency_ms" to 300, "price_out" to 5),
        mapOf("name" to "sonnet", "tier" to 2, "latency_ms" to 900, "price_out" to 15))
    println("model: ${chooseModel(mapOf("tier" to 2, "max_latency_ms" to 1000), models)}")
}
