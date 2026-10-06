import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A design that sits at every threshold of the review, as the tests' clean design does.
    val cleanFlags = setOf("feedback_loop", "model_measured", "replace_on_change", "deferral", "protected_segment", "rollback", "human_step", "owner",
        "accuracy_stated", "managed_settings", "irreversible_action", "team")
    val numbers = mapOf("team_value_chats" to 15, "tool_tokens" to 10000, "eval_cases" to 20, "rollout_stages" to 3, "retain_days" to 365,
        "floor_days" to 90, "ceiling_days" to 365, "team_size" to 10, "latency_ms" to 2000, "availability_tenths" to 995)

    val findings = launchReview(cleanFlags, numbers)
    println("clean design: $findings -> ${verdict(findings)}")

    // The same design with an agent that does not need to be one, and PII reaching the model.
    val risky = launchReview(cleanFlags + setOf("agent", "path_known", "pii_reaches_model"), numbers)
    println("risky design: $risky -> ${verdict(risky)}")
    println("findings per domain: ${scorecard(risky)}")
    println("accuracy needed when an error costs 250 and a review 5: ${neededAccuracy(250, 5)}")
}
