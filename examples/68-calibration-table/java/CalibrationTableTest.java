import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CalibrationTableTest {
    private static final List<CalibrationTable.Rec> SMALL = List.of(
        new CalibrationTable.Rec("a", 95, true), new CalibrationTable.Rec("a", 95, true), new CalibrationTable.Rec("b", 55, false), new CalibrationTable.Rec("b", 55, true));

    @Test
    void ratioRoundsHalfUpAndSurvivesAnEmptyWhole() {
        assertEquals(List.of(50, 33, 67, 0), List.of(CalibrationTable.ratio(1, 2), CalibrationTable.ratio(1, 3), CalibrationTable.ratio(2, 3), CalibrationTable.ratio(0, 0)));
    }

    @Test
    void accuracyIsReportedOverallAndPerType() {
        assertEquals(75, CalibrationTable.overallAccuracy(SMALL));
        assertEquals(List.of(new CalibrationTable.TypeRow("a", 2, 2, 100), new CalibrationTable.TypeRow("b", 1, 2, 50)), CalibrationTable.accuracyByType(SMALL));
    }

    @Test
    void theTableComparesWhatABucketClaimedWithHowOftenItWasRight() {
        assertEquals(List.of(new CalibrationTable.Bucket("50-59", 2, 55, 50), new CalibrationTable.Bucket("90-100", 2, 95, 100)), CalibrationTable.calibrationTable(SMALL));
    }

    @Test
    void precisionAboveCountsWhatWouldSkipReview() {
        assertEquals(List.of(new CalibrationTable.Kept(2, 100), new CalibrationTable.Kept(4, 75), new CalibrationTable.Kept(0, 0)),
            List.of(CalibrationTable.precisionAbove(SMALL, 90), CalibrationTable.precisionAbove(SMALL, 50), CalibrationTable.precisionAbove(SMALL, 99)));
    }

    @Test
    void theGeneratedRecordsHideAWeakTypeBehindTheOverallFigure() {
        List<CalibrationTable.Rec> records = CalibrationTable.makeRecords(200);
        int handwritten = CalibrationTable.accuracyByType(records).stream().filter(t -> t.docType().equals("handwritten")).findFirst().orElseThrow().percent();
        int invoice = CalibrationTable.accuracyByType(records).stream().filter(t -> t.docType().equals("invoice")).findFirst().orElseThrow().percent();
        int overall = CalibrationTable.overallAccuracy(records);
        assertEquals(200, records.size());
        assertTrue(handwritten < overall && overall < invoice);
        assertEquals(200, CalibrationTable.calibrationTable(records).stream().mapToInt(CalibrationTable.Bucket::count).sum());
    }
}
