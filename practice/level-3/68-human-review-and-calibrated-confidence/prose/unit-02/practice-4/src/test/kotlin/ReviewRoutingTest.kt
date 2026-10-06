import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReviewRoutingTest {
    private fun <T : Any> got(value: T?): T {
        assertNotNull(value, "the function returned nothing")
        return value!!
    }

    private fun recs(docType: String, field: String, right: Int, wrong: Int) = List(right) { Rec(docType, field, true) } + List(wrong) { Rec(docType, field, false) }

    private fun seg(rows: List<Seg>, name: String) = rows.first { it.segment == name }

    @Test
    fun m1_accuracyIsReportedPerDocumentTypeAndFieldNextToTheOverallFigure() {
        val rows = got(accuracyBy(recs("invoice", "total", 90, 0) + recs("receipt", "date", 8, 2)))
        assertEquals(listOf("overall", "invoice/total", "receipt/date"), rows.map { it.segment })
        assertEquals(Seg("overall", 98, 100, 98), rows[0])
        assertEquals(Seg("receipt/date", 8, 10, 80), rows[2])
    }

    @Test
    fun e1_aWeakSegmentIsHiddenByAHighOverallFigureAndFoundByTheBreakdown() {
        val rows = got(accuracyBy(recs("invoice", "total", 970, 10) + recs("handwritten", "total", 8, 12)))
        assertEquals(listOf(98, 40, 99), listOf(seg(rows, "overall").percent, seg(rows, "handwritten/total").percent, seg(rows, "invoice/total").percent))
        assertEquals(0, got(accuracyBy(emptyList()))[0].percent)
    }

    @Test
    fun e2_automationNeedsEverySegmentToPassAndEnoughSamplesInEach() {
        assertEquals(Automation(false, emptyList(), listOf("handwritten/total")), canAutomate(recs("invoice", "total", 97, 3) + recs("receipt", "date", 50, 0) + recs("handwritten", "total", 10, 0), 95, 30))
        assertEquals(Automation(false, listOf("receipt/date"), emptyList()), canAutomate(recs("invoice", "total", 97, 3) + recs("receipt", "date", 45, 5), 95, 30))
        assertEquals(Automation(true, emptyList(), emptyList()), canAutomate(recs("invoice", "total", 97, 3) + recs("receipt", "date", 50, 0), 95, 30))
        assertFalse(got(canAutomate(emptyList(), 95, 30)).automate)
        assertEquals(Automation(false, listOf("receipt/date"), emptyList()), canAutomate(recs("invoice", "total", 97, 3) + recs("receipt", "date", 20, 10), 95, 30))
        assertEquals(Automation(true, emptyList(), emptyList()), canAutomate(recs("invoice", "total", 95, 5), 95, 30))
    }

    @Test
    fun e3_theThresholdIsTheLowestConfidenceWhoseAcceptedItemsMeetTheTargetPrecision() {
        val labeled = listOf(Labeled(95, true), Labeled(90, true), Labeled(85, true), Labeled(80, false), Labeled(70, false), Labeled(60, false))
        assertEquals(85, calibrateThreshold(labeled, 90))
        assertEquals(80, calibrateThreshold(labeled, 70))
        assertEquals(60, calibrateThreshold(labeled, 50))
    }

    @Test
    fun e4_noThresholdExistsWhenNoConfidenceLevelMeetsTheTarget() {
        assertNull(calibrateThreshold(listOf(Labeled(95, false), Labeled(90, false)), 90))
        assertNull(calibrateThreshold(emptyList(), 90))
        assertEquals(95, calibrateThreshold(listOf(Labeled(95, true), Labeled(90, false)), 100))
    }

    @Test
    fun e5_theStratifiedSampleTakesTheBestRankedItemsOfEveryStratum() {
        val items = listOf(Item("a1", "invoice", 5), Item("a2", "invoice", 1), Item("a3", "invoice", 3), Item("b1", "receipt", 9), Item("c1", "handwritten", 2), Item("c2", "handwritten", 2))
        assertEquals(listOf("a2", "a3", "b1", "c1", "c2"), stratifiedSample(items, 2))
        assertEquals(listOf("a2", "b1", "c1"), stratifiedSample(items, 1))
        assertEquals(emptyList<String>(), stratifiedSample(emptyList(), 3))
    }

    @Test
    fun e6_lowConfidenceAndConflictsGoToReviewWithTheWeakestFirst() {
        val rows = listOf(Extraction("x1", 90, false), Extraction("x2", 60, false), Extraction("x3", 99, true), Extraction("x4", 79, false), Extraction("x5", 80, false))
        assertEquals(Routing(listOf("x3", "x2", "x4"), emptyList(), listOf("x1", "x5")), route(rows, 80, 10))
    }

    @Test
    fun e7_reviewCapacityIsRespectedAndTheRestWaitInABacklog() {
        val rows = List(5) { Extraction("r$it", 50 + it, false) } + Extraction("ok", 99, false)
        assertEquals(Routing(listOf("r0", "r1"), listOf("r2", "r3", "r4"), listOf("ok")), route(rows, 80, 2))
        assertEquals(emptyList<String>(), got(route(rows, 80, 0)).review)
    }

    @Test
    fun e8_anIrreversibleActionNeedsAPersonWhateverTheConfidence() {
        assertEquals(listOf("human", "human"), listOf(got(checkpoint("delete_records", 1)), got(checkpoint("send_payment", 5))))
        assertEquals(listOf("auto", "human"), listOf(got(checkpoint("update_label", 50)), got(checkpoint("update_label", 5000))))
        assertEquals(listOf("auto", "human"), listOf(got(checkpoint("update_label", 1000)), got(checkpoint("update_label", 300, 200))))
    }
}
