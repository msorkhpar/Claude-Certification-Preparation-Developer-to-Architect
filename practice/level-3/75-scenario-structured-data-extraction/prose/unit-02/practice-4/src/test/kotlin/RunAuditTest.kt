import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RunAuditTest {
    private val policy = Policy(90, 3, 5)

    private fun run(kind: String = "typed", status: String = "valid", correct: Boolean = true, invented: Boolean = false, wasted: Boolean = false, sumOk: Boolean = true) =
        Run("d", kind, status, correct, invented, wasted, sumOk)

    private fun many(n: Int, r: Run) = List(n) { r }

    private fun seg(kind: String, n: Int, correct: Int, percent: Int, automate: Boolean) = Segment(kind, n, correct, percent, automate)

    @Test
    fun m1_aMixedRunGetsEveryCountBothAccuraciesTheSegmentsAndTheFirstFix() {
        val runs = many(3, run()) + listOf(run("scanned"), run("scanned", "needs_review", false), run("handwritten", "failed", false, invented = true),
            run("handwritten", "needs_review", false, wasted = true))
        assertEquals(Report(7, 4, 2, 1, 57, 100, false, listOf(seg("handwritten", 2, 0, 0, false), seg("scanned", 2, 1, 50, false), seg("typed", 3, 3, 100, true)),
            1, 1, 0, true, "make_fields_nullable"), audit(runs, policy))
    }

    @Test
    fun e1_anEmptyRunHasZeroFiguresNoSegmentsAndNeverMeetsTheTarget() {
        assertEquals(Report(0, 0, 0, 0, 0, 0, false, listOf(), 0, 0, 0, false, "none"), audit(listOf(), policy))
    }

    @Test
    fun e2_theRunMeetsTheTargetAtExactlyTheTargetAndNotBelowIt() {
        val at = many(9, run()) + many(1, run(correct = false))
        val below = many(8, run()) + many(2, run(correct = false))
        assertTrue(audit(at, policy).meetsTarget)
        assertEquals(90, audit(at, policy).accuracyAll)
        assertFalse(audit(below, policy).meetsTarget)
        assertEquals(80, audit(below, policy).accuracyAll)
    }

    @Test
    fun e3_aKindNeedsAtLeastTheMinimumNumberOfDocumentsToBeAutomated() {
        val report = audit(many(3, run("typed")) + many(2, run("scanned")), policy)
        assertEquals(listOf(seg("scanned", 2, 2, 100, false), seg("typed", 3, 3, 100, true)), report.segments)
    }

    @Test
    fun e4_aKindIsAutomatedAtExactlyTheTargetAccuracyAndNotBelowIt() {
        val report = audit(many(9, run("typed")) + many(1, run("typed", correct = false)) + many(17, run("scanned")) + many(2, run("scanned", correct = false)), policy)
        assertEquals(listOf(seg("scanned", 19, 17, 89, false), seg("typed", 10, 9, 90, true)), report.segments)
    }

    @Test
    fun e5_theFigureIsOverstatedOnlyWhenTheValidatedAccuracyExceedsTheAllDocumentAccuracyByMoreThanTheGap() {
        val atGap = audit(many(19, run()) + many(1, run(status = "failed", correct = false)), policy)
        assertEquals(listOf(95, 100, false, "none"), listOf(atGap.accuracyAll, atGap.accuracyValidated, atGap.overstated, atGap.firstFix))
        val over = audit(many(16, run()) + many(1, run(status = "failed", correct = false)), policy)
        assertEquals(listOf(94, 100, true, "measure_all_documents"), listOf(over.accuracyAll, over.accuracyValidated, over.overstated, over.firstFix))
    }

    @Test
    fun e6_eachShapeCountsDocumentsOnceAndAnUncheckedTotalCountsOnlyDocumentsAcceptedAsValid() {
        val runs = listOf(run(sumOk = false), run(status = "needs_review", correct = false, sumOk = false), run(status = "failed", correct = false, sumOk = false),
            run(invented = true, wasted = true))
        val report = audit(runs, policy)
        assertEquals(listOf(1, 1, 1), listOf(report.invented, report.wastedRetries, report.uncheckedTotals))
    }

    @Test
    fun e7_theFirstFixFollowsTheOrderOfWhatCostsMost() {
        assertEquals("make_fields_nullable", audit(listOf(run(invented = true, wasted = true, sumOk = false)), policy).firstFix)
        assertEquals("stop_retrying_absent", audit(listOf(run(wasted = true, sumOk = false)), policy).firstFix)
        assertEquals("add_semantic_checks", audit(listOf(run(sumOk = false)), policy).firstFix)
        assertEquals("measure_all_documents", audit(many(16, run()) + many(1, run(status = "failed", correct = false)), policy).firstFix)
        assertEquals("improve_weak_segments", audit(many(8, run()) + many(2, run(correct = false)), policy).firstFix)
        assertEquals("none", audit(many(5, run()), policy).firstFix)
    }

    @Test
    fun e8_percentagesAreWholeNumbersRoundedHalfUp() {
        val report = audit(many(1, run()) + many(7, run(correct = false)), policy)
        assertEquals(listOf(13, 13, 13), listOf(report.accuracyAll, report.accuracyValidated, report.segments[0].percent))
        assertEquals(67, audit(many(2, run()) + many(1, run(correct = false)), policy).accuracyAll)
        assertEquals(33, audit(many(1, run()) + many(2, run(correct = false)), policy).accuracyAll)
    }
}
