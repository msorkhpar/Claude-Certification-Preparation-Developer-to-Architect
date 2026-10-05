import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A sound workflow design passes; a multi-agent design that writes without approval and has no feedback loop does not.
    val stages = mapOf("input" to listOf("parse"), "processing" to listOf("classify", "route"), "output" to listOf("validate", "send"), "feedback" to listOf("review a sample"))
    fun design(name: String, pattern: String, agents: Int, writes: Boolean, stages: Map<String, Any?>): Map<String, Any?> = linkedMapOf(
        "name" to name, "pattern" to pattern, "agents" to agents, "cost" to 3, "path_known" to true, "parallel_independent" to false,
        "shared_context" to false, "needs_audit" to true, "writes_without_approval" to writes, "stages" to stages)

    val sound = design("intake", "workflow", 1, false, stages)
    val risky = design("research", "multi-agent", 4, true, stages + ("feedback" to emptyList<String>()))
    for (d in listOf(sound, risky)) {
        val findings = review(d)
        println("${d["name"]} -> ${findings?.let { verdict(it) }} $findings")
    }
}
