import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExtractionChecksTest {
    private static final ExtractionChecks.Invoice WRONG = new ExtractionChecks.Invoice(List.of(100.0, 20.5), 130.0, "Total due: 130.00 EUR");
    private static final ExtractionChecks.Invoice RIGHT = new ExtractionChecks.Invoice(List.of(100.0, 20.5, 9.5), 130.0, "Total due: 130.00 EUR");

    @Test
    void aRequiredFieldGetsFilledAndANullableOneStaysNull() {
        assertEquals("PO-0000", ExtractionChecks.scriptedValue("no order here", false));
        assertNull(ExtractionChecks.scriptedValue("no order here", true));
        assertEquals("4471", ExtractionChecks.scriptedValue("Order PO 4471 shipped", false));
    }

    @Test
    void theChecksFindAWrongSumAndAQuotationThatIsNotInTheDocument() {
        assertEquals(List.of(), ExtractionChecks.check(RIGHT, ExtractionChecks.DOC));
        assertEquals(List.of("total: the items add up to 120.5, not 130.0"), ExtractionChecks.check(WRONG, ExtractionChecks.DOC));
        assertEquals(List.of("evidence: this quotation is not in the document"), ExtractionChecks.check(RIGHT.withEvidence("Total due: 130.00 USD"), ExtractionChecks.DOC));
    }

    @Test
    void aRetryCarriesTheDocumentTheFailedAnswerAndTheProblems() {
        var result = ExtractionChecks.extract(ExtractionChecks.DOC, List.of(WRONG, RIGHT));
        assertEquals("valid", result.status());
        assertEquals(2, result.attempts());
        assertEquals(1, result.feedback().size());
        String text = result.feedback().get(0);
        assertTrue(text.contains(ExtractionChecks.DOC));
        assertTrue(text.contains("\"total\": 130.0"));
        assertTrue(text.contains("- total: the items add up to 120.5, not 130.0"));
        assertEquals("failed", ExtractionChecks.extract(ExtractionChecks.DOC, List.of(WRONG, WRONG), 1).status());
        assertEquals("failed", ExtractionChecks.extract(ExtractionChecks.DOC, List.of(WRONG, RIGHT), 0).status());
    }

    private static Map<String, Double> accuracy(double validated, double all) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("validated_only", validated);
        m.put("all_documents", all);
        return m;
    }

    @Test
    void accuracyOnValidatedRecordsAloneHidesTheFailures() {
        List<ExtractionChecks.Outcome> outcomes = new ArrayList<>(ExtractionChecks.repeat("valid", true, 5));
        outcomes.addAll(ExtractionChecks.repeat("valid", false, 1));
        outcomes.addAll(ExtractionChecks.repeat("failed", false, 4));
        assertEquals(accuracy(0.83, 0.5), ExtractionChecks.accuracy(outcomes));
        assertEquals(accuracy(0.0, 0.0), ExtractionChecks.accuracy(List.of()));
    }

    private static Map<String, Object> choice(String toolChoice, boolean checkReply) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tool_choice", toolChoice);
        m.put("check_reply", checkReply);
        return m;
    }

    @Test
    void forcedChoiceIsUsedWhereAcceptedAndAutoWithAReplyCheckElsewhere() {
        List<String> two = List.of("a", "b");
        assertEquals(choice("any", false), ExtractionChecks.requestChoice("claude-haiku-4-5", two));
        assertEquals(choice("tool:a", false), ExtractionChecks.requestChoice("claude-haiku-4-5", List.of("a")));
        assertEquals(choice("auto", true), ExtractionChecks.requestChoice("claude-opus-5-5", two));
    }
}
