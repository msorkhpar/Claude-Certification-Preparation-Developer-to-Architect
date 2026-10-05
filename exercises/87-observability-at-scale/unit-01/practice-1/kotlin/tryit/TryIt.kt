import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

// Run executes this file. Change the calls in main to try your code; Submit runs the tests.
fun main() {
    // Turn the logger up, so the log.log(DEBUG, ...) lines of your code show under the printed lines.
    System.setProperty("java.util.logging.SimpleFormatter.format", "%4\$s %5\$s%n")
    val handler = ConsoleHandler().apply { level = Level.ALL }
    Logger.getLogger("").apply { level = Level.ALL; addHandler(handler) }

    // One trace of a request: the assistant calls a search, then the model answers slowly.
    val spans = listOf(Span("s1", "", "agent", "assistant", "ok", 1900, ""),
        Span("s2", "s1", "retrieval", "search", "ok", 100, ""), Span("s3", "s1", "llm", "answer", "ok", 1700, ""))
    println("kept as: ${keepTrace("trace-1", spans, 0)}")
    println("kept as (a failed span): ${keepTrace("trace-1", listOf(Span("s1", "", "agent", "assistant", "error", 90, "timeout")), 0)}")

    // Where the failure started, from the deepest failed span.
    val failed = listOf(Span("s1", "", "agent", "assistant", "error", 900, ""), Span("s2", "s1", "tool", "lookup_order", "error", 800, "HTTP 500"))
    println("root cause: ${rootCause(failed)}")

    // A log event without content, and the trail of one request across components.
    println("redacted: ${redact(mapOf("trace" to "t", "tool_input" to "secret", "input_tokens" to 5))}")
    val events = listOf(Event("r1", 30, "tool", "lookup done"), Event("r2", 10, "api", "other"), Event("r1", 10, "api", "received"))
    println("trail: ${requestTrail(events, "r1")}")
}
