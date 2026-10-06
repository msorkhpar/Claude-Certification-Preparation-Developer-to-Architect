import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A well-made tool, like the one the tests start from: a clear description with when to use it and when not.
    val good = mapOf(
        "name" to "lookup_order",
        "description" to ("Looks up one order by its id and returns its status, items and total in cents. Use when the customer gives an order id " +
            "such as A-1042 or asks where an order is. Do not use it to find a customer by name; use get_customer instead of " +
            "this tool for that. It returns no payment details."),
        "input_schema" to mapOf("type" to "object", "required" to listOf("order_id"),
            "properties" to mapOf("order_id" to mapOf("type" to "string", "description" to "The order id, for example A-1042."))),
        "input_examples" to listOf(mapOf("order_id" to "A-1042")),
        "annotations" to mapOf("readOnlyHint" to true),
    )
    // A poor one: a vague name, a short description and a parameter nobody explained.
    val poor = mapOf("name" to "helper", "description" to "Gets stuff.",
        "input_schema" to mapOf("type" to "object", "properties" to mapOf("q" to mapOf("type" to "string")), "required" to listOf("q")))

    println("good tool: ${lintTool(good)}")
    val poorRules = lintTool(poor)
    println("poor tool: $poorRules")
    println("rules the poor tool breaks: ${poorRules?.size ?: 0}")
}
