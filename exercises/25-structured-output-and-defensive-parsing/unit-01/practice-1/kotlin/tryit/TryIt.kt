import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val doc = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business."
    @Suppress("UNCHECKED_CAST")
    val schema = Json.parse("""
        {"type":"object","required":["vendor","total","currency","evidence"],
         "properties":{"vendor":{"type":"string"},"total":{"type":"number","minimum":0},
                       "currency":{"type":"string","enum":["USD","EUR","GBP"]},"evidence":{"type":"string"}},
         "additionalProperties":false}""") as Map<String, Any?>
    val good = """{"vendor":"Acme Tools","total":120.5,"currency":"EUR","evidence":"Total due: 120.50 EUR"}"""

    // A hand-written stand-in for the model, in the shape of a Messages API reply: it always answers with valid JSON.
    val ask: Ask = { _ ->
        mapOf("type" to "message", "role" to "assistant", "stop_reason" to "end_turn",
            "content" to listOf(mapOf("type" to "text", "text" to good)))
    }

    val result = extract(ask, doc, schema, 3, listOf("evidence"))

    println("status: ${result["status"]}")
    println("attempts: ${result["attempts"]}")
    println("value: ${result["value"]}")
    println("errors: ${result["errors"]}")
}
