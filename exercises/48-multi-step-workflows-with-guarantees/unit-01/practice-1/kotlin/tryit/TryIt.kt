import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val order = mapOf("order_id" to "O1", "customer_id" to "C1", "total_cents" to 5000, "refunded_cents" to 0)
    val refunds = mutableListOf<Map<String, Any?>>()

    // A scripted backend, like the one the tests use: four functions that answer with fixed data.
    val backend: Backend = mapOf(
        "verify_identity" to { args: Map<String, Any?> -> if (args["code"] == "1234") mapOf("verified" to "yes", "customer_id" to "C1") else mapOf("verified" to "no") },
        "lookup_order" to { _: Map<String, Any?> -> order },
        "process_refund" to { args: Map<String, Any?> -> refunds.add(args); mapOf("refund_id" to "R${refunds.size}", "amount_cents" to args["amount_cents"]) },
        "escalate" to { _: Map<String, Any?> -> mapOf("ticket_id" to "T1") },
    )
    val desk = RefundDesk(backend)

    // The model breaks the order of the steps: a refund before identity is verified must be blocked in code.
    val early = desk.call("process_refund", mapOf("order_id" to "O1", "amount_cents" to 2000))
    println("refund before verifying: ${early["content"]} | is_error: ${early["is_error"]}")

    for ((name, args) in listOf("verify_identity" to mapOf("code" to "1234"), "lookup_order" to mapOf("order_id" to "O1"),
        "process_refund" to mapOf("order_id" to "O1", "amount_cents" to 2000))) {
        println("$name: ${desk.call(name, args)["content"]}")
    }
    println("refunds that reached the backend: ${refunds.size}")
}
