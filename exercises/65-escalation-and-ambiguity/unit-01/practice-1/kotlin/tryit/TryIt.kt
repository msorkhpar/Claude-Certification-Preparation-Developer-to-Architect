import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A customer who asks for a person is escalated at once; a calm one the agent can resolve is not.
    val asked = Case(askedForPerson = true, matches = 1, policyCovers = true, attemptsWithoutProgress = 0, sentiment = "calm", confidence = 95)
    println("asked for a person: ${decide(asked)}")
    val calm = asked.copy(askedForPerson = false)
    println("calm and covered: ${decide(calm)}")

    // Two customers match the name: ask only for the field that tells them apart.
    val matches = listOf(
        mapOf("id" to "c1", "name" to "Ana Ruiz", "email" to "ana@example.com", "zip" to "10115"),
        mapOf("id" to "c2", "name" to "Ana Ruiz", "email" to "ana.r@example.com", "zip" to "10115"))
    println("ask for: ${clarifyingFields(matches)}")

    // The hand-off carries the facts, not the transcript.
    val handoff = HandoffCase("C-77", "refund over the limit", "duplicate charge", "\$129.50",
        listOf("verified identity", "checked order"), "approve the refund", "user: hello ... 40 turns ...")
    println(handoffText(handoff))
}
