import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // The catalog and roles the tests use for the first main case m1.
    val catalog = listOf(
        mapOf("name" to "web_search", "tags" to listOf("web")),
        mapOf("name" to "fetch_page", "tags" to listOf("web")),
        mapOf("name" to "load_document", "tags" to listOf("documents")),
        mapOf("name" to "verify_fact", "tags" to listOf("web"), "scoped" to true),
        mapOf("name" to "publish_report", "tags" to listOf("reports"), "irreversible" to true),
    )
    val roles = linkedMapOf<String, Map<String, Any?>>(
        "searcher" to mapOf("specialisation" to listOf("web")),
        "analyst" to mapOf("specialisation" to listOf("documents")),
    )

    val assigned = assignTools(roles, catalog)
    println("searcher tools: ${assigned?.get("searcher")}")
    println("analyst tools: ${assigned?.get("analyst")}")

    // A call to a tool that cannot be undone, checked against the policy.
    val policy = mapOf("tools" to mapOf("process_refund" to mapOf("cap" to 500, "irreversible" to true)))
    val call = mapOf("id" to "c1", "tool" to "process_refund", "amount" to 50, "customer" to "C-1", "verified_customer" to "C-1")
    println("without approval: ${authorize(call, policy, listOf())}")
    println("with approval: ${authorize(call, policy, listOf("c1"))}")
}
