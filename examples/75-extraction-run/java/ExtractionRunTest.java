import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExtractionRunTest {
    private static ExtractionRun.Extracted run(String doc) {
        return ExtractionRun.extract(doc, ExtractionRun.DOCS.get(doc).text());
    }

    @Test
    void aRecordThatIsFineIsValidOnTheFirstAttempt() {
        var result = run("d1");
        assertEquals("valid", result.status());
        assertEquals(1, result.attempts());
        assertEquals(List.of(), result.retried());
    }

    @Test
    void aSemanticErrorAndAnInventedVendorAreRetriedOnceAndThenFixed() {
        assertEquals(List.of(new ExtractionRun.Err("semantic", "total")), ExtractionRun.validate(ExtractionRun.REPLIES.get("d2").get(0), ExtractionRun.DOCS.get("d2").text()));
        assertEquals(List.of(new ExtractionRun.Err("ungrounded", "vendor")), ExtractionRun.validate(ExtractionRun.REPLIES.get("d3").get(0), ExtractionRun.DOCS.get("d3").text()));
        for (String doc : List.of("d2", "d3")) {
            var result = run(doc);
            assertEquals("valid", result.status());
            assertEquals(2, result.attempts());
        }
    }

    @Test
    void anAbsentValueIsNeverRetriedAndGoesToReview() {
        var result = run("d4");
        assertEquals("needs_review", result.status());
        assertEquals(1, result.attempts());
        assertEquals(List.of(new ExtractionRun.Err("absent", "total")), result.errors());
    }

    @Test
    void aFlaggedConflictIsInformationAndGoesToReviewWithoutARetry() {
        var result = run("d5");
        assertEquals(List.of(), ExtractionRun.validate(result.record(), ExtractionRun.DOCS.get("d5").text()));
        assertEquals("needs_review", result.status());
        assertEquals(1, result.attempts());
    }

    @Test
    void anErrorThatSurvivesTheRetryFailsTheDocumentAfterTwoAttempts() {
        var result = run("d6");
        assertEquals("failed", result.status());
        assertEquals(2, result.attempts());
        assertEquals(List.of("ungrounded"), result.retried());
    }
}
