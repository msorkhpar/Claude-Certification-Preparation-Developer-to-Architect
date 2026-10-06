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

/**
 * TODO 1 of 9 (unlocks e7): one line of the retirement calendar.
 * Receives the days left, the model name and whether the date is tentative. Returns `<name>: <days> days, <level>` with ` (tentative)`
 * added when it is tentative; the level is `retired` below 0, `urgent` up to 14, `migrate now` up to 60, else `watch`.
 * Example: statusLine(14, "a", true) -> "a: 14 days, urgent (tentative)"
 */
private fun statusLine(days: Long, name: String, tentative: Boolean): String {
    return ""
}

fun retirementStatus(models: List<Model>, today: String): List<String> {
    log.log(System.Logger.Level.DEBUG, "retirementStatus input: {0}", models)
    return models.sortedWith(compareBy<Model>({ daysUntil(today, it.date) }, { it.name }, { it.tentative })).map { statusLine(daysUntil(today, it.date), it.name, it.tentative) }
}

/**
 * TODO 2 of 9 (unlocks e8): migrate the thinking setting.
 * Receives the thinking setting and the list of changes so far. `budget` becomes `adaptive` and `disabled` becomes `between_tools`, each
 * adding its sentence from the statement to `changes`; anything else is kept. Returns the new setting.
 * Example: migrateThinking("disabled", changes) -> "between_tools", and changes gains "thinking disabled replaced by between_tools"
 */
private fun migrateThinking(thinking: String, changes: MutableList<String>): String {
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

/**
 * TODO 3 of 9 (unlocks e1): the reasons for failed must-pass cases.
 * Receives the cases. Returns a list with `must-pass failed: <ids>` (sorted, joined by `, `) for the cases marked must pass that the new
 * model fails, or an empty list. Example: one failing must-pass case a1 -> ["must-pass failed: a1"]
 */
private fun mustPass(cases: List<Case>): List<String> {
    return emptyList()
}

/**
 * TODO 4 of 9 (unlocks e2): the reason for a protected segment that lost answers.
 * Receives the cases and the protected segments. Returns a list with `protected segment lost answers: <segments>` (sorted, distinct)
 * for the protected segments with a case the old model got right and the new one did not, or an empty list.
 * Example: refund lost b3 -> ["protected segment lost answers: refund"]
 */
private fun protectedLost(cases: List<Case>, protectedSegments: Set<String>): List<String> {
    return emptyList()
}

/**
 * TODO 5 of 9 (unlocks e3): the reason for more losses than gains.
 * Receives the cases. Returns a list with `net loss: lost N, gained M` when the cases lost (old right, new wrong) outnumber the cases
 * gained (the opposite), or an empty list. Example: 2 lost and 1 gained -> ["net loss: lost 2, gained 1"]
 */
private fun netLoss(cases: List<Case>): List<String> {
    return emptyList()
}

/**
 * TODO 6 of 9 (unlocks e4): the reason for a cost rise over the limit.
 * Receives the cases and the largest allowed rise in whole percent. The rise is the new total cost over the old one, rounded down, 0 when
 * the old total is 0 or the cost fell. Returns a list with `cost up X% over the Y% limit` when X is above Y, or an empty list.
 * Example: costs 40 -> 50 with a limit of 20 -> ["cost up 25% over the 20% limit"]; costs 40 -> 48 with a limit of 20 -> []
 */
private fun costRise(cases: List<Case>, maxCostUp: Int): List<String> {
    return emptyList()
}

/**
 * TODO 7 of 9 (unlocks e5): the reason for a slow tail.
 * Receives the cases and the largest allowed 95th-percentile time in ms. Uses `percentile` over the `newMs` of the cases. Returns a list
 * with `p95 latency X ms over the Y ms limit` when X is above Y, or an empty list. Example: p95 3000 with a limit of 2000 -> one reason
 */
private fun latency(cases: List<Case>, maxP95: Int): List<String> {
    return emptyList()
}

/**
 * TODO 8 of 9 (unlocks m1): the decision of the gate.
 * Receives the list of reasons. Returns "no-go" when there is any reason, "go" when there is none. Example: decision(emptyList()) -> "go"
 */
private fun decision(reasons: List<String>): String {
    return ""
}

fun gate(cases: List<Case>, protectedSegments: Set<String>, maxCostUp: Int, maxP95: Int): Verdict {
    val reasons = mustPass(cases) + protectedLost(cases, protectedSegments) + netLoss(cases) + costRise(cases, maxCostUp) + latency(cases, maxP95)
    return Verdict(decision(reasons), reasons)
}

/**
 * TODO 9 of 9 (unlocks e6): the step of a staged roll-out.
 * Receives the stage (1, 5, 25 or 100), the requests and errors seen, the fewest requests to judge and the most errors per 1000.
 * Returns "hold at <stage>" with too few requests, "rollback to 0" when errors per 1000 (rounded down) pass the limit, "complete" at
 * stage 100 when healthy, else "advance to <next stage>". Example: rolloutStep(1, 2000, 6, 1000, 5) -> "advance to 5"
 */
fun rolloutStep(stage: Int, requests: Int, errors: Int, minRequests: Int, maxErrorsPer1000: Int): String {
    return ""
}
