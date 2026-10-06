import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // How often to submit a batch so a 30-hour SLA still leaves room for the 24-hour window and 2 hours of handling.
    println("interval for a 30 h SLA: ${submissionInterval(30)}")
    println("API for a blocking check: ${chooseApi(true)}")
    println("API for a nightly report: ${chooseApi(false)}")

    // What to do with the results of a batch: (custom id, result kind) pairs and the size of each request.
    val results = listOf(Result("a1", "succeeded"), Result("big", "expired"), Result("bad", "invalid_request"), Result("late", "errored"))
    println("resubmission plan: ${resubmissionPlan(results, mapOf("big" to 2000, "bad" to 10, "late" to 10), 1000)}")
    println("review passes: ${reviewPlan(listOf("a.py", "b.py", "c.py"))}")
}
