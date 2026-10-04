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
    val seen = linkedMapOf<String, IntArray>()
    for (r in results) {
        val counts = seen.getOrPut(r.segment) { IntArray(2) }
        counts[0]++
        if (r.correct) counts[1]++
    }
    val table = seen.map { (s, c) -> Line(s, c[0], c[1], pct(c[1], c[0]), (c[0] - c[1]) * (costs[s] ?: 1)) }
    return table.sortedWith(compareBy<Line>({ -it.cost }, { it.segment }))
}

fun percentile(values: List<Int>, p: Int): Int {
    if (values.isEmpty()) return 0
    val ordered = values.sorted()
    return ordered[(p * ordered.size + 99) / 100 - 1]
}

fun abVerdict(x1: Int, n1: Int, x2: Int, n2: Int, minN: Int = 200): String? {
    if (n1 < minN || n2 < minN) return "too few cases"
    val bigN = (n1 + n2).toLong()
    val x = (x1 + x2).toLong()
    if (x == 0L || x == bigN) return "no clear difference"
    val d = x2.toLong() * n1 - x1.toLong() * n2
    if (d * d * bigN * 10000L < 38416L * n1 * n2 * x * (bigN - x)) return "no clear difference"
    return if (d > 0) "new is better" else "old is better"
}

fun shadowGate(pairs: List<Paired>, protectedSegments: Set<String>): Gate? {
    val lost = pairs.filter { it.oldOk && !it.newOk }.map { it.segment }
    val gained = pairs.count { it.newOk && !it.oldOk }
    val blocked = lost.filter { it in protectedSegments }.toSortedSet().toList()
    val ship = blocked.isEmpty() && lost.size <= gained
    return Gate(if (ship) "ship" else "hold", lost.size, gained, blocked)
}

fun diagnose(found: Boolean, supported: Boolean, formatOk: Boolean, passesOnStronger: Boolean): String? {
    if (!found) return "retrieval or data"
    if (!supported) return "ungrounded answer"
    if (!formatOk) return "format instructions"
    if (!passesOnStronger) return "prompt or task"
    return "model mismatch"
}

fun chooseModel(options: List<Option>, minAccuracy: Int, maxP95: Int): String? =
    options.filter { it.accuracy >= minAccuracy && it.p95 <= maxP95 }.sortedWith(compareBy<Option>({ it.cost }, { it.name })).firstOrNull()?.name ?: "none"
