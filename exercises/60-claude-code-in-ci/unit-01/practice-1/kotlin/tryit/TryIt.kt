import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A small schema of the model's answer (your review-schema.json is the full one) and the team's policy.
    val schema = com.fasterxml.jackson.databind.ObjectMapper().readTree(
        """{"type":"object","required":["findings"],"properties":{"findings":{"type":"array","items":{"type":"object",
        "required":["file","line","category","severity","issue","suggested_fix","detected_pattern"],"properties":{
        "file":{"type":"string"},"line":{"type":"integer"},"category":{"type":"string"},
        "severity":{"type":"string","enum":["low","medium","high"]},"issue":{"type":"string"},
        "suggested_fix":{"type":"string"},"detected_pattern":{"type":"string"}}}}}}""")
    val policy = mapOf("min_severity" to "medium", "disabled_categories" to listOf("style"), "fail_on" to listOf("high"))

    // What `claude -p --output-format json` prints for a successful run with one finding.
    val stdout = """{"type":"result","subtype":"success","is_error":false,"structured_output":{"findings":[{"file":"api.py","line":12,
        "category":"bug","severity":"medium","issue":"Unchecked None.","suggested_fix":"Return early.","detected_pattern":"missing-none-check"}]}}"""

    println("a valid run: ${ReviewGate.gate(stdout, 0, schema, policy)}")
    println("claude exited with 2: ${ReviewGate.gate(stdout, 2, schema, policy)}")
    println("not JSON at all: ${ReviewGate.gate("Error: no key", 1, schema, policy)}")
    println(ReviewGate.reviewPrompt("+ x = 1").lines().take(3))
}
