/** Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md. */

/** One graded case: its segment and whether the answer was right. */
data class Result(val segment: String, val correct: Boolean)

/** One line of the report: the segment, its cases, the right answers, the whole percent and the cost of the wrong ones. */
data class Line(val segment: String, val cases: Int, val right: Int, val percent: Int, val cost: Int)

/** One case run on the old and the new version. */
data class Paired(val segment: String, val oldOk: Boolean, val newOk: Boolean)

/** The shadow gate's decision, the right answers lost and gained, and the protected segments that lost. */
data class Gate(val decision: String, val lost: Int, val gained: Int, val blocked: List<String>)

/** A model option: its name, accuracy in percent, 95th-percentile latency in ms and cost. */
data class Option(val name: String, val accuracy: Int, val p95: Int, val cost: Int)

fun pct(part: Int, whole: Int): Int = if (whole != 0) (200 * part + whole) / (2 * whole) else 0

fun segmentTable(results: List<Result>, costs: Map<String, Int>): List<Line>? {
    // TODO: one line per segment, the highest error cost first, ties by segment name.
    return null
}

fun percentile(values: List<Int>, p: Int): Int {
    // TODO: the nearest-rank percentile of the values, 0 for no values.
    return -1
}

fun abVerdict(x1: Int, n1: Int, x2: Int, n2: Int, minN: Int = 200): String? {
    // TODO: too few cases, no clear difference, new is better or old is better, at 95 percent.
    return null
}

fun shadowGate(pairs: List<Paired>, protectedSegments: Set<String>): Gate? {
    // TODO: the decision, the losses, the gains and the protected segments that lost, for a shadow run of the new version against the old.
    return null
}

fun diagnose(found: Boolean, supported: Boolean, formatOk: Boolean, passesOnStronger: Boolean): String? {
    // TODO: retrieval or data, ungrounded answer, format instructions, prompt or task, or model mismatch, in that order.
    return null
}

fun chooseModel(options: List<Option>, minAccuracy: Int, maxP95: Int): String? {
    // TODO: the cheapest option that meets the accuracy floor and the latency limit, or none.
    return null
}
