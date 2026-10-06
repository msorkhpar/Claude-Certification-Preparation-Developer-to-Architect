import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Jobs a team wants to repeat: each is a small map of what is known about it.
    val jobs = linkedMapOf<String, Map<String, Any>>(
        "check the build while I watch" to mapOf(),
        "poll a status page every 5 minutes" to mapOf("interval_seconds" to 300),
        "nightly report, laptop may be closed" to mapOf("interval_seconds" to 86400, "machine_off" to true),
        "react to a pull request unattended" to mapOf("trigger" to "event", "repo_event" to true),
        "run in a pipeline" to mapOf("ci" to true, "interval_seconds" to 600),
    )
    for ((name, job) in jobs) println("$name: ${choose(job)}")
}
