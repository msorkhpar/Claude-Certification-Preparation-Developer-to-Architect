import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val plan = """["research the topic", "draft the outline", "check the facts"]"""

    // A stand-in for the model, like the one the tests script: the start of the prompt says which step is asking.
    val model: Ask = { prompt ->
        when {
            prompt.startsWith("Plan") -> plan
            prompt.startsWith("Subtask") -> "done: " + prompt.lines().first().removePrefix("Subtask: ")
            else -> "FINAL"
        }
    }

    // The orchestrator asks for a plan, runs one worker per subtask, then combines the results.
    val result = orchestrate(model, "Write a guide")

    println("status: ${result["status"]} | fallback: ${result["fallback"]} | calls: ${result["calls"]}")
    println("plan: ${result["plan"]}")
    println("results: ${result["results"]}")
    println("answer: ${result["answer"]}")
}
