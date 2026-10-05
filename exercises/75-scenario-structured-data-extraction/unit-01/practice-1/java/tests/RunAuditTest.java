import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunAuditTest {
    private static final RunAudit.Policy POLICY = new RunAudit.Policy(90, 3, 5);

    private static RunAudit.Run run(String kind, String status, boolean correct, boolean invented, boolean wasted, boolean sumOk) {
        return new RunAudit.Run("d", kind, status, correct, invented, wasted, sumOk);
    }

    private static RunAudit.Run run(String kind) {
        return run(kind, "valid", true, false, false, true);
    }

    private static RunAudit.Run wrong(String kind, String status) {
        return run(kind, status, false, false, false, true);
    }

    private static List<RunAudit.Run> many(int n, RunAudit.Run r) {
        List<RunAudit.Run> all = new ArrayList<>();
        for (int i = 0; i < n; i++) all.add(r);
        return all;
    }

    @SafeVarargs
    private static List<RunAudit.Run> join(List<RunAudit.Run>... parts) {
        List<RunAudit.Run> all = new ArrayList<>();
        for (List<RunAudit.Run> p : parts) all.addAll(p);
        return all;
    }

    private static RunAudit.Segment seg(String kind, int n, int correct, int percent, boolean automate) {
        return new RunAudit.Segment(kind, n, correct, percent, automate);
    }

    @Test
    void m1_aMixedRunGetsEveryCountBothAccuraciesTheSegmentsAndTheFirstFix() {
        var runs = join(many(3, run("typed")), List.of(run("scanned"), wrong("scanned", "needs_review"),
            run("handwritten", "failed", false, true, false, true), run("handwritten", "needs_review", false, false, true, true)));
        assertEquals(new RunAudit.Report(7, 4, 2, 1, 57, 100, false,
            List.of(seg("handwritten", 2, 0, 0, false), seg("scanned", 2, 1, 50, false), seg("typed", 3, 3, 100, true)), 1, 1, 0, true, "make_fields_nullable"),
            RunAudit.audit(runs, POLICY));
    }

    @Test
    void e1_anEmptyRunHasZeroFiguresNoSegmentsAndNeverMeetsTheTarget() {
        assertEquals(new RunAudit.Report(0, 0, 0, 0, 0, 0, false, List.of(), 0, 0, 0, false, "none"), RunAudit.audit(List.of(), POLICY));
    }

    @Test
    void e2_theRunMeetsTheTargetAtExactlyTheTargetAndNotBelowIt() {
        var at = join(many(9, run("typed")), many(1, wrong("typed", "valid")));
        var below = join(many(8, run("typed")), many(2, wrong("typed", "valid")));
        assertTrue(RunAudit.audit(at, POLICY).meetsTarget());
        assertEquals(90, RunAudit.audit(at, POLICY).accuracyAll());
        assertFalse(RunAudit.audit(below, POLICY).meetsTarget());
        assertEquals(80, RunAudit.audit(below, POLICY).accuracyAll());
    }

    @Test
    void e3_aKindNeedsAtLeastTheMinimumNumberOfDocumentsToBeAutomated() {
        var report = RunAudit.audit(join(many(3, run("typed")), many(2, run("scanned"))), POLICY);
        assertEquals(List.of(seg("scanned", 2, 2, 100, false), seg("typed", 3, 3, 100, true)), report.segments());
    }

    @Test
    void e4_aKindIsAutomatedAtExactlyTheTargetAccuracyAndNotBelowIt() {
        var report = RunAudit.audit(join(many(9, run("typed")), many(1, wrong("typed", "valid")), many(17, run("scanned")), many(2, wrong("scanned", "valid"))), POLICY);
        assertEquals(List.of(seg("scanned", 19, 17, 89, false), seg("typed", 10, 9, 90, true)), report.segments());
    }

    @Test
    void e5_theFigureIsOverstatedOnlyWhenTheValidatedAccuracyExceedsTheAllDocumentAccuracyByMoreThanTheGap() {
        var atGap = RunAudit.audit(join(many(19, run("typed")), many(1, wrong("typed", "failed"))), POLICY);
        assertEquals(95, atGap.accuracyAll());
        assertEquals(100, atGap.accuracyValidated());
        assertFalse(atGap.overstated());
        assertEquals("none", atGap.firstFix());
        var over = RunAudit.audit(join(many(16, run("typed")), many(1, wrong("typed", "failed"))), POLICY);
        assertEquals(94, over.accuracyAll());
        assertEquals(100, over.accuracyValidated());
        assertTrue(over.overstated());
        assertEquals("measure_all_documents", over.firstFix());
    }

    @Test
    void e6_eachShapeCountsDocumentsOnceAndAnUncheckedTotalCountsOnlyDocumentsAcceptedAsValid() {
        var runs = List.of(run("typed", "valid", true, false, false, false), run("typed", "needs_review", false, false, false, false),
            run("typed", "failed", false, false, false, false), run("typed", "valid", true, true, true, true));
        var report = RunAudit.audit(runs, POLICY);
        assertEquals(1, report.invented());
        assertEquals(1, report.wastedRetries());
        assertEquals(1, report.uncheckedTotals());
    }

    @Test
    void e7_theFirstFixFollowsTheOrderOfWhatCostsMost() {
        assertEquals("make_fields_nullable", RunAudit.audit(List.of(run("typed", "valid", true, true, true, false)), POLICY).firstFix());
        assertEquals("stop_retrying_absent", RunAudit.audit(List.of(run("typed", "valid", true, false, true, false)), POLICY).firstFix());
        assertEquals("add_semantic_checks", RunAudit.audit(List.of(run("typed", "valid", true, false, false, false)), POLICY).firstFix());
        assertEquals("measure_all_documents", RunAudit.audit(join(many(16, run("typed")), many(1, wrong("typed", "failed"))), POLICY).firstFix());
        assertEquals("improve_weak_segments", RunAudit.audit(join(many(8, run("typed")), many(2, wrong("typed", "valid"))), POLICY).firstFix());
        assertEquals("none", RunAudit.audit(many(5, run("typed")), POLICY).firstFix());
    }

    @Test
    void e8_percentagesAreWholeNumbersRoundedHalfUp() {
        var report = RunAudit.audit(join(many(1, run("typed")), many(7, wrong("typed", "valid"))), POLICY);
        assertEquals(13, report.accuracyAll());
        assertEquals(13, report.accuracyValidated());
        assertEquals(13, report.segments().get(0).percent());
        assertEquals(67, RunAudit.audit(join(many(2, run("typed")), many(1, wrong("typed", "valid"))), POLICY).accuracyAll());
        assertEquals(33, RunAudit.audit(join(many(1, run("typed")), many(2, wrong("typed", "valid"))), POLICY).accuracyAll());
    }
}
