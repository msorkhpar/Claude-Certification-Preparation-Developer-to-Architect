import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // The scratchpad: one line per finding, a fact is recorded once per area.
    var findings = addFinding(emptyList(), "auth", "tokens are signed in TokenSigner", "auth/TokenSigner.java:12")
    findings = addFinding(findings, "billing", "invoices use cents", "billing/Money.java:5")
    findings = addFinding(findings, "auth", "tokens are signed in TokenSigner", "auth/Other.java:99")
    println(renderScratchpad(findings))

    // The manifest of the subagents, and what to do with each after a crash.
    val agents = listOf(
        AgentEntry("search", "state/search.md", "running"),
        AgentEntry("auth", "state/auth.md", "done"),
        AgentEntry("billing", "state/billing.md", "failed"))
    val manifest = buildManifest(agents)
    println("manifest: $manifest")
    println("resume plan: ${resumePlan(manifest, setOf("state/auth.md", "state/search.md"))}")
    println("compact command: ${compactCommand(listOf("the open questions", "file paths"))}")
}
