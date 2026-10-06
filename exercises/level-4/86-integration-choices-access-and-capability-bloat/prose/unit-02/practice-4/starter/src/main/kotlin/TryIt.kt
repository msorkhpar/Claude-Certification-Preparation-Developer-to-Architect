import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    val catalog = linkedMapOf("read_ticket" to Tool("read", 160), "draft_reply" to Tool("draft", 220),
        "issue_refund" to Tool("money", 240), "export_report" to Tool("read", 300))

    // An agent holds more tools than its role needs: the audit names what to remove and which of them are risky.
    val agent = Agent(catalog.keys.toList(), listOf("read_ticket", "draft_reply"), mapOf("read_ticket" to 12, "draft_reply" to 9))
    println("audit: ${audit(agent, catalog)}")

    // Loading a long tool list: the most used tools load now, the rest wait behind a search tool.
    val tools = (1..12).associate { "t%02d".format(it) to 100 }
    println("loading plan: ${planLoading(tools, mapOf("t01" to 9, "t02" to 5), keep = 3)}")

    // The gateway checks one request against the policy.
    val policy = Policy(mapOf("k1" to "support"), mapOf("support" to setOf("standard")), mapOf("support" to setOf("read_ticket", "draft_reply")),
        mapOf("support" to 2), mapOf("standard" to "claude-sonnet-5-5"))
    println("allowed: ${gateway(Request("k1", "standard", "read_ticket", 0), policy)}")
    println("over the limit: ${gateway(Request("k1", "standard", "read_ticket", 2), policy)}")
}
