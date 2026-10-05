private val log = System.getLogger("run_audit")

data class Run(val id: String, val kind: String, val status: String, val correct: Boolean, val invented: Boolean, val retriedAbsent: Boolean, val sumOk: Boolean)

data class Policy(val target: Int, val minN: Int, val gap: Int)

data class Segment(val kind: String, val n: Int, val correct: Int, val percent: Int, val automate: Boolean)

data class Report(
    val n: Int, val valid: Int, val needsReview: Int, val failed: Int, val accuracyAll: Int, val accuracyValidated: Int, val meetsTarget: Boolean,
    val segments: List<Segment>, val invented: Int, val wastedRetries: Int, val uncheckedTotals: Int, val overstated: Boolean, val firstFix: String,
)

/**
 * TODO 1 of 6 (unlocks m1, e1 and e8): a whole percentage, rounded half up.
 * Receives the correct count and the total. Returns `(200 * correct + total) / (2 * total)` in integer arithmetic, and 0 when the total is 0.
 * Example: percent(2, 3) -> 67, percent(1, 3) -> 33, percent(0, 0) -> 0
 */
fun percent(correct: Int, total: Int): Int = 0

/**
 * TODO 2 of 6 (unlocks m1, e3 and e4): one entry per kind of document.
 * Receives the runs and the policy. Returns a list sorted by kind of Segment(kind, n, correct, percent, automate); `automate` needs at
 * least `minN` documents of the kind and `correct * 100 >= target * n` for it.
 * Example: 9 of 10 typed documents with minN 5 and target 90 -> [Segment("typed", 10, 9, 90, true)]
 */
fun segmentsOf(runs: List<Run>, policy: Policy): List<Segment> = listOf(Segment("", 0, 0, 0, false))

/**
 * TODO 3 of 6 (unlocks m1 and e6): count the failure shapes in documents.
 * Receives the runs. Returns Triple(invented, wastedRetries, uncheckedTotals): documents flagged `invented`, documents flagged
 * `retriedAbsent`, and `valid` documents whose `sumOk` is false (a document never accepted is not counted).
 * Example: one valid document with sumOk false and one failed document with sumOk false -> Triple(0, 0, 1)
 */
fun failureShapes(runs: List<Run>): Triple<Int, Int, Int> = Triple(0, 0, 0)

/**
 * TODO 4 of 6 (unlocks m1, e1 and e2): does the run meet the target?
 * Receives the correct count, the document count and the target in percent. Returns true when there is at least one document
 * and `right * 100 >= target * n`. Example: meets(9, 10, 90) -> true, meets(8, 10, 90) -> false, meets(0, 0, 90) -> false
 */
fun meets(right: Int, n: Int, target: Int): Boolean = false

/**
 * TODO 5 of 6 (unlocks m1 and e5): is the figure overstated?
 * Receives both accuracies and the gap in points. Returns true only when the validated accuracy exceeds the all-document
 * accuracy by more than the gap. Example: isOverstated(100, 95, 5) -> false, isOverstated(100, 94, 5) -> true
 */
fun isOverstated(accuracyValidated: Int, accuracyAll: Int, gap: Int): Boolean = false

/**
 * TODO 6 of 6 (unlocks m1, e1 and e7): the first fix that applies.
 * Receives the document count, the three shape counts, `overstated` and `meetsTarget`. Returns `none` for an empty run, else the
 * first that applies of make_fields_nullable (invented), stop_retrying_absent (wasted), add_semantic_checks (unchecked),
 * measure_all_documents (overstated), improve_weak_segments (target not met), and `none`.
 * Example: chooseFix(3, 0, 1, 1, false, true) -> "stop_retrying_absent"
 */
fun chooseFix(n: Int, invented: Int, wasted: Int, unchecked: Int, overstated: Boolean, meetsTarget: Boolean): String = ""

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
