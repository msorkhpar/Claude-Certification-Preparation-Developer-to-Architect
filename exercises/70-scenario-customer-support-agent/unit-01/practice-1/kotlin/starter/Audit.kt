/** One call the agent made: the tool, whether it succeeded and the tool it should have used (null when not known). */
data class Step(val tool: String, val ok: Boolean = true, val rightTool: String? = null)

data class Session(val id: String, val steps: List<Step>, val outcome: String, val needsHuman: Boolean, val refundCents: Int, val limitCents: Int)

data class Report(
    val sessions: Int, val resolved: Int, val fcr: Double, val meetsTarget: Boolean, val overEscalated: Int, val underEscalated: Int,
    val skippedPrerequisite: Int, val wrongTool: Int, val overLimitRefunds: Int, val diagnosis: String,
)

private val log = System.getLogger("audit")

private val PROTECTED = listOf("lookup_order", "process_refund")
private const val TARGET = 0.8

/**
 * TODO 1 of 5 (unlocks e2): did an order or refund call come before the first successful get_customer call?
 * Receives a session's steps. Returns true or false; a failed get_customer does not identify anyone.
 * Example: [lookup_order, get_customer] -> true, [get_customer, lookup_order] -> false
 */
fun skippedPrerequisite(steps: List<Step>): Boolean = false

/**
 * TODO 2 of 5 (unlocks e6): does a step have a known right tool that differs from the tool used?
 * Receives a session's steps (rightTool is null when not known). Returns true for at least one such step, false otherwise.
 * Example: [Step("get_customer", true, "lookup_order")] -> true; a step with a null rightTool never counts.
 */
fun hasWrongTool(steps: List<Step>): Boolean = false

/**
 * TODO 3 of 5 (unlocks e3): was a refund above the limit actually made?
 * Receives one session. Returns true only when its outcome is "resolved" and refundCents is above limitCents.
 * Example: resolved, refund 10001, limit 10000 -> true; escalated with the same refund -> false
 */
fun isOverLimit(session: Session): Boolean = false

/**
 * TODO 4 of 5 (unlocks e1 and e5): the first contact rate.
 * Receives the resolved count and the session count. Returns resolved / n rounded to three decimals, 0.0 when n is 0.
 * Example: firstContactRate(2, 3) -> 0.667
 */
fun firstContactRate(resolved: Int, n: Int): Double = 0.0

/**
 * TODO 5 of 5 (unlocks e4 and e1): the first fix, by the order in the statement.
 * Receives the counts of skipped prerequisites, over-limit refunds, wrong-tool sessions, over- and under-escalations.
 * Returns "enforce_in_code", "rewrite_tool_descriptions", "write_escalation_criteria" or "none".
 * Example: diagnose(0, 0, 2, 1, 0) -> "rewrite_tool_descriptions"
 */
fun diagnose(skipped: Int, overLimit: Int, wrong: Int, over: Int, under: Int): String = ""

/** Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix. */
fun audit(sessions: List<Session>): Report {
    log.log(System.Logger.Level.DEBUG, "audit input: {0}", sessions)
    val n = sessions.size
    val resolved = sessions.count { it.outcome == "resolved" }
    val over = sessions.count { it.outcome == "escalated" && !it.needsHuman }
    val under = sessions.count { it.outcome == "resolved" && it.needsHuman }
    val skipped = sessions.count { skippedPrerequisite(it.steps) }
    val wrong = sessions.count { hasWrongTool(it.steps) }
    val overLimit = sessions.count { isOverLimit(it) }
    val fcr = firstContactRate(resolved, n)
    return Report(n, resolved, fcr, fcr >= TARGET, over, under, skipped, wrong, overLimit, diagnose(skipped, overLimit, wrong, over, under))
}
