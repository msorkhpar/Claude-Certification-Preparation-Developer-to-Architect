import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // The four model roles are plain functions, like the ones the tests script.
    val planner: Planner = { _ ->
        mapOf("delegate" to true, "answer" to null, "subtasks" to listOf(
            mapOf("scope" to "chips", "brief" to "chips: find 2024 chip supply news"),
            mapOf("scope" to "cars", "brief" to "cars: find 2024 car output news"),
            mapOf("scope" to "rates", "brief" to "rates: find 2024 interest rates")))
    }
    // A subagent knows only its brief: here it just reports on the topic that starts it.
    val subagent: Spoke = { brief -> brief.substringBefore(":") + " report" }
    val reviewer: Reviewer = { _, _ -> emptyList() } // no gaps: nothing more to ask
    val synthesizer: Synthesizer = { _, findings -> findings.joinToString(" | ") { it["text"].toString() } }

    val result = coordinate(planner, subagent, reviewer, synthesizer, "How did supply change?")

    println("status: ${result?.get("status")} | subagent calls: ${result?.get("subagent_calls")} | rounds: ${result?.get("rounds")}")
    println("findings: ${result?.get("findings")}")
    println("answer: ${result?.get("answer")}")
}
