import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val policy = Policy(90, 3, 5)
    // A run of seven documents: three typed ones right, two scans, two handwritten ones that went wrong.
    val runs = List(3) { Run("d", "typed", "valid", true, false, false, true) } + listOf(
        Run("d", "scanned", "valid", true, false, false, true),
        Run("d", "scanned", "needs_review", false, false, false, true),
        Run("d", "handwritten", "failed", false, true, false, true),
        Run("d", "handwritten", "needs_review", false, false, true, true))
    println(audit(runs, policy))
}
