import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Rollout kit: the retirement calendar, the settings a new model refuses, a gate on a regression suite and a staged roll-out. See ../../statement.md. */

/** One case of the regression suite, run on the old and the new model. */
data class Case(val id: String, val segment: String, val mustPass: Boolean, val oldOk: Boolean, val newOk: Boolean, val oldCost: Int, val newCost: Int, val newMs: Int)

/** The settings of a request that matter to a migration; null means the setting is absent. */
data class Request(val model: String, val temperature: Double?, val topP: Double?, val topK: Double?, val thinking: String, val toolChoice: String, val strict: Boolean, val prefill: Boolean)

/** A model, the date of its retirement (ISO) and whether that date may still move later. */
data class Model(val name: String, val date: String, val tentative: Boolean)

/** The migrated request and the changes made to it. */
data class Migration(val request: Request, val changes: List<String>)

/** The gate's decision (go or no-go) and its reasons. */
data class Verdict(val decision: String, val reasons: List<String>)

val STAGES = listOf(1, 5, 25, 100)
const val TARGET = "claude-sonnet-5-5"

fun daysUntil(today: String, date: String): Long = ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(date))

fun percentile(values: List<Int>, p: Int): Int {
    if (values.isEmpty()) return 0
    val ordered = values.sorted()
    return ordered[(p * ordered.size + 99) / 100 - 1]
}

fun retirementStatus(models: List<Model>, today: String): List<String>? =
    models.sortedWith(compareBy<Model>({ daysUntil(today, it.date) }, { it.name }, { it.tentative })).map {
        val days = daysUntil(today, it.date)
        val level = if (days < 0) "retired" else if (days <= 14) "urgent" else if (days <= 60) "migrate now" else "watch"
        "${it.name}: $days days, $level${if (it.tentative) " (tentative)" else ""}"
    }

fun migrateRequest(request: Request): Migration? {
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
    return Migration(Request(TARGET, null, null, null, thinking, toolChoice, strict, false), changes)
}

fun gate(cases: List<Case>, protectedSegments: Set<String>, maxCostUp: Int, maxP95: Int): Verdict? {
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

fun rolloutStep(stage: Int, requests: Int, errors: Int, minRequests: Int, maxErrorsPer1000: Int): String? {
    if (requests < minRequests) return "hold at $stage"
    if (errors * 1000L / requests > maxErrorsPer1000) return "rollback to 0"
    if (stage == STAGES.last()) return "complete"
    return "advance to ${STAGES[STAGES.indexOf(stage) + 1]}"
}
