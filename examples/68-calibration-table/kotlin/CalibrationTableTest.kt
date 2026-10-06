import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CalibrationTableTest {
    private val small = listOf(Rec("a", 95, true), Rec("a", 95, true), Rec("b", 55, false), Rec("b", 55, true))

    @Test
    fun ratioRoundsHalfUpAndSurvivesAnEmptyWhole() {
        assertEquals(listOf(50, 33, 67, 0), listOf(ratio(1, 2), ratio(1, 3), ratio(2, 3), ratio(0, 0)))
    }

    @Test
    fun accuracyIsReportedOverallAndPerType() {
        assertEquals(75, overallAccuracy(small))
        assertEquals(listOf(TypeRow("a", 2, 2, 100), TypeRow("b", 1, 2, 50)), accuracyByType(small))
    }

    @Test
    fun theTableComparesWhatABucketClaimedWithHowOftenItWasRight() {
        assertEquals(listOf(Bucket("50-59", 2, 55, 50), Bucket("90-100", 2, 95, 100)), calibrationTable(small))
    }

    @Test
    fun precisionAboveCountsWhatWouldSkipReview() {
        assertEquals(listOf(Kept(2, 100), Kept(4, 75), Kept(0, 0)), listOf(precisionAbove(small, 90), precisionAbove(small, 50), precisionAbove(small, 99)))
    }

    @Test
    fun theGeneratedRecordsHideAWeakTypeBehindTheOverallFigure() {
        val records = makeRecords()
        val byType = accuracyByType(records).associate { it.docType to it.percent }
        assertEquals(200, records.size)
        assertTrue(byType.getValue("handwritten") < overallAccuracy(records) && overallAccuracy(records) < byType.getValue("invoice"))
        assertEquals(200, calibrationTable(records).sumOf { it.count })
    }
}
