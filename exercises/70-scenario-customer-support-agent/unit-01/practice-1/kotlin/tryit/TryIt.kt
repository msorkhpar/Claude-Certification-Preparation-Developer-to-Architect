import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Sessions of a support agent: a clean one, one that skipped the customer check, and one escalated without need.
    val clean = listOf(Step("get_customer"), Step("lookup_order"), Step("process_refund"))
    val sessions = listOf(
        Session("s", clean, "resolved", false, 2000, 10000),
        Session("s", listOf(Step("lookup_order"), Step("get_customer"), Step("process_refund")), "resolved", false, 0, 10000),
        Session("s", listOf(Step("get_customer")), "escalated", false, 0, 10000))
    println(audit(sessions))
}
