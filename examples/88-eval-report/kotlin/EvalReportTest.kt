import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EvalReportTest {
    @Test
    fun theTwoVersionsHaveTheSameOverallAccuracyAndDifferentSegments() {
        val rows = buildCases()
        assertEquals(52, rows.size)
        val oldT = segmentTable(rows, "old")
        val newT = segmentTable(rows, "new")
        assertEquals(48, oldT.map { it.right }.sum())
        assertEquals(48, newT.map { it.right }.sum())
        assertEquals(Line("refund", 8, 5, 63, 60), oldT[0])
        assertEquals(Line("refund", 8, 7, 88, 20), newT[0])
    }

    @Test
    fun percentileUsesTheNearestRankAndNeedsNoSortedInput() {
        val values = listOf(4800, 800, 1000, 900)
        assertEquals(900, percentile(values, 50))
        assertEquals(4800, percentile(values, 95))
        assertEquals(0, percentile(listOf(), 95))
    }

    @Test
    fun abVerdictNamesTheBetterSideOnlyWhenItClearsTheBar() {
        assertEquals("new is better", abVerdict(410, 500, 438, 500))
        assertEquals("old is better", abVerdict(438, 500, 410, 500))
        assertEquals("no clear difference", abVerdict(410, 500, 425, 500))
        assertEquals("too few cases", abVerdict(82, 100, 90, 100))
        assertEquals("no clear difference", abVerdict(0, 300, 0, 300))
    }

    @Test
    fun theShadowGateHoldsForAProtectedRegressionOrANetLoss() {
        assertEquals(Gate("hold", 2, 2, listOf("complaint")), shadowGate(buildCases(), setOf("refund", "complaint")))
        assertEquals("ship", shadowGate(buildCases(), setOf("refund")).decision)
    }

    @Test
    fun diagnoseChecksTheEvidenceBeforeThePromptAndTheModelLast() {
        assertEquals("retrieval or data", diagnose(false, false, false, false))
        assertEquals("format instructions", diagnose(true, true, false, true))
        assertEquals("model mismatch", diagnose(true, true, true, true))
    }

    @Test
    fun chooseModelTakesTheCheapestThatMeetsBothLimits() {
        val options = listOf(Option("small", 84, 900, 1), Option("medium", 91, 1800, 3), Option("large", 95, 4200, 9))
        assertEquals("medium", chooseModel(options, 90, 2000))
        assertEquals("none", chooseModel(options, 94, 2000))
    }
}
