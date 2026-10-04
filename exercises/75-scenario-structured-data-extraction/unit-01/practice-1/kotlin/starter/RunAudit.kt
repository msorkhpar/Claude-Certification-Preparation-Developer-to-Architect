// Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix.
// Read statement.md for the fields of a run, of the policy and of the report, then replace the body of audit().
data class Run(val id: String, val kind: String, val status: String, val correct: Boolean, val invented: Boolean, val retriedAbsent: Boolean, val sumOk: Boolean)

data class Policy(val target: Int, val minN: Int, val gap: Int)

data class Segment(val kind: String, val n: Int, val correct: Int, val percent: Int, val automate: Boolean)

data class Report(
    val n: Int, val valid: Int, val needsReview: Int, val failed: Int, val accuracyAll: Int, val accuracyValidated: Int, val meetsTarget: Boolean,
    val segments: List<Segment>, val invented: Int, val wastedRetries: Int, val uncheckedTotals: Int, val overstated: Boolean, val firstFix: String,
)

fun audit(runs: List<Run>, policy: Policy): Report = Report(0, 0, 0, 0, 0, 0, false, listOf(), 0, 0, 0, false, "")
