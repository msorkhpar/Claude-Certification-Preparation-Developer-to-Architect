import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EvalReportTest {
    @Test
    void theTwoVersionsHaveTheSameOverallAccuracyAndDifferentSegments() {
        List<EvalReport.Row> rows = EvalReport.buildCases();
        assertEquals(52, rows.size());
        List<EvalReport.Line> oldT = EvalReport.segmentTable(rows, "old");
        List<EvalReport.Line> newT = EvalReport.segmentTable(rows, "new");
        assertEquals(48, oldT.stream().mapToInt(EvalReport.Line::right).sum());
        assertEquals(48, newT.stream().mapToInt(EvalReport.Line::right).sum());
        assertEquals(new EvalReport.Line("refund", 8, 5, 63, 60), oldT.get(0));
        assertEquals(new EvalReport.Line("refund", 8, 7, 88, 20), newT.get(0));
    }

    @Test
    void percentileUsesTheNearestRankAndNeedsNoSortedInput() {
        List<Integer> values = List.of(4800, 800, 1000, 900);
        assertEquals(900, EvalReport.percentile(values, 50));
        assertEquals(4800, EvalReport.percentile(values, 95));
        assertEquals(0, EvalReport.percentile(List.of(), 95));
    }

    @Test
    void abVerdictNamesTheBetterSideOnlyWhenItClearsTheBar() {
        assertEquals("new is better", EvalReport.abVerdict(410, 500, 438, 500));
        assertEquals("old is better", EvalReport.abVerdict(438, 500, 410, 500));
        assertEquals("no clear difference", EvalReport.abVerdict(410, 500, 425, 500));
        assertEquals("too few cases", EvalReport.abVerdict(82, 100, 90, 100));
        assertEquals("no clear difference", EvalReport.abVerdict(0, 300, 0, 300));
    }

    @Test
    void theShadowGateHoldsForAProtectedRegressionOrANetLoss() {
        assertEquals(new EvalReport.Gate("hold", 2, 2, List.of("complaint")), EvalReport.shadowGate(EvalReport.buildCases(), Set.of("refund", "complaint")));
        assertEquals("ship", EvalReport.shadowGate(EvalReport.buildCases(), Set.of("refund")).decision());
    }

    @Test
    void diagnoseChecksTheEvidenceBeforeThePromptAndTheModelLast() {
        assertEquals("retrieval or data", EvalReport.diagnose(false, false, false, false));
        assertEquals("format instructions", EvalReport.diagnose(true, true, false, true));
        assertEquals("model mismatch", EvalReport.diagnose(true, true, true, true));
    }

    @Test
    void chooseModelTakesTheCheapestThatMeetsBothLimits() {
        List<EvalReport.Option> options = List.of(new EvalReport.Option("small", 84, 900, 1), new EvalReport.Option("medium", 91, 1800, 3), new EvalReport.Option("large", 95, 4200, 9));
        assertEquals("medium", EvalReport.chooseModel(options, 90, 2000));
        assertEquals("none", EvalReport.chooseModel(options, 94, 2000));
    }
}
