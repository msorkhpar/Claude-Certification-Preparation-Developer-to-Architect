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
    // TODO: error, slow, retries, feedback, sampled or dropped, in that order of priority.
    return null
}

fun rootCause(spans: List<Span>): Cause? {
    // TODO: the layer, name, why and path of the deepest failing span, or of a stale or empty retrieval when nothing failed.
    return null
}

fun drift(baseline: Map<String, Int>, current: Map<String, Int>, tolerance: Int): List<String>? {
    // TODO: "<name> up|down <percent>%" for each metric that moved by more than the tolerance, sorted by name.
    return null
}

fun alertAt(series: List<Int>, threshold: Int, windows: Int): Int {
    // TODO: the index that completes `windows` consecutive values over the threshold, or -1.
    return -2
}

fun redact(event: Map<String, Any>, allowed: Set<String> = setOf()): Map<String, Any>? {
    // TODO: drop the content fields unless they are allowed by name.
    return null
}

fun requestTrail(events: List<Event>, request: String): List<String>? {
    // TODO: "<component>: <message>" for each event of the request, in time order.
    return null
}
