data class Run(val id: String, val kind: String, val status: String, val correct: Boolean, val invented: Boolean, val retriedAbsent: Boolean, val sumOk: Boolean)

data class Policy(val target: Int, val minN: Int, val gap: Int)

data class Segment(val kind: String, val n: Int, val correct: Int, val percent: Int, val automate: Boolean)

data class Report(
    val n: Int, val valid: Int, val needsReview: Int, val failed: Int, val accuracyAll: Int, val accuracyValidated: Int, val meetsTarget: Boolean,
    val segments: List<Segment>, val invented: Int, val wastedRetries: Int, val uncheckedTotals: Int, val overstated: Boolean, val firstFix: String,
)

/** A whole percentage, rounded half up; 0 when there is nothing to divide. */
fun percent(correct: Int, total: Int): Int = if (total > 0) (200 * correct + total) / (2 * total) else 0

/** Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix. */
fun audit(runs: List<Run>, policy: Policy): Report {
    val n = runs.size
    val valid = runs.count { it.status == "valid" }
    val right = runs.count { it.correct }
    val rightValid = runs.count { it.status == "valid" && it.correct }
    val accuracyAll = percent(right, n)
    val accuracyValidated = percent(rightValid, valid)
    val segments = runs.map { it.kind }.toSortedSet().map { kind ->
        val group = runs.filter { it.kind == kind }
        val ok = group.count { it.correct }
        Segment(kind, group.size, ok, percent(ok, group.size), group.size >= policy.minN && ok * 100 >= policy.target * group.size)
    }
    val invented = runs.count { it.invented }
    val wasted = runs.count { it.retriedAbsent }
    val unchecked = runs.count { it.status == "valid" && !it.sumOk }
    val overstated = accuracyValidated - accuracyAll > policy.gap
    val meetsTarget = n > 0 && right * 100 >= policy.target * n
    val firstFix = when {
        n == 0 -> "none"
        invented > 0 -> "make_fields_nullable"
        wasted > 0 -> "stop_retrying_absent"
        unchecked > 0 -> "add_semantic_checks"
        overstated -> "measure_all_documents"
        !meetsTarget -> "improve_weak_segments"
        else -> "none"
    }
    return Report(n, valid, runs.count { it.status == "needs_review" }, runs.count { it.status == "failed" }, accuracyAll, accuracyValidated, meetsTarget, segments,
        invented, wasted, unchecked, overstated, firstFix)
}
