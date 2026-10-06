import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Findings from two subagents: the same claim from two sources, and one that disagrees.
    val findings = listOf(
        Finding("revenue 2023", "4.1B", "Annual report", "2024-02-01"),
        Finding("revenue 2023", "4.1B", "Press release", "2024-02-03"),
        Finding("headcount", "910", "Press release", "2024-03-01"),
        Finding("headcount", "950", "Blog", "2024-03-01"))
    println("missing fields: ${checkFinding(Finding("headcount", "910", null, null))}")

    val merged = merge(findings)
    for (entry in merged) println("entry: ${entry.claim} ${entry.status} ${entry.values}")

    // What the report says about coverage, and how an entry is shown.
    println("coverage: ${coverageNote(listOf("revenue 2023", "headcount", "patents"), merged, mapOf("patents" to "the registry timed out"))}")
    println("rendered: ${merged.firstOrNull()?.let { render(it, "news") }}")
}
