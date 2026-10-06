private val log = System.getLogger("trace_triage")

/**
 * Observability decisions for a system of agents and tools: which traces to keep, how to find the layer that failed, when a change in a metric is drift, when to alert and what a log record may hold.
 *
 * The traces, metrics and events are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domains 3 and 4), Anthropic's article on its multi-agent research system
 * and the Claude Code monitoring documentation, read on 2026-10-04. Nothing here calls a model.
 */
data class Span(val id: String, val parent: String, val kind: String, val name: String, val status: String, val ms: Int, val note: String)

data class Cause(val layer: String, val name: String, val why: String, val path: List<String>)

val CONTENT = setOf("prompt", "response", "tool_input", "tool_output")
val TRACES: Map<String, List<Span>> = linkedMapOf(
    "t-refund" to listOf(Span("s1", "", "agent", "orchestrator", "error", 9200, ""), Span("s2", "s1", "agent", "order-researcher", "error", 8700, ""), Span("s3", "s2", "llm", "plan", "ok", 900, ""),
        Span("s4", "s2", "tool", "web_fetch", "error", 5000, ""), Span("s5", "s1", "llm", "summarise", "ok", 400, "")),
    "t-policy" to listOf(Span("s1", "", "agent", "assistant", "ok", 2100, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 120, "stale"), Span("s3", "s1", "llm", "answer", "ok", 1800, "")),
    "t-empty" to listOf(Span("s1", "", "agent", "assistant", "ok", 1500, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 90, "no-hits"), Span("s3", "s1", "llm", "answer", "ok", 1300, "")),
    "t-plain" to listOf(Span("s1", "", "agent", "assistant", "ok", 1900, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 110, ""), Span("s3", "s1", "llm", "answer", "ok", 1700, "")),
)

/** A number from 0 to 99 that depends on the trace id alone, the same in every agent and every language. */
fun bucket(traceId: String): Int {
    var h = 7
    for (c in traceId) h = (h * 31 + c.code) % 1000003
    return h % 100
}

/** Tail-based sampling: a trace with an error, a slow root or a bad-answer flag is always kept, and the rest are kept by their id at `rate` percent. */
fun keepReason(traceId: String, spans: List<Span>, rate: Int, feedback: Boolean = false, slowMs: Int = 5000): String {
    if (spans.any { it.status == "error" }) return "error"
    if (spans[0].ms > slowMs) return "slow"
    if (feedback) return "feedback"
    return if (bucket(traceId) < rate) "sampled" else "dropped"
}

/** The deepest failing span is the origin, not the span that reported the error; with no failure, a retrieval that returned stale or no chunks is blamed. */
fun rootCause(spans: List<Span>): Cause {
    log.log(System.Logger.Level.DEBUG, "rootCause input: {0}", spans)
    val byId = spans.associateBy { it.id }
    val failed = spans.filter { it.status == "error" }
    if (failed.isNotEmpty()) {
        val parents = failed.map { it.parent }.toSet()
        val origin = failed.first { it.id !in parents }
        val path = mutableListOf<String>()
        var cursor: Span? = origin
        while (cursor != null) {
            path.add(cursor.name)
            cursor = byId[cursor.parent]
        }
        return Cause(origin.kind, origin.name, "failed", path.reversed())
    }
    for (s in spans) if (s.kind == "retrieval" && (s.note == "stale" || s.note == "no-hits")) return Cause("retrieval", s.name, s.note, listOf(spans[0].name, s.name))
    return Cause("none", "", "no span failed", listOf())
}

/** Metrics whose relative change since the baseline is over `tolerance` percent, in either direction. */
fun drift(baseline: Map<String, Int>, current: Map<String, Int>, tolerance: Int): List<String> {
    val out = mutableListOf<String>()
    for (name in baseline.keys.sorted()) {
        val base = baseline.getValue(name)
        val now = current.getValue(name)
        val pct = if (base != 0) Math.abs(now - base) * 100 / base else if (now != 0) 100 else 0
        if (pct > tolerance) out.add("$name ${if (now > base) "up" else "down"} $pct%")
    }
    return out
}

/** The index of the window that completes `windows` consecutive values over the threshold, or -1. */
fun alertAt(series: List<Int>, threshold: Int, windows: Int): Int {
    var run = 0
    for ((i, value) in series.withIndex()) {
        run = if (value > threshold) run + 1 else 0
        if (run >= windows) return i
    }
    return -1
}

/** A log record keeps ids, counts and timings and drops the content fields unless they are allowed by name. */
fun redact(event: Map<String, Any>, allowed: Set<String> = setOf()): Map<String, Any> = event.filter { it.key !in CONTENT || it.key in allowed }

fun main() {
    val healthy = (0 until 100).map { "trace-$it" }
    val kept = healthy.count { keepReason(it, TRACES.getValue("t-plain"), 10) == "sampled" }
    val same = kept == healthy.count { bucket(it) < 10 }
    println("100 healthy traces at a 10 percent rate: $kept kept by id, the same $kept in every agent: ${if (same) "True" else "False"}")
    for ((name, spans) in TRACES) {
        val cause = rootCause(spans)
        println("$name: kept as ${keepReason(name, spans, 0)}; cause: " + listOf(cause.layer, cause.name, cause.why).filter { it.isNotEmpty() }.joinToString(" "))
    }
    println("path of t-refund: " + rootCause(TRACES.getValue("t-refund")).path.joinToString(" > "))
    println("a bad-answer flag keeps t-plain at rate 0: " + keepReason("t-plain", TRACES.getValue("t-plain"), 0, true))
    val drifted = drift(
        mapOf("retrieval_hits" to 5, "refusals_per_1000" to 4, "tokens_per_answer" to 900, "tool_errors_per_1000" to 12),
        mapOf("retrieval_hits" to 3, "refusals_per_1000" to 4, "tokens_per_answer" to 1260, "tool_errors_per_1000" to 13), 25)
    println("drift against last week, tolerance 25%: " + drifted.joinToString(", "))
    val series = listOf(1, 2, 9, 2, 8, 9, 10, 3)
    println("error rate per window $series, threshold 5: one window over fires at ${alertAt(series, 5, 1)}, three in a row fire at ${alertAt(series, 5, 3)}")
    val event = linkedMapOf<String, Any>("trace" to "t-1", "model" to "claude-sonnet-5-5", "input_tokens" to 1200, "output_tokens" to 300, "tool" to "lookup_order", "status" to "ok", "prompt" to "(text)", "tool_input" to "(text)")
    val plain = redact(event).keys.sorted()
    val extra = redact(event, setOf("tool_input")).keys.filter { it !in plain }.sorted()
    println("log record keeps: " + plain.joinToString(", ") + "; with tool_input allowed by name: " + extra.joinToString(", "))
}
