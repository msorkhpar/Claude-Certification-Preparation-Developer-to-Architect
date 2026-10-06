import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Results of an evaluation as (segment, correct) rows; a wrong refund costs more than a wrong order status.
    val costs = mapOf("order status" to 1, "refund" to 20, "policy" to 5)
    val results = List(30) { Result("order status", true) } + List(5) { Result("refund", true) } + List(3) { Result("refund", false) } +
        List(9) { Result("policy", true) } + Result("policy", false)
    println("segments: ${segmentTable(results, costs)}")

    // The same cases under the old and the new prompt: a gain in one segment must not hide a loss in a protected one.
    val pairs = listOf(Paired("refund", true, false), Paired("policy", false, true), Paired("policy", false, true), Paired("order status", true, true))
    println("shadow gate: ${shadowGate(pairs, setOf("refund"))}")
    println("A/B verdict: ${abVerdict(100, 200, 160, 200)}")

    // The cheapest model that is accurate and fast enough: (name, accuracy, p95 latency, cost).
    println("model: ${chooseModel(listOf(Option("small", 88, 900, 1), Option("medium", 94, 1500, 3), Option("large", 97, 4000, 9)), 90, 2000)}")
}
