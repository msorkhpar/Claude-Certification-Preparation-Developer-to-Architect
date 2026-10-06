import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Two of the course models, with prices in dollars per million tokens, as the tests use them.
    val haiku = mapOf("id" to "claude-haiku-4-5-20251001", "tier" to 1, "context" to 200_000L, "max_output" to 64_000L, "input" to 1.0, "output" to 5.0, "cache_read_multiplier" to 0.1)
    val sonnet = mapOf("id" to "claude-sonnet-5-5", "tier" to 2, "context" to 1_000_000L, "max_output" to 128_000L, "input" to 2.0, "output" to 10.0, "cache_read_multiplier" to 0.1)
    val catalog = listOf(haiku, sonnet)
    val usage = mapOf("input_tokens" to 1200, "output_tokens" to 300)

    println("sonnet cost (micro-dollars): ${requestCost(sonnet, usage)}")
    println("haiku cost (micro-dollars): ${requestCost(haiku, usage)}")
    try {
        println("tier 1 task goes to: ${route(catalog, mapOf("min_tier" to 1, "usage" to usage))}")
        println("tier 2 task goes to: ${route(catalog, mapOf("min_tier" to 2, "usage" to usage))}")
    } catch (err: NoModelError) {
        println("no model: ${err.message}")
    }
}
