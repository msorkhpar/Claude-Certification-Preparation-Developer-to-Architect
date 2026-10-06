/** Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md. */

private val log = System.getLogger("evalkit")

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

/**
 * TODO 1 of 7 (unlocks m1 and e1): the cost of the wrong answers of one segment.
 * Receives the number of wrong answers, the segment name and the costs by segment. Returns wrong times the segment's cost, 1 when the
 * segment has no entry. Example: errorCost(3, "refund", mapOf("refund" to 20)) -> 60, errorCost(2, "odd", mapOf("refund" to 20)) -> 2
 */
private fun errorCost(wrong: Int, segment: String, costs: Map<String, Int>): Int = 0

/**
 * TODO 2 of 7 (unlocks m1 and e1): order the report lines.
 * Receives the lines. Returns them with the highest cost first and equal costs by segment name.
 * Example: a line with cost 5 comes before a line with cost 0, and two lines with cost 1 are ordered "a" before "b".
 */
private fun order(table: List<Line>): List<Line> = table

fun segmentTable(results: List<Result>, costs: Map<String, Int>): List<Line> {
    log.log(System.Logger.Level.DEBUG, "segmentTable input: {0}", results)
    val seen = linkedMapOf<String, IntArray>()
    for (r in results) {
        val counts = seen.getOrPut(r.segment) { IntArray(2) }
        counts[0]++
        if (r.correct) counts[1]++
    }
    return order(seen.map { (s, c) -> Line(s, c[0], c[1], pct(c[1], c[0]), errorCost(c[0] - c[1], s, costs)) })
}

/**
 * TODO 3 of 7 (unlocks e2): the nearest-rank percentile.
 * Receives the values in any order and p from 1 to 100. Returns the value at rank ceil(p * n / 100) of the sorted values, counting from 1,
 * and 0 for no values. Example: percentile(listOf(4800, 800, 1000, 900), 95) -> 4800
 */
fun percentile(values: List<Int>, p: Int): Int {
    return -1
}

/**
 * TODO 4 of 7 (unlocks e3 and e4): the A/B verdict at 95 percent.
 * Receives the right answers and cases of the old version (x1 of n1) and the new one (x2 of n2). Returns "too few cases" when an arm has
 * fewer than minN cases, "no clear difference" when the pooled right answers are 0 or all, otherwise the integer test from the statement:
 * "new is better", "old is better" or "no clear difference". Example: abVerdict(410, 500, 438, 500) -> "new is better"
 */
fun abVerdict(x1: Int, n1: Int, x2: Int, n2: Int, minN: Int = 200): String {
    return ""
}

/**
 * TODO 5 of 7 (unlocks e5): ship or hold.
 * Receives the protected segments that lost, the number lost and the number gained. Returns "hold" when a protected segment lost or more
 * were lost than gained, "ship" otherwise. Example: decision(emptyList(), 2, 2) -> "ship", decision(listOf("refund"), 1, 5) -> "hold"
 */
private fun decision(blocked: List<String>, lost: Int, gained: Int): String = ""

fun shadowGate(pairs: List<Paired>, protectedSegments: Set<String>): Gate {
    val lost = pairs.filter { it.oldOk && !it.newOk }.map { it.segment }
    val gained = pairs.count { it.newOk && !it.oldOk }
    val blocked = lost.filter { it in protectedSegments }.toSortedSet().toList()
    return Gate(decision(blocked, lost.size, gained), lost.size, gained, blocked)
}

/**
 * TODO 6 of 7 (unlocks e6): where to look first for a wrong answer.
 * Receives four booleans. Returns "retrieval or data" when the evidence was not found, then "ungrounded answer" when it is not supported,
 * then "format instructions" when the format is wrong, then "prompt or task" when it fails on a stronger model, else "model mismatch".
 * Example: diagnose(true, true, false, true) -> "format instructions"
 */
fun diagnose(found: Boolean, supported: Boolean, formatOk: Boolean, passesOnStronger: Boolean): String {
    return ""
}

/**
 * TODO 7 of 7 (unlocks e7): the cheapest model that meets both limits.
 * Receives options (name, accuracy, p95, cost). Returns the name of the cheapest one with accuracy >= minAccuracy and p95 <= maxP95,
 * equal costs by name, or "none". Example: chooseModel(listOf(Option("a", 90, 100, 2)), 95, 100) -> "none"
 */
fun chooseModel(options: List<Option>, minAccuracy: Int, maxP95: Int): String {
    return ""
}
