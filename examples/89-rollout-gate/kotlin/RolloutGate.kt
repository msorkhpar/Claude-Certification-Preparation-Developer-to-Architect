import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Moving a system to a new model at scale: the calendar of retirements, the settings a new model refuses, a gate that a regression suite must pass, and a staged roll-out with a way back.
 *
 * The cases, costs, timings and counts are invented. The model names and dates are the ones the Claude documentation listed on 2026-10-04; the rules about settings are those of its migration guide for Claude Sonnet 5.5. Nothing here calls a model.
 */
data class Case(val id: String, val segment: String, val mustPass: Boolean, val oldOk: Boolean, val newOk: Boolean, val oldCost: Int, val newCost: Int, val newMs: Int)

data class Request(val model: String, val temperature: Double?, val topP: Double?, val topK: Double?, val thinking: String, val toolChoice: String, val strict: Boolean, val prefill: Boolean)

data class Model(val name: String, val date: String, val tentative: Boolean)

data class Verdict(val decision: String, val reasons: List<String>)

val STAGES = listOf(1, 5, 25, 100)
const val TARGET = "claude-sonnet-5-5"

fun daysUntil(today: String, date: String): Long = ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(date))

fun level(days: Long): String = if (days < 0) "retired" else if (days <= 14) "urgent" else if (days <= 60) "migrate now" else "watch"

/** One line per model, the nearest retirement first. A date marked tentative is a date that may move later. */
fun retirementStatus(models: List<Model>, today: String): List<String> =
    models.sortedWith(compareBy<Model>({ daysUntil(today, it.date) }, { it.name }, { it.tentative })).map {
        val days = daysUntil(today, it.date)
        "${it.name}: $days days, ${level(days)}${if (it.tentative) " (tentative)" else ""}"
    }

/** The settings the target model refuses are removed or replaced, and each change is named. */
fun migrateRequest(request: Request): Pair<Request, List<String>> {
    val changes = mutableListOf<String>()
    if (request.model != TARGET) changes.add("model set to $TARGET")
    if (request.temperature != null) changes.add("removed temperature")
    if (request.topP != null) changes.add("removed top_p")
    if (request.topK != null) changes.add("removed top_k")
    var thinking = request.thinking
    if (thinking == "budget") {
        thinking = "adaptive"
        changes.add("thinking budget replaced by adaptive thinking; sweep the effort")
    } else if (thinking == "disabled") {
        thinking = "between_tools"
        changes.add("thinking disabled replaced by between_tools")
    }
    var toolChoice = request.toolChoice
    var strict = request.strict
    if (toolChoice == "any" || toolChoice == "tool") {
        toolChoice = "auto"
        strict = true
        changes.add("forced tool choice replaced by auto with strict tools")
    }
    if (request.prefill) changes.add("assistant prefill removed; state the format in the instructions")
    return Pair(Request(TARGET, null, null, null, thinking, toolChoice, strict, false), changes)
}

fun percentile(values: List<Int>, p: Int): Int {
    if (values.isEmpty()) return 0
    val ordered = values.sorted()
    return ordered[(p * ordered.size + 99) / 100 - 1]
}

/** A go needs every check to pass; every check that fails adds a reason, in a fixed order. */
fun gate(cases: List<Case>, protectedSegments: Set<String>, maxCostUp: Int, maxP95: Int): Verdict {
    val reasons = mutableListOf<String>()
    val failed = cases.filter { it.mustPass && !it.newOk }.map { it.id }.sorted()
    if (failed.isNotEmpty()) reasons.add("must-pass failed: " + failed.joinToString(", "))
    val lost = cases.filter { it.oldOk && !it.newOk }
    val gained = cases.filter { it.newOk && !it.oldOk }
    val hit = lost.filter { it.segment in protectedSegments }.map { it.segment }.toSortedSet().toList()
    if (hit.isNotEmpty()) reasons.add("protected segment lost answers: " + hit.joinToString(", "))
    if (lost.size > gained.size) reasons.add("net loss: lost ${lost.size}, gained ${gained.size}")
    val oldTotal = cases.map { it.oldCost }.sum()
    val newTotal = cases.map { it.newCost }.sum()
    val up = if (oldTotal > 0 && newTotal > oldTotal) (newTotal - oldTotal) * 100 / oldTotal else 0
    if (up > maxCostUp) reasons.add("cost up $up% over the $maxCostUp% limit")
    val p95 = percentile(cases.map { it.newMs }, 95)
    if (p95 > maxP95) reasons.add("p95 latency $p95 ms over the $maxP95 ms limit")
    return Verdict(if (reasons.isEmpty()) "go" else "no-go", reasons)
}

/** Hold until the stage has enough requests, roll back to zero when errors pass the limit, otherwise go on. */
fun rolloutStep(stage: Int, requests: Int, errors: Int, minRequests: Int, maxErrorsPer1000: Int): String {
    if (requests < minRequests) return "hold at $stage"
    if (errors * 1000L / requests > maxErrorsPer1000) return "rollback to 0"
    if (stage == STAGES.last()) return "complete"
    return "advance to ${STAGES[STAGES.indexOf(stage) + 1]}"
}

fun suite(): List<Case> {
    val rows = mutableListOf<Case>()
    val billing = listOf(900, 950, 1000, 1100, 1200)
    for (i in 1..5) rows.add(Case("b$i", "billing", true, true, true, 4, 5, billing[i - 1]))
    val refund = listOf(1500, 1600, 1700, 1800, 2100)
    for (i in 1..5) rows.add(Case("r$i", "refund", i <= 2, true, i != 4, 6, 8, refund[i - 1]))
    for (i in 1..10) rows.add(Case("f$i", "faq", false, i !in listOf(8, 9, 10), i != 10, 2, 3, 500 + 20 * i + 80 * (i / 2)))
    return rows
}

fun main() {
    val models = listOf(Model("claude-haiku-4-5-20251001", "2026-10-15", true), Model("claude-sonnet-4-5-20250929", "2026-11-30", false), Model("claude-opus-4-1-20250805", "2026-08-05", false))
    println("retirement calendar on 2026-10-04:")
    for (line in retirementStatus(models, "2026-10-04")) println("  $line")
    val old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, null, "budget", "tool", false, true)
    val (next, changes) = migrateRequest(old)
    println("request for ${next.model}: ${changes.size} changes")
    for (change in changes) println("  $change")
    val cases = suite()
    val first = gate(cases, setOf("refund"), 25, 2000)
    println("gate on ${cases.size} cases: ${first.decision}")
    for (reason in first.reasons) println("  $reason")
    val fixed = cases.map { if (it.id == "r4") it.copy(newOk = true) else it }
    val second = gate(fixed, setOf("refund"), 40, 2000)
    println("gate after the refund fix, cost limit 40%: ${second.decision}, ${second.reasons.size} reasons")
    for ((stage, requests, errors) in listOf(Triple(1, 2000, 6), Triple(5, 300, 0), Triple(5, 10000, 20), Triple(25, 50000, 400), Triple(100, 50000, 10))) {
        println("roll-out at $stage% with $requests requests and $errors errors: ${rolloutStep(stage, requests, errors, 1000, 5)}")
    }
}
