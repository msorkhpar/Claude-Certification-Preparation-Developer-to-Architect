import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val policy = mapOf("allowed" to listOf("haiku", "sonnet"), "routes" to mapOf("classify" to "haiku", "draft" to "sonnet", "review" to "opus"),
        "default" to "sonnet", "cheaper" to mapOf("opus" to "sonnet", "sonnet" to "haiku"))

    // The gateway first checks the team's budget, then routes the request: near the limit it picks a cheaper model.
    for (spend in listOf(0L, 850L, 990L)) {
        val status = admit(spend, 1000, 100)
        println("spend $spend/1000 -> $status: a review request goes to ${status?.let { route(mapOf("task" to "review"), policy, it) }}")
    }

    // A delivery check: does a 95th-percentile latency of 40 s leave 20 percent of margin under a 60 s timeout?
    println("delivery: ${delivery(40, 60, 20)}")
}
