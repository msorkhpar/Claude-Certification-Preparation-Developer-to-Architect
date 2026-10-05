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
    // TODO 1 of 7 (unlocks m1 and e1): why a trace is kept.
    // Receives the trace id, its spans (`status`, `ms`, `kind`, `name`; the first span is the root), the sampling rate (0 to 100) and the flags. Returns the first reason
    // that applies, in this order: "error" when any span has the status "error"; "slow" when the root's `ms` is over `slowMs` (equal is not slow); "retries" when
    // one tool (a span of kind "tool") was called three times or more by name; "feedback" when `feedback` is set; otherwise "sampled" when `bucket(traceId)` is
    // below `rate` and "dropped" when it is not.
    // Example: one tool span called "fetch" three times, nothing else wrong -> "retries"
    return null
}

private fun deepest(failed: List<Span>): Span {
    // TODO 2 of 7 (unlocks e2): the span that is the origin of a failure.
    // Receives the spans that failed (each has `id` and `parent`). Returns the first one that is not the parent of another failed span, so the error that a
    // parent merely reported is passed over for the span below it.
    // Example: s1 (root) <- s2 <- s3, all failed -> s3; two failed children of one parent -> the first of them
    return failed[0]
}

private fun blamedRetrieval(spans: List<Span>): Span? {
    // TODO 3 of 7 (unlocks e3): the retrieval span to blame when nothing failed.
    // Receives the spans. Returns the first span of kind "retrieval" whose `note` is "stale" or "no-hits", or null when there is none.
    // Example: a retrieval span with the note "stale" -> that span
    return null
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
    // TODO 4 of 7 (unlocks e4): the metrics that moved.
    // Receives the baseline and the current values (metric name to a whole number) and a tolerance in percent. Goes through the baseline names in alphabetical order.
    // The change is `Math.abs(now - base) * 100 / base` (integer division); a baseline of 0 counts as 100 when the value is not 0 and 0 when it is. Returns one string
    // "<name> up <pct>%" or "<name> down <pct>%" for each metric whose change is over the tolerance (equal is fine).
    // Example: baseline 10, now 14, tolerance 30 -> [a up 40%]
    return emptyList()
}

fun alertAt(series: List<Int>, threshold: Int, windows: Int): Int {
    // TODO 5 of 7 (unlocks e5): when an alert fires.
    // Receives the series of window values, the threshold and how many windows in a row must be over it (strictly over). Returns the index of the window at which the
    // count of consecutive windows over the threshold first reaches `windows`, or -1 when it never does. A window at or under the threshold starts the count again.
    // Example: [1, 2, 9, 2, 8, 9, 10, 3], threshold 5, windows 3 -> 6
    return -1
}

fun redact(event: Map<String, Any>, allowed: Set<String> = setOf()): Map<String, Any>? {
    // TODO 6 of 7 (unlocks e6): what a log record keeps.
    // Receives a record (a map) and the names that may stay. Returns a new map without the fields in CONTENT (`prompt`, `response`, `tool_input`, `tool_output`) unless the
    // field's name is in `allowed`; every other field stays.
    // Example: {trace: t, prompt: x} -> {trace: t}
    return event.toMap()
}

fun requestTrail(events: List<Event>, request: String): List<String>? {
    // TODO 7 of 7 (unlocks e7): one request's story.
    // Receives the events of every component (`request`, `ts`, `component`, `message`) and a request id. Returns "<component>: <message>" for each event of that request, in
    // order of `ts` (events with the same `ts` stay in the order they came); an unknown request gives an empty list.
    // Example: events at ts 30 (tool), 10 (api), 20 (agent) -> [api: ..., agent: ..., tool: ...]
    return emptyList()
}
