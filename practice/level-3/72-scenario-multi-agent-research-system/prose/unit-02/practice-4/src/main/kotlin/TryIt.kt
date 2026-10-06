import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Two subagents agree on claim X; a third scope timed out and is not covered.
    val a = Result("a", "ok", listOf(Finding("X", "1", "s1", "2025-01-01"), Finding("Y", "2", "s2", "2025-01-01")), null)
    val b = Result("b", "ok", listOf(Finding("X", "1", "s3", "2025-01-01")), null)
    val c = Result("c", "error", emptyList(), Failure("timeout", "q-c", emptyList(), listOf("q-c-narrow")))
    val report = synthesize(listOf("a", "b", "c"), listOf(a, b, c))
    println("status: ${report.status}")
    println("covered: ${report.covered} | gaps: ${report.gaps}")
    println("claims: ${report.claims}")
    println("note: ${report.note}")
}
