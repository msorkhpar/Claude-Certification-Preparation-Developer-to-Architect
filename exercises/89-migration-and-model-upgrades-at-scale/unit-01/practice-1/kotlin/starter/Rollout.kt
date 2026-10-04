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

fun retirementStatus(models: List<Model>, today: String): List<String>? {
    // TODO: "<name>: <days> days, <level>" per model, the nearest retirement first, with " (tentative)" when the date may move.
    return null
}

fun migrateRequest(request: Request): Migration? {
    // TODO: the new request and the list of changes: drop what the target refuses, replace what it changes, and name each change.
    return null
}

fun gate(cases: List<Case>, protectedSegments: Set<String>, maxCostUp: Int, maxP95: Int): Verdict? {
    // TODO: the decision (go or no-go) and the reasons: must-pass, protected segments, net loss, cost and tail, in that order.
    return null
}

fun rolloutStep(stage: Int, requests: Int, errors: Int, minRequests: Int, maxErrorsPer1000: Int): String? {
    // TODO: "hold at S", "rollback to 0", "complete" or "advance to N" for one observed stage.
    return null
}
