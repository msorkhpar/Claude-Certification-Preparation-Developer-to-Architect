/**
 * Evaluation decisions for a system that changes: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits.
 *
 * The cases, counts and timings are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domain 4) and the Claude documentation pages on defining success, developing tests, reducing hallucinations and reducing latency, read on 2026-10-04. Nothing here calls a model.
 */

private val log = System.getLogger("eval_report")

data class Row(val segment: String, val oldOk: Boolean, val newOk: Boolean)

data class Line(val segment: String, val cases: Int, val right: Int, val percent: Int, val cost: Int)

data class Option(val name: String, val accuracy: Int, val p95: Int, val cost: Int)

data class Gate(val decision: String, val lost: Int, val gained: Int, val blocked: List<String>)

val COSTS = mapOf("order status" to 1, "refund" to 20, "policy" to 5, "complaint" to 10)

/** One row per case: right in both, right only with the current prompt, right only with the new one, then wrong in both. */
fun buildCases(): List<Row> {
    val rows = mutableListOf<Row>()
    val segments = listOf("order status", "refund", "policy", "complaint")
    val counts = listOf(30, 8, 10, 4)
    val both = listOf(30, 5, 8, 3)
    val oldOnly = listOf(0, 0, 1, 1)
    val newOnly = listOf(0, 2, 0, 0)
    for (g in segments.indices) {
        val rest = counts[g] - both[g] - oldOnly[g] - newOnly[g]
        repeat(both[g]) { rows.add(Row(segments[g], true, true)) }
        repeat(oldOnly[g]) { rows.add(Row(segments[g], true, false)) }
        repeat(newOnly[g]) { rows.add(Row(segments[g], false, true)) }
        repeat(rest) { rows.add(Row(segments[g], false, false)) }
    }
    return rows
}

/** Whole percent, half up, with integers only so that every language agrees. */
fun pct(part: Int, whole: Int): Int = if (whole != 0) (200 * part + whole) / (2 * whole) else 0

/** Per segment: cases, right answers, accuracy and the cost of the wrong ones, worst cost first. */
fun segmentTable(rows: List<Row>, which: String): List<Line> {
    val out = linkedMapOf<String, IntArray>()
    for (row in rows) {
        val ok = if (which == "old") row.oldOk else row.newOk
        val counts = out.getOrPut(row.segment) { IntArray(2) }
        counts[0]++
        if (ok) counts[1]++
    }
    val table = out.map { (s, c) -> Line(s, c[0], c[1], pct(c[1], c[0]), (c[0] - c[1]) * (COSTS[s] ?: 1)) }
    return table.sortedWith(compareBy<Line>({ -it.cost }, { it.segment }))
}

/** Nearest rank: the value at rank ceil(p * n / 100) of the sorted list. */
fun percentile(values: List<Int>, p: Int): Int {
    if (values.isEmpty()) return 0
    val ordered = values.sorted()
    return ordered[(p * ordered.size + 99) / 100 - 1]
}

/** Two-proportion test at 95 percent, done with integers: z squared = D*D*N / (n1*n2*X*(N-X)), compared with 1.96 squared. */
fun abVerdict(x1: Int, n1: Int, x2: Int, n2: Int, minN: Int = 200): String {
    if (n1 < minN || n2 < minN) return "too few cases"
    val bigN = (n1 + n2).toLong()
    val x = (x1 + x2).toLong()
    if (x == 0L || x == bigN) return "no clear difference"
    val d = x2.toLong() * n1 - x1.toLong() * n2
    if (d * d * bigN * 10000L < 38416L * n1 * n2 * x * (bigN - x)) return "no clear difference"
    return if (d > 0) "new is better" else "old is better"
}

/** Ship only when no protected segment lost a right answer and the new version lost fewer than it gained. */
fun shadowGate(rows: List<Row>, protectedSegments: Set<String>): Gate {
    log.log(System.Logger.Level.DEBUG, "shadowGate input: {0}", rows)
    val lost = rows.filter { it.oldOk && !it.newOk }.map { it.segment }
    val gained = rows.count { it.newOk && !it.oldOk }
    val blocked = lost.filter { it in protectedSegments }.toSortedSet().toList()
    val ship = blocked.isEmpty() && lost.size <= gained
    return Gate(if (ship) "ship" else "hold", lost.size, gained, blocked)
}

/** Where to look first: the evidence, then the grounding, then the format, then the task, and the model last. */
fun diagnose(found: Boolean, supported: Boolean, formatOk: Boolean, passesOnStronger: Boolean): String {
    if (!found) return "retrieval or data"
    if (!supported) return "ungrounded answer"
    if (!formatOk) return "format instructions"
    if (!passesOnStronger) return "prompt or task"
    return "model mismatch"
}

/** The cheapest option that meets the accuracy floor and the latency limit, ties by name; none when nothing does. */
fun chooseModel(options: List<Option>, minAccuracy: Int, maxP95: Int): String =
    options.filter { it.accuracy >= minAccuracy && it.p95 <= maxP95 }.sortedWith(compareBy<Option>({ it.cost }, { it.name })).firstOrNull()?.name ?: "none"

fun main() {
    val rows = buildCases()
    for ((which, label) in listOf("old" to "current prompt", "new" to "new prompt")) {
        val table = segmentTable(rows, which)
        val right = table.map { it.right }.sum()
        println("$label: $right/${rows.size} right, ${pct(right, rows.size)}% overall, error cost ${table.map { it.cost }.sum()}")
        for (l in table) println("  ${l.segment.padEnd(13)} ${l.right}/${l.cases} ${l.percent}% cost ${l.cost}")
    }
    val latencies = listOf(800, 820, 850, 900, 950, 980, 1000, 1100, 1200, 1500, 2400, 4800)
    println("latency ms: mean ${latencies.sum() / latencies.size}, p50 ${percentile(latencies, 50)}, p95 ${percentile(latencies, 95)}")
    println("live test, 500 cases each, 410 right against 438: ${abVerdict(410, 500, 438, 500)}")
    println("live test, 500 cases each, 410 right against 425: ${abVerdict(410, 500, 425, 500)}")
    println("live test, 100 cases each, 82 right against 90: ${abVerdict(82, 100, 90, 100)}")
    val gate = shadowGate(rows, setOf("refund", "complaint"))
    println("shadow run: ${gate.decision}, lost ${gate.lost}, gained ${gate.gained}, protected segments hit: ${if (gate.blocked.isEmpty()) "none" else gate.blocked.joinToString(", ")}")
    println("diagnose, no chunk had the answer: ${diagnose(false, false, true, true)}")
    println("diagnose, a claim no chunk supports: ${diagnose(true, false, true, true)}")
    println("diagnose, a reply in the wrong shape: ${diagnose(true, true, false, true)}")
    println("diagnose, fails on a stronger model too: ${diagnose(true, true, true, false)}")
    println("diagnose, passes only on a stronger model: ${diagnose(true, true, true, true)}")
    val options = listOf(Option("small", 84, 900, 1), Option("medium", 91, 1800, 3), Option("large", 95, 4200, 9))
    println("model for 90% accuracy within 2000 ms: ${chooseModel(options, 90, 2000)}; for 94% within 2000 ms: ${chooseModel(options, 94, 2000)}")
}
