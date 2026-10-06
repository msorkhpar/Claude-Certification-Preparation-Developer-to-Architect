import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Three situations from the scenario bank, described by a few features.
    val situations = linkedMapOf<String, Map<String, Any>>(
        "a rule that must never be broken" to mapOf("guarantee" to true, "knowledge" to "convention"),
        "a database that needs a connection" to mapOf("external_system" to true),
        "work that must run while the laptop is closed" to mapOf("timing" to "interval", "presence" to "away"),
    )
    for ((name, features) in situations) {
        println("$name: ${choose(features)}")
    }
}
