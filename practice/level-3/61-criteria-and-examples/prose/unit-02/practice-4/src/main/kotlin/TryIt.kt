import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A review specification like the tests use: explicit criteria first, then examples of both verdicts.
    val spec = mapOf(
        "criteria" to listOf(linkedMapOf(
            "id" to "bug",
            "report" to "A comment whose claimed behaviour contradicts what the code does.",
            "skip" to "Minor style, naming and patterns the codebase already uses.",
            "severity" to linkedMapOf("high" to "A null dereference on a request path.", "low" to "A misleading variable name."),
        )),
        "examples" to listOf(
            linkedMapOf("verdict" to "report", "category" to "bug", "code" to "total = price * qty  # sum of the line items",
                "reason" to "The comment says the line sums items, the code multiplies."),
            linkedMapOf("verdict" to "skip", "code" to "for i in range(n):  # loop", "reason" to "Terse and accurate."),
        ),
    )
    val prompt = buildReviewPrompt(spec, "+ x = 1")
    println("prompt lines: ${prompt?.lines()?.size}")
    println("first line: ${prompt?.lines()?.first()}")
    println("diff is last: ${prompt?.trimEnd()?.endsWith("</diff>")}")

    // Which categories to switch off, from what reviewers accepted or dismissed.
    fun finding(verdict: String) = mapOf("category" to "style", "verdict" to verdict, "detected_pattern" to "line-length")
    val findings = List(4) { finding("dismissed") } + finding("accepted")
    println("category report: ${categoryReport(findings)}")
}
