import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A tool result is large; keep only the fields the next turn needs, with their exact values.
    val order = linkedMapOf("order_id" to "A-1042", "items" to "2 x kettle", "warehouse_bin" to "R7-22", "refund_amount" to "\$129.50")
    println("trimmed record: ${trimRecord(order, listOf("refund_amount", "order_id"))}")

    // A newer value replaces the current one; the old one is kept as superseded.
    var facts = updateFacts(emptyMap(), "address", "12 Oak St", "2026-08-01")
    facts = updateFacts(facts, "address", "9 Elm Rd", "2026-09-10")
    println("facts: $facts")

    // The context of the next request: case facts, then the summary, then the recent messages.
    val caseFacts = listOf(FactEntry("c1", "refund", "\$129.50", "2026-09-02"))
    val recent = listOf(Message("user", "text", "m1", "Where is my refund?"))
    println(buildContext("c1", caseFacts, "The customer asked about a refund.", recent))

    // A tool call and its result stay together when the window drops old messages.
    val messages = listOf(Message("user", "text", "m1", "x".repeat(40)),
        Message("assistant", "tool_use", "t1", "lookup"), Message("user", "tool_result", "t1", "found"))
    println("window ids: ${window(messages, 6).map { it.id }}")
}
