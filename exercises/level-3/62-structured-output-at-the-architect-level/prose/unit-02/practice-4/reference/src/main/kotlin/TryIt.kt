import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val doc = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 120.50 EUR\nThank you."
    fun good(): MutableMap<String, Any?> = linkedMapOf(
        "vendor" to "Acme Tools", "currency" to "EUR", "currency_detail" to null, "line_items" to listOf(100.0, 20.5),
        "stated_total" to 120.5, "calculated_total" to 120.5, "conflict_detected" to false,
        "provenance" to mapOf("vendor" to "Invoice from Acme Tools", "currency" to "120.50 EUR", "stated_total" to "Total due: 120.50 EUR"),
    )

    // A stand-in for the model, like the tests use: it always answers with the same record.
    val result = extractDocument(doc, { _, feedback ->
        println("model called, feedback: $feedback")
        good()
    })
    println("status: ${result?.get("status")}")
    println("attempts: ${result?.get("attempts")}")

    // validate() on its own: a record whose calculated total disagrees with its line items.
    val bad = good().also { it["calculated_total"] = 99.0 }
    println("errors: ${validate(bad, doc)}")
}
