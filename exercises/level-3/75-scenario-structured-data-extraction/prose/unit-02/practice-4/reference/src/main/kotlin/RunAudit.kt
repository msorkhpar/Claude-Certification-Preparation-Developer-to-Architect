private val log = System.getLogger("run_audit")

data class Run(val id: String, val kind: String, val status: String, val correct: Boolean, val invented: Boolean, val retriedAbsent: Boolean, val sumOk: Boolean)

data class Policy(val target: Int, val minN: Int, val gap: Int)

data class Segment(val kind: String, val n: Int, val correct: Int, val percent: Int, val automate: Boolean)

data class Report(
    val n: Int, val valid: Int, val needsReview: Int, val failed: Int, val accuracyAll: Int, val accuracyValidated: Int, val meetsTarget: Boolean,
    val segments: List<Segment>, val invented: Int, val wastedRetries: Int, val uncheckedTotals: Int, val overstated: Boolean, val firstFix: String,
)

/** A whole percentage, rounded half up; 0 when there is nothing to divide. */
fun percent(correct: Int, total: Int): Int = if (total > 0) (200 * correct + total) / (2 * total) else 0

/** One entry per kind, sorted by kind. */
fun segmentsOf(runs: List<Run>, policy: Policy): List<Segment> = runs.map { it.kind }.toSortedSet().map { kind ->
    val group = runs.filter { it.kind == kind }
    val ok = group.count { it.correct }
    Segment(kind, group.size, ok, percent(ok, group.size), group.size >= policy.minN && ok * 100 >= policy.target * group.size)
}

/** Triple(invented, wastedRetries, uncheckedTotals) counted in documents. */
fun failureShapes(runs: List<Run>): Triple<Int, Int, Int> =
    Triple(runs.count { it.invented }, runs.count { it.retriedAbsent }, runs.count { it.status == "valid" && !it.sumOk })

/** True when the run has a document and right * 100 >= target * n. */
fun meets(right: Int, n: Int, target: Int): Boolean = n > 0 && right * 100 >= target * n

/** True when the validated accuracy exceeds the all-document accuracy by more than the gap. */
fun isOverstated(accuracyValidated: Int, accuracyAll: Int, gap: Int): Boolean = accuracyValidated - accuracyAll > gap

/** The first fix that applies, in the order of the statement. */
fun chooseFix(n: Int, invented: Int, wasted: Int, unchecked: Int, overstated: Boolean, meetsTarget: Boolean): String = when {
    n == 0 -> "none"
    invented > 0 -> "make_fields_nullable"
    wasted > 0 -> "stop_retrying_absent"
    unchecked > 0 -> "add_semantic_checks"
    overstated -> "measure_all_documents"
    !meetsTarget -> "improve_weak_segments"
    else -> "none"
}

/** Audit an extraction run: the accuracy on every document, the accuracy by kind of document, the failure shapes and the first fix. */
fun audit(runs: List<Run>, policy: Policy): Report {
    log.log(System.Logger.Level.DEBUG, "audit input: {0}", runs)
    val n = runs.size
    val valid = runs.count { it.status == "valid" }
    val right = runs.count { it.correct }
    val rightValid = runs.count { it.status == "valid" && it.correct }
    val accuracyAll = percent(right, n)
    val accuracyValidated = percent(rightValid, valid)
    val (invented, wasted, unchecked) = failureShapes(runs)
    val overstated = isOverstated(accuracyValidated, accuracyAll, policy.gap)
    val meetsTarget = meets(right, n, policy.target)
    return Report(n, valid, runs.count { it.status == "needs_review" }, runs.count { it.status == "failed" }, accuracyAll, accuracyValidated, meetsTarget, segmentsOf(runs, policy),
        invented, wasted, unchecked, overstated, chooseFix(n, invented, wasted, unchecked, overstated, meetsTarget))
}
