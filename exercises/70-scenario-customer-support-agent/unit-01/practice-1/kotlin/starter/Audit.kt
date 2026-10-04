/** One call the agent made: the tool, whether it succeeded and the tool it should have used (null when not known). */
data class Step(val tool: String, val ok: Boolean = true, val rightTool: String? = null)

data class Session(val id: String, val steps: List<Step>, val outcome: String, val needsHuman: Boolean, val refundCents: Int, val limitCents: Int)

data class Report(
    val sessions: Int, val resolved: Int, val fcr: Double, val meetsTarget: Boolean, val overEscalated: Int, val underEscalated: Int,
    val skippedPrerequisite: Int, val wrongTool: Int, val overLimitRefunds: Int, val diagnosis: String,
)

/**
 * Audit a support agent's recorded sessions: the rates, the failure shapes and the first fix.
 * Read statement.md for the fields of a session and of the report, then replace the body of audit().
 */
fun audit(sessions: List<Session>): Report = Report(0, 0, 0.0, false, 0, 0, 0, 0, 0, "")
