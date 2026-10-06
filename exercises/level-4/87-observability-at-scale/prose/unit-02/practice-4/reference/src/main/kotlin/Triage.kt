private val log = System.getLogger("triage")

/** Trace triage: which traces to keep, the layer that failed, drift, alerts, redaction and one request's trail. See ../../statement.md. */

/** One step of a trace: its id and its parent's id (empty for the root), the kind (agent, llm, tool or retrieval), the name, the status (ok or error), the milliseconds and a note. */
data class Span(val id: String, val parent: String, val kind: String, val name: String, val status: String, val ms: Int, val note: String)

/** The layer and name of the origin, why it is blamed and the path of names from the root to it. */
data class Cause(val layer: String, val name: String, val why: String, val path: List<String>)

/** A log line of one component, tied to a request by its id. */
data class Event(val request: String, val ts: Int, val component: String, val message: String)

val CONTENT = setOf("prompt", "response", "tool_input", "tool_output")

fun bucket(traceId: String): Int {
    var h = 7
    for (c in traceId) h = (h * 31 + c.code) % 1000003
    return h % 100
}

fun keepTrace(traceId: String, spans: List<Span>, rate: Int, feedback: Boolean = false, slowMs: Int = 5000): String? {
    log.log(System.Logger.Level.DEBUG, "keepTrace input: {0}", spans)
    if (spans.any { it.status == "error" }) return "error"
    if (spans[0].ms > slowMs) return "slow"
    val tools = spans.filter { it.kind == "tool" }.map { it.name }
    if (tools.any { n -> tools.count { it == n } >= 3 }) return "retries"
    if (feedback) return "feedback"
    return if (bucket(traceId) < rate) "sampled" else "dropped"
}

private fun deepest(failed: List<Span>): Span {
    val parents = failed.map { it.parent }.toSet()
    return failed.first { it.id !in parents }
}

private fun blamedRetrieval(spans: List<Span>): Span? {
    return spans.firstOrNull { it.kind == "retrieval" && (it.note == "stale" || it.note == "no-hits") }
}

fun rootCause(spans: List<Span>): Cause? {
    val byId = spans.associateBy { it.id }
    val failed = spans.filter { it.status == "error" }
    if (failed.isNotEmpty()) {
        val origin = deepest(failed)
        val path = mutableListOf<String>()
        var cursor: Span? = origin
        while (cursor != null) {
            path.add(cursor.name)
            cursor = byId[cursor.parent]
        }
        return Cause(origin.kind, origin.name, "failed", path.reversed())
    }
    val s = blamedRetrieval(spans)
    if (s != null) return Cause("retrieval", s.name, s.note, listOf(spans[0].name, s.name))
    return Cause("none", "", "no span failed", listOf())
}

fun drift(baseline: Map<String, Int>, current: Map<String, Int>, tolerance: Int): List<String>? {
    val out = mutableListOf<String>()
    for (name in baseline.keys.sorted()) {
        val base = baseline.getValue(name)
        val now = current.getValue(name)
        val pct = if (base != 0) Math.abs(now - base) * 100 / base else if (now != 0) 100 else 0
        if (pct > tolerance) out.add("$name ${if (now > base) "up" else "down"} $pct%")
    }
    return out
}

fun alertAt(series: List<Int>, threshold: Int, windows: Int): Int {
    var run = 0
    for ((i, value) in series.withIndex()) {
        run = if (value > threshold) run + 1 else 0
        if (run >= windows) return i
    }
    return -1
}

fun redact(event: Map<String, Any>, allowed: Set<String> = setOf()): Map<String, Any>? {
    return event.filter { it.key !in CONTENT || it.key in allowed }
}

fun requestTrail(events: List<Event>, request: String): List<String>? {
    return events.filter { it.request == request }.sortedBy { it.ts }.map { "${it.component}: ${it.message}" }
}
