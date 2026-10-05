import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // The retirement calendar: days left per model, nearest first, with the level of urgency.
    val models = listOf(Model("claude-old-a", "2026-10-18", true), Model("claude-old-b", "2026-11-30", false), Model("claude-old-c", "2027-01-01", false))
    for (line in retirementStatus(models, "2026-10-04")) println("calendar: $line")

    // A request written for an older model, and what the migration changes in it.
    val old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, 40.0, "disabled", "any", false, true)
    val migration = migrateRequest(old)
    println("migrated model: ${migration.request.model}")
    for (change in migration.changes) println("change: $change")

    // A staged rollout: one decision per stage from the traffic and the errors seen so far.
    println("stage 1: ${rolloutStep(1, 2000, 6, 1000, 5)}")
    println("stage 25: ${rolloutStep(25, 50000, 400, 1000, 5)}")
}
