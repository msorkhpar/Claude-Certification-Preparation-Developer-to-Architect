import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = System.getLogger("rollout")

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

/** Whole days from one ISO date to another, negative when it has passed (written for you). */
fun daysUntil(today: String, date: String): Long = ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(date))

/** Nearest-rank percentile, 0 for no values (written for you). */
fun percentile(values: List<Int>, p: Int): Int {
    if (values.isEmpty()) return 0
    val ordered = values.sorted()
    return ordered[(p * ordered.size + 99) / 100 - 1]
}

private fun statusLine(days: Long, name: String, tentative: Boolean): String {
    val level = if (days < 0) "retired" else if (days <= 14) "urgent" else if (days <= 60) "migrate now" else "watch"
    return "$name: $days days, $level${if (tentative) " (tentative)" else ""}"
}

fun retirementStatus(models: List<Model>, today: String): List<String> {
    log.log(System.Logger.Level.DEBUG, "retirementStatus input: {0}", models)
    return models.sortedWith(compareBy<Model>({ daysUntil(today, it.date) }, { it.name }, { it.tentative })).map { statusLine(daysUntil(today, it.date), it.name, it.tentative) }
}

private fun migrateThinking(thinking: String, changes: MutableList<String>): String {
    if (thinking == "budget") {
        changes.add("thinking budget replaced by adaptive thinking; sweep the effort")
        return "adaptive"
    }
    if (thinking == "disabled") {
        changes.add("thinking disabled replaced by between_tools")
        return "between_tools"
    }
    return thinking
}

fun migrateRequest(request: Request): Migration {
    val changes = mutableListOf<String>()
    if (request.model != TARGET) changes.add("model set to $TARGET")
    if (request.temperature != null) changes.add("removed temperature")
    if (request.topP != null) changes.add("removed top_p")
    if (request.topK != null) changes.add("removed top_k")
    val thinking = migrateThinking(request.thinking, changes)
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

private fun mustPass(cases: List<Case>): List<String> {
    val failed = cases.filter { it.mustPass && !it.newOk }.map { it.id }.sorted()
    return if (failed.isNotEmpty()) listOf("must-pass failed: " + failed.joinToString(", ")) else emptyList()
}

private fun protectedLost(cases: List<Case>, protectedSegments: Set<String>): List<String> {
    val hit = cases.filter { it.oldOk && !it.newOk && it.segment in protectedSegments }.map { it.segment }.toSortedSet().toList()
    return if (hit.isNotEmpty()) listOf("protected segment lost answers: " + hit.joinToString(", ")) else emptyList()
}

private fun netLoss(cases: List<Case>): List<String> {
    val lost = cases.count { it.oldOk && !it.newOk }
    val gained = cases.count { it.newOk && !it.oldOk }
    return if (lost > gained) listOf("net loss: lost $lost, gained $gained") else emptyList()
}

private fun costRise(cases: List<Case>, maxCostUp: Int): List<String> {
    val oldTotal = cases.sumOf { it.oldCost }
    val newTotal = cases.sumOf { it.newCost }
    val up = if (oldTotal > 0 && newTotal > oldTotal) (newTotal - oldTotal) * 100 / oldTotal else 0
    return if (up > maxCostUp) listOf("cost up $up% over the $maxCostUp% limit") else emptyList()
}

private fun latency(cases: List<Case>, maxP95: Int): List<String> {
    val p95 = percentile(cases.map { it.newMs }, 95)
    return if (p95 > maxP95) listOf("p95 latency $p95 ms over the $maxP95 ms limit") else emptyList()
}

private fun decision(reasons: List<String>): String = if (reasons.isEmpty()) "go" else "no-go"

fun gate(cases: List<Case>, protectedSegments: Set<String>, maxCostUp: Int, maxP95: Int): Verdict {
    val reasons = mustPass(cases) + protectedLost(cases, protectedSegments) + netLoss(cases) + costRise(cases, maxCostUp) + latency(cases, maxP95)
    return Verdict(decision(reasons), reasons)
}

fun rolloutStep(stage: Int, requests: Int, errors: Int, minRequests: Int, maxErrorsPer1000: Int): String {
    if (requests < minRequests) return "hold at $stage"
    if (errors * 1000L / requests > maxErrorsPer1000) return "rollback to 0"
    if (stage == STAGES.last()) return "complete"
    return "advance to ${STAGES[STAGES.indexOf(stage) + 1]}"
}
