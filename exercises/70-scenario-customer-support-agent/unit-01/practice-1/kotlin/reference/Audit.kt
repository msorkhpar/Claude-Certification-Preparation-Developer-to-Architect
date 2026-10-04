/** One call the agent made: the tool, whether it succeeded and the tool it should have used (null when not known). */
data class Step(val tool: String, val ok: Boolean = true, val rightTool: String? = null)

data class Session(val id: String, val steps: List<Step>, val outcome: String, val needsHuman: Boolean, val refundCents: Int, val limitCents: Int)

data class Report(
    val sessions: Int, val resolved: Int, val fcr: Double, val meetsTarget: Boolean, val overEscalated: Int, val underEscalated: Int,
    val skippedPrerequisite: Int, val wrongTool: Int, val overLimitRefunds: Int, val diagnosis: String,
)

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

/** Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix. */
fun audit(sessions: List<Session>): Report {
    val n = sessions.size
    val resolved = sessions.count { it.outcome == "resolved" }
    val over = sessions.count { it.outcome == "escalated" && !it.needsHuman }
    val under = sessions.count { it.outcome == "resolved" && it.needsHuman }
    val skipped = sessions.count { skippedPrerequisite(it.steps) }
    val wrong = sessions.count { s -> s.steps.any { it.rightTool != null && it.tool != it.rightTool } }
    val overLimit = sessions.count { it.outcome == "resolved" && it.refundCents > it.limitCents }
    val fcr = if (n > 0) Math.round(resolved.toDouble() / n * 1000) / 1000.0 else 0.0
    val diagnosis = when {
        skipped > 0 || overLimit > 0 -> "enforce_in_code"
        wrong > 0 && wrong >= over + under -> "rewrite_tool_descriptions"
        over + under > 0 -> "write_escalation_criteria"
        else -> "none"
    }
    return Report(n, resolved, fcr, fcr >= TARGET, over, under, skipped, wrong, overLimit, diagnosis)
}
