import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // Four test cases, each with its own automated check, like the first main test case.
    @Suppress("UNCHECKED_CAST")
    val cases = Json.parse("""
        [{"id":"c1","input":"I love it","tags":["core"],"check":{"type":"exact","expected":"positive"}},
         {"id":"c2","input":"awful","tags":["core"],"check":{"type":"exact","expected":"negative"}},
         {"id":"c3","input":"order 7","tags":["extract"],"check":{"type":"regex","pattern":"ORD-\\d{4}"}},
         {"id":"c4","input":"meh","tags":["core","edge"],"check":{"type":"exact","expected":"neutral"}}]""") as List<Map<String, Any?>>
    val answers = mapOf("I love it" to "positive", "awful" to "negative", "order 7" to "The order is ORD-0007.", "meh" to "positive")

    // The application under test is a plain function: here it just looks the answer up.
    val report = Harness.runEval(cases, { text -> answers.getValue(text) }, null, 1)

    println("passed: ${report["passed"]} of ${report["total"]}")
    println("pass rate: ${report["pass_rate"]}")
    for (result in report["results"] as? List<*> ?: emptyList<Any?>()) {
        result as Map<*, *>
        println("  ${result["id"]} ${if (result["passed"] == true) "passed" else "failed"} - ${result["reason"]}")
    }
}
