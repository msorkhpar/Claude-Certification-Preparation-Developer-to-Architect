import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewRoutingTest {
    private static <T> T got(T value) {
        assertNotNull(value, "the method returned nothing");
        return value;
    }

    private static List<ReviewRouting.Rec> recs(String docType, String field, int right, int wrong) {
        List<ReviewRouting.Rec> out = new ArrayList<>();
        for (int i = 0; i < right; i++) out.add(new ReviewRouting.Rec(docType, field, true));
        for (int i = 0; i < wrong; i++) out.add(new ReviewRouting.Rec(docType, field, false));
        return out;
    }

    @SafeVarargs
    private static List<ReviewRouting.Rec> all(List<ReviewRouting.Rec>... parts) {
        List<ReviewRouting.Rec> out = new ArrayList<>();
        for (List<ReviewRouting.Rec> p : parts) out.addAll(p);
        return out;
    }

    private static ReviewRouting.Seg seg(List<ReviewRouting.Seg> rows, String name) {
        return rows.stream().filter(r -> r.segment().equals(name)).findFirst().orElseThrow();
    }

    private static ReviewRouting.Labeled l(int confidence, boolean correct) {
        return new ReviewRouting.Labeled(confidence, correct);
    }

    private static ReviewRouting.Item item(String id, String stratum, int rank) {
        return new ReviewRouting.Item(id, stratum, rank);
    }

    private static ReviewRouting.Extraction ex(String id, int confidence, boolean conflict) {
        return new ReviewRouting.Extraction(id, confidence, conflict);
    }

    @Test
    void m1_accuracyIsReportedPerDocumentTypeAndFieldNextToTheOverallFigure() {
        List<ReviewRouting.Seg> rows = got(ReviewRouting.accuracyBy(all(recs("invoice", "total", 90, 0), recs("receipt", "date", 8, 2))));
        assertEquals(List.of("overall", "invoice/total", "receipt/date"), rows.stream().map(ReviewRouting.Seg::segment).toList());
        assertEquals(new ReviewRouting.Seg("overall", 98, 100, 98), rows.get(0));
        assertEquals(new ReviewRouting.Seg("receipt/date", 8, 10, 80), rows.get(2));
    }

    @Test
    void e1_aWeakSegmentIsHiddenByAHighOverallFigureAndFoundByTheBreakdown() {
        List<ReviewRouting.Seg> rows = got(ReviewRouting.accuracyBy(all(recs("invoice", "total", 970, 10), recs("handwritten", "total", 8, 12))));
        assertEquals(List.of(98, 40, 99), List.of(seg(rows, "overall").percent(), seg(rows, "handwritten/total").percent(), seg(rows, "invoice/total").percent()));
        assertEquals(0, got(ReviewRouting.accuracyBy(List.of())).get(0).percent());
    }

    @Test
    void e2_automationNeedsEverySegmentToPassAndEnoughSamplesInEach() {
        assertEquals(new ReviewRouting.Automation(false, List.of(), List.of("handwritten/total")),
            ReviewRouting.canAutomate(all(recs("invoice", "total", 97, 3), recs("receipt", "date", 50, 0), recs("handwritten", "total", 10, 0)), 95, 30));
        assertEquals(new ReviewRouting.Automation(false, List.of("receipt/date"), List.of()), ReviewRouting.canAutomate(all(recs("invoice", "total", 97, 3), recs("receipt", "date", 45, 5)), 95, 30));
        assertEquals(new ReviewRouting.Automation(true, List.of(), List.of()), ReviewRouting.canAutomate(all(recs("invoice", "total", 97, 3), recs("receipt", "date", 50, 0)), 95, 30));
        assertFalse(got(ReviewRouting.canAutomate(List.of(), 95, 30)).automate());
        assertEquals(new ReviewRouting.Automation(false, List.of("receipt/date"), List.of()), ReviewRouting.canAutomate(all(recs("invoice", "total", 97, 3), recs("receipt", "date", 20, 10)), 95, 30));
        assertEquals(new ReviewRouting.Automation(true, List.of(), List.of()), ReviewRouting.canAutomate(recs("invoice", "total", 95, 5), 95, 30));
    }

    @Test
    void e3_theThresholdIsTheLowestConfidenceWhoseAcceptedItemsMeetTheTargetPrecision() {
        List<ReviewRouting.Labeled> labeled = List.of(l(95, true), l(90, true), l(85, true), l(80, false), l(70, false), l(60, false));
        assertEquals(85, ReviewRouting.calibrateThreshold(labeled, 90));
        assertEquals(80, ReviewRouting.calibrateThreshold(labeled, 70));
        assertEquals(60, ReviewRouting.calibrateThreshold(labeled, 50));
    }

    @Test
    void e4_noThresholdExistsWhenNoConfidenceLevelMeetsTheTarget() {
        assertNull(ReviewRouting.calibrateThreshold(List.of(l(95, false), l(90, false)), 90));
        assertNull(ReviewRouting.calibrateThreshold(List.of(), 90));
        assertEquals(95, ReviewRouting.calibrateThreshold(List.of(l(95, true), l(90, false)), 100));
    }

    @Test
    void e5_theStratifiedSampleTakesTheBestRankedItemsOfEveryStratum() {
        List<ReviewRouting.Item> items = List.of(item("a1", "invoice", 5), item("a2", "invoice", 1), item("a3", "invoice", 3), item("b1", "receipt", 9), item("c1", "handwritten", 2), item("c2", "handwritten", 2));
        assertEquals(List.of("a2", "a3", "b1", "c1", "c2"), ReviewRouting.stratifiedSample(items, 2));
        assertEquals(List.of("a2", "b1", "c1"), ReviewRouting.stratifiedSample(items, 1));
        assertEquals(List.of(), ReviewRouting.stratifiedSample(List.of(), 3));
    }

    @Test
    void e6_lowConfidenceAndConflictsGoToReviewWithTheWeakestFirst() {
        List<ReviewRouting.Extraction> rows = List.of(ex("x1", 90, false), ex("x2", 60, false), ex("x3", 99, true), ex("x4", 79, false), ex("x5", 80, false));
        assertEquals(new ReviewRouting.Routing(List.of("x3", "x2", "x4"), List.of(), List.of("x1", "x5")), ReviewRouting.route(rows, 80, 10));
    }

    @Test
    void e7_reviewCapacityIsRespectedAndTheRestWaitInABacklog() {
        List<ReviewRouting.Extraction> rows = new ArrayList<>();
        for (int i = 0; i < 5; i++) rows.add(ex("r" + i, 50 + i, false));
        rows.add(ex("ok", 99, false));
        assertEquals(new ReviewRouting.Routing(List.of("r0", "r1"), List.of("r2", "r3", "r4"), List.of("ok")), ReviewRouting.route(rows, 80, 2));
        assertEquals(List.of(), got(ReviewRouting.route(rows, 80, 0)).review());
    }

    @Test
    void e8_anIrreversibleActionNeedsAPersonWhateverTheConfidence() {
        assertEquals(List.of("human", "human"), List.of(got(ReviewRouting.checkpoint("delete_records", 1)), got(ReviewRouting.checkpoint("send_payment", 5))));
        assertEquals(List.of("auto", "human"), List.of(got(ReviewRouting.checkpoint("update_label", 50)), got(ReviewRouting.checkpoint("update_label", 5000))));
        assertEquals(List.of("auto", "human"), List.of(got(ReviewRouting.checkpoint("update_label", 1000)), got(ReviewRouting.checkpoint("update_label", 300, 200))));
    }
}
