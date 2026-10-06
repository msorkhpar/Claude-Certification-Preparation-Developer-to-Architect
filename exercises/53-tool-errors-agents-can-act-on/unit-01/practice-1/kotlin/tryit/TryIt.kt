import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // A scripted tool, like the one the tests use: it times out twice (a transient failure), then works.
    val script = mutableListOf<Any?>(ToolError("transient", "The billing service timed out after 5 s."),
        ToolError("transient", "The billing service timed out after 5 s."), "refund R-1 created")
    val waits = mutableListOf<Int>()
    val tool = { _: Map<String, Any?> ->
        val step = script.removeAt(0)
        if (step is RuntimeException) throw step
        step
    }

    // The sleep is injected, so nothing really waits: it records the delays asked for.
    val result = runTool(tool, mapOf("order" to "A-7", "amount" to 40), mapOf("max_retries" to 2, "base_delay_ms" to 100)) { waits.add(it) }

    println("ok: ${result["ok"]} | content: ${result["content"]} | attempts: ${result["attempts"]}")
    println("waits (ms): $waits")
    println("tool_result block: ${toToolResult("toolu_1", result)}")
    println("next action: ${nextAction(result)}")
}
