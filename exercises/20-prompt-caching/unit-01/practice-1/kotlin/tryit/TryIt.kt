import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    fun block(id: String, section: String, tokens: Int, vararg extra: Pair<String, Any?>): Map<String, Any?> =
        mapOf("id" to id, "section" to section, "tokens" to tokens) + extra

    // A request whose blocks arrive in the wrong order: the volatile date sits first, in the system prompt.
    val blocks = listOf(
        block("date", "system", 20, "volatile" to true),
        block("tools", "tools", 2000),
        block("rules", "system", 3000, "breakpoint" to true),
        block("manual", "messages", 6000, "breakpoint" to true),
        block("question", "messages", 40),
    )
    try {
        val plan = planRequest(blocks, 1024)
        println("order: ${plan.map { it["id"] }}")
        println("cache per block: ${plan.map { "${it["id"]}=${it["cache"]}" }}")
    } catch (err: PlanError) {
        println("plan error: ${err.message}")
    }
}
