import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BatchReviewTest {
    private static BatchReview.Finding finding(String file, int line, String severity, String issue, int confidence) {
        return new BatchReview.Finding(file, line, severity, issue, confidence);
    }

    private static BatchReview.Finding finding(String severity, int confidence) {
        return finding("a.py", 10, severity, "unchecked input", confidence);
    }

    private static BatchReview.Result r(String id, String kind) {
        return new BatchReview.Result(id, kind);
    }

    private static BatchReview.Step s(String id, String action) {
        return new BatchReview.Step(id, action);
    }

    @Test
    void m1_theIntervalBetweenSubmissionsLeavesRoomForTheWindowAndTheHandling() {
        assertEquals(4, BatchReview.submissionInterval(30));
        assertEquals(20, BatchReview.submissionInterval(48, 24, 4));
    }

    @Test
    void e1_anSlaWithoutRoomForABatchIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> BatchReview.submissionInterval(26));
        assertThrows(IllegalArgumentException.class, () -> BatchReview.submissionInterval(20));
    }

    @Test
    void e2_aBlockingCheckOrAToolLoopNeedsTheSynchronousApi() {
        assertEquals("synchronous", BatchReview.chooseApi(true, false));
        assertEquals("batch", BatchReview.chooseApi(false, false));
        assertEquals("synchronous", BatchReview.chooseApi(false, true));
    }

    @Test
    void e3_onlyTheItemsThatDidNotSucceedAreResubmittedByCustomId() {
        List<BatchReview.Result> results = List.of(r("a1", "succeeded"), r("a2", "expired"), r("a3", "succeeded"), r("a4", "canceled"), r("a5", "server_error"));
        assertEquals(List.of(s("a2", "resubmit"), s("a4", "resubmit"), s("a5", "resubmit")), BatchReview.resubmissionPlan(results, Map.of(), 1000));
        assertEquals(List.of(), BatchReview.resubmissionPlan(List.of(r("a1", "succeeded")), Map.of(), 1000));
    }

    @Test
    void e4_anItemOverTheLimitIsChunkedAndARejectedRequestIsFixedFirst() {
        List<BatchReview.Result> results = List.of(r("big", "invalid_request"), r("bad", "invalid_request"), r("late", "expired"), r("bigexp", "expired"));
        Map<String, Integer> sizes = Map.of("big", 5000, "bad", 100, "late", 100, "bigexp", 5000);
        assertEquals(List.of(s("big", "chunk"), s("bad", "fix"), s("late", "resubmit"), s("bigexp", "chunk")), BatchReview.resubmissionPlan(results, sizes, 1000));
    }

    @Test
    void e5_aMultiFileReviewGetsALocalPassPerFileAndOneIntegrationPass() {
        List<BatchReview.Pass> plan = BatchReview.reviewPlan(List.of("a.py", "b.py", "c.py"));
        assertEquals(List.of("local:a.py", "local:b.py", "local:c.py", "integration"), plan.stream().map(BatchReview.Pass::name).toList());
        assertEquals(List.of("a.py"), plan.get(0).files());
        assertEquals(List.of("a.py", "b.py", "c.py"), plan.get(3).files());
        assertEquals(List.of("local:a.py"), BatchReview.reviewPlan(List.of("a.py")).stream().map(BatchReview.Pass::name).toList());
    }

    @Test
    void e6_theSameFindingFromTwoPassesIsOneFindingWithTheHighestSeverityAndTheLowestConfidence() {
        List<BatchReview.Merged> merged = BatchReview.mergePasses(List.of(List.of(finding("low", 95)), List.of(finding("high", 85), finding("b.py", 3, "medium", "race", 90))));
        assertEquals(2, merged.size());
        BatchReview.Merged first = merged.get(0);
        assertEquals(List.of("a.py", 10, "high", 2, 85), List.of(first.file(), first.line(), first.severity(), first.passes(), first.confidence()));
        assertEquals(1, merged.get(1).passes());
    }

    @Test
    void e7_aFindingIsAcceptedOnlyWhenTwoIndependentPassesAgreeWithConfidence() {
        List<BatchReview.Merged> samePass = BatchReview.mergePasses(List.of(List.of(finding("medium", 90), finding("medium", 90)), List.of()));
        assertEquals(1, samePass.get(0).passes());
        assertEquals("verify", samePass.get(0).route());
        assertEquals("verify", BatchReview.mergePasses(List.of(List.of(finding("medium", 99)))).get(0).route());
        assertEquals("verify", BatchReview.mergePasses(List.of(List.of(finding("medium", 60)), List.of(finding("medium", 90)))).get(0).route());
        assertEquals("accept", BatchReview.mergePasses(List.of(List.of(finding("medium", 80)), List.of(finding("medium", 90)))).get(0).route());
    }
}
