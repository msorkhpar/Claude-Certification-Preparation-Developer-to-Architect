import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A small spec like the first main test case: a role, one document, one constraint and a task with a placeholder.
    val spec = Spec(
        task = "Classify the message about {{topic}}.",
        role = "You are a careful support analyst for {{company}}.",
        documents = listOf(Doc("policy.txt", "Refunds within 30 days.")),
        constraints = listOf("Answer in one word."),
    )
    val prompt = buildPrompt(spec, mapOf("company" to "Acme", "topic" to "delivery"))

    println("prompt length: ${prompt.length}")
    println("starts with: ${prompt.take(20).replace("\n", "\\n")}")
    println("has task block: ${prompt.contains("<task>\nClassify the message about delivery.\n</task>")}")
    println(prompt)
}
