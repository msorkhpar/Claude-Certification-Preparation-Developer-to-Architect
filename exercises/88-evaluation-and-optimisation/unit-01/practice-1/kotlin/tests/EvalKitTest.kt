import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EvalKitTest {
    private val costs = mapOf("order status" to 1, "refund" to 20, "policy" to 5, "complaint" to 10)

    private fun results(vararg groups: Triple<String, Int, Int>): List<Result> = groups.flatMap { (segment, total, right) -> (0 until total).map { Result(segment, it < right) } }

    private fun pairs(n: Int, segment: String, oldOk: Boolean, newOk: Boolean): List<Paired> = (0 until n).map { Paired(segment, oldOk, newOk) }

    private fun table(results: List<Result>, costs: Map<String, Int>): List<Line> {
        val result = segmentTable(results, costs)
        assertNotNull(result, "segmentTable returned nothing")
        return result ?: listOf()
    }

    private fun gate(pairs: List<Paired>, protectedSegments: Set<String>): Gate {
        val result = shadowGate(pairs, protectedSegments)
        assertNotNull(result, "shadowGate returned nothing")
        return result ?: Gate("", 0, 0, listOf())
    }

    @Test
    fun m1_aSegmentTableReportsAccuracyAndErrorCostWithTheCostliestSegmentFirst() {
        val rs = results(Triple("order status", 30, 30), Triple("refund", 8, 5), Triple("policy", 10, 9), Triple("complaint", 4, 4))
        assertEquals(listOf(Line("refund", 8, 5, 63, 60), Line("policy", 10, 9, 90, 5), Line("complaint", 4, 4, 100, 0), Line("order status", 30, 30, 100, 0)), table(rs, costs))
    }

    @Test
    fun e1_aSegmentWithNoCostEntryCostsOnePerErrorAndNoResultsGiveAnEmptyTable() {
        assertEquals(listOf(Line("odd", 3, 1, 33, 2)), table(results(Triple("odd", 3, 1)), costs))
        assertEquals(listOf<Line>(), table(listOf(), costs))
        assertEquals(listOf(Line("a", 2, 1, 50, 1), Line("b", 2, 1, 50, 1)), table(results(Triple("b", 2, 1), Triple("a", 2, 1)), mapOf("a" to 1, "b" to 1)))
    }

    @Test
    fun e2_aPercentileUsesTheNearestRankAndDoesNotNeedSortedInput() {
        val values = listOf(4800, 800, 1000, 900)
        assertEquals(900, percentile(values, 50))
        assertEquals(4800, percentile(values, 95))
        assertEquals(4800, percentile(values, 100))
        assertEquals(800, percentile(values, 1))
        assertEquals(7, percentile(listOf(7), 50))
        assertEquals(0, percentile(listOf(), 50))
    }

    @Test
    fun e3_aTestWithFewerCasesThanTheMinimumInEitherArmDecidesNothing() {
        assertEquals("too few cases", abVerdict(150, 199, 190, 400))
        assertEquals("too few cases", abVerdict(150, 400, 190, 199))
        assertEquals("new is better", abVerdict(100, 200, 160, 200))
        assertEquals("no clear difference", abVerdict(10, 100, 19, 100, 50))
        assertEquals("too few cases", abVerdict(10, 100, 19, 100))
    }

    @Test
    fun e4_aDifferenceIsCalledOnlyWhenItClearsThe95PercentBarAndTheBetterSideIsNamed() {
        assertEquals("new is better", abVerdict(410, 500, 438, 500))
        assertEquals("old is better", abVerdict(438, 500, 410, 500))
        assertEquals("no clear difference", abVerdict(410, 500, 431, 500))
        assertEquals("no clear difference", abVerdict(0, 300, 0, 300))
        assertEquals("no clear difference", abVerdict(300, 300, 300, 300))
    }

    @Test
    fun e5_aShadowRunIsHeldForARegressionInAProtectedSegmentOrForMoreLossesThanGains() {
        val ps = pairs(2, "refund", false, true) + pairs(1, "policy", true, false) + pairs(1, "complaint", true, false) + pairs(5, "policy", true, true)
        assertEquals(Gate("hold", 2, 2, listOf("complaint")), gate(ps, setOf("refund", "complaint")))
        assertEquals(Gate("ship", 2, 2, listOf()), gate(ps, setOf("refund")))
        val worse = pairs(3, "policy", true, false) + pairs(2, "policy", false, true)
        assertEquals(Gate("hold", 3, 2, listOf()), gate(worse, setOf()))
    }

    @Test
    fun e6_diagnosisChecksTheEvidenceThenTheGroundingThenTheFormatThenTheStrongerModel() {
        assertEquals("retrieval or data", diagnose(false, false, false, false))
        assertEquals("ungrounded answer", diagnose(true, false, false, false))
        assertEquals("format instructions", diagnose(true, true, false, false))
        assertEquals("prompt or task", diagnose(true, true, true, false))
        assertEquals("model mismatch", diagnose(true, true, true, true))
    }

    @Test
    fun e7_modelChoiceTakesTheCheapestOptionThatMeetsTheAccuracyFloorAndTheLatencyLimit() {
        val options = listOf(Option("small", 84, 900, 1), Option("medium", 91, 1800, 3), Option("large", 95, 4200, 9))
        assertEquals("medium", chooseModel(options, 90, 2000))
        assertEquals("none", chooseModel(options, 94, 2000))
        assertEquals("medium", chooseModel(options, 91, 1800))
        assertEquals("small", chooseModel(options, 80, 5000))
        assertEquals("a", chooseModel(listOf(Option("b", 90, 100, 2), Option("a", 90, 100, 2)), 90, 100))
    }
}
