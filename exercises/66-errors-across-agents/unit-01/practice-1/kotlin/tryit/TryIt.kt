import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A stand-in for a search subagent, like the tests use: it times out once, then answers.
    val news = searchWithRecovery("news") { _, attempt ->
        if (attempt == 1) Reply("error", "timeout") else Reply("ok", items = listOf("a", "b"))
    }
    println("transient failure: $news")

    // A search the agent may not run: the failure is not transient.
    val filings = searchWithRecovery("filings") { _, _ -> Reply("error", "permission") }
    println("permission failure: $filings")

    // What the coordinator does with each outcome, and what the final report admits.
    val results = linkedMapOf("news" to news, "filings" to filings)
    println("plan: ${coordinatorPlan(results)}")
    println(coverageNote(results, listOf("news", "filings", "patents")))
}
