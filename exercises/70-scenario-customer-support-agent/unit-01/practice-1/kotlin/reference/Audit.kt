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

/** True when an order or refund call came before the first successful get_customer call. */
fun skippedPrerequisite(steps: List<Step>): Boolean {
    var verified = false
    for (step in steps) {
        if (step.tool == "get_customer" && step.ok) verified = true
        else if (step.tool in PROTECTED && !verified) return true
    }
    return false
}

/** True when at least one step has a known right tool that differs from the tool used. */
fun hasWrongTool(steps: List<Step>): Boolean = steps.any { it.rightTool != null && it.tool != it.rightTool }

/** True when the session was resolved with a refund above its limit. */
fun isOverLimit(session: Session): Boolean = session.outcome == "resolved" && session.refundCents > session.limitCents

/** The resolved share, rounded to three decimals; 0.0 for no sessions. */
fun firstContactRate(resolved: Int, n: Int): Double = if (n > 0) Math.round(resolved.toDouble() / n * 1000) / 1000.0 else 0.0

/** The first fix, by the order of the statement. */
fun diagnose(skipped: Int, overLimit: Int, wrong: Int, over: Int, under: Int): String = when {
    skipped > 0 || overLimit > 0 -> "enforce_in_code"
    wrong > 0 && wrong >= over + under -> "rewrite_tool_descriptions"
    over + under > 0 -> "write_escalation_criteria"
    else -> "none"
}

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
