import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExtractionTest {
    private static final String DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 120.50 EUR\nThank you.";
    private static final String DOC_NO_VENDOR = "Items: 10.00\nTotal due: 10.00 USD";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Object copy(Object value) {
        if (value instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) out.put((String) e.getKey(), copy(e.getValue()));
            return out;
        }
        if (value instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            for (Object o : l) out.add(copy(o));
            return out;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyOf(Map<String, Object> m) {
        return (Map<String, Object>) copy(m);
    }

    private static Map<String, Object> good() {
        return map("vendor", "Acme Tools", "currency", "EUR", "currency_detail", null, "line_items", List.of(100.0, 20.5), "stated_total", 120.5, "calculated_total", 120.5, "conflict_detected", false,
                "provenance", map("vendor", "Invoice from Acme Tools", "currency", "120.50 EUR", "stated_total", "Total due: 120.50 EUR"));
    }

    private static Map<String, Object> record(Object... over) {
        Map<String, Object> r = good();
        for (int i = 0; i < over.length; i += 2) r.put((String) over[i], over[i + 1]);
        return r;
    }

    private static Map<String, Object> prov(Object... kv) {
        return map(kv);
    }

    private static final class Scripted {
        final List<Map<String, Object>> records = new ArrayList<>();
        final List<Map<String, Object>> calls = new ArrayList<>();

        Scripted(List<Map<String, Object>> records) {
            this.records.addAll(records);
        }

        Map<String, Object> call(String document, Map<String, Object> feedback) {
            calls.add(map("document", document, "feedback", feedback == null ? null : copy(feedback)));
            return copyOf(records.get(Math.min(calls.size() - 1, records.size() - 1)));
        }
    }

    private static Map<String, Object> run(Scripted scripted, String document, List<String> required, int maxRetries) {
        Map<String, Object> result = Extraction.extractDocument(document, scripted::call, required, maxRetries);
        assertNotNull(result, "extractDocument returned nothing");
        return result;
    }

    private static List<String> kinds(List<Map<String, Object>> errors) {
        assertNotNull(errors, "validate returned nothing");
        List<String> out = new ArrayList<>();
        for (Map<String, Object> e : errors) out.add(e.get("kind") + ":" + e.get("field"));
        return out;
    }

    @Test
    void m1_aDocumentWithEveryValuePresentAndQuotedComesBackValidOnTheFirstAttempt() {
        Scripted s = new Scripted(List.of(good()));
        Map<String, Object> result = run(s, DOC, List.of(), 2);
        assertEquals(map("status", "valid", "record", good(), "attempts", 1, "errors", List.of()), result);
        assertEquals(List.of(map("document", DOC, "feedback", null)), s.calls);
    }

    @Test
    void e1_aValueTheDocumentDoesNotGiveIsNullAndNeedsNoQuoteWhileAnInventedValueFailsAsUngrounded() {
        Map<String, Object> noVendor = record("vendor", null, "currency", "USD", "line_items", List.of(10.0), "stated_total", 10.0, "calculated_total", 10.0, "provenance", prov("currency", "10.00 USD", "stated_total", "Total due: 10.00 USD"));
        assertEquals(List.of(), kinds(Extraction.validate(noVendor, DOC_NO_VENDOR)));
        Map<String, Object> invented = record("currency", "USD", "line_items", List.of(10.0), "stated_total", 10.0, "calculated_total", 10.0,
                "provenance", prov("vendor", "Invoice from Acme Tools", "currency", "10.00 USD", "stated_total", "Total due: 10.00 USD"));
        assertEquals(List.of("ungrounded:vendor"), kinds(Extraction.validate(invented, DOC_NO_VENDOR)));
        Map<String, Object> unquoted = record("provenance", prov("vendor", "Invoice from Acme Tools", "currency", "120.50 EUR"));
        assertEquals(List.of("ungrounded:stated_total"), kinds(Extraction.validate(unquoted, DOC)));
        assertEquals(List.of("ungrounded:vendor"), kinds(Extraction.validate(record("provenance", prov("vendor", "", "currency", "120.50 EUR", "stated_total", "Total due: 120.50 EUR")), DOC)));
    }

    @SuppressWarnings("unchecked")
    @Test
    void e2_aRetryCarriesTheOriginalDocumentTheFailedRecordAndOnlyTheErrorsASecondLookCanFix() {
        Map<String, Object> bad = record("vendor", "Acme Corp", "provenance", prov("vendor", "Invoice from Acme Corp", "currency", "120.50 EUR", "stated_total", "Total due: 120.50 EUR"));
        Scripted s = new Scripted(List.of(bad, good()));
        Map<String, Object> result = run(s, DOC, List.of("vendor", "currency"), 2);
        assertEquals("valid", result.get("status"));
        assertEquals(2, result.get("attempts"));
        assertEquals(2, s.calls.size());
        assertEquals(map("document", DOC, "feedback", null), s.calls.get(0));
        assertEquals(DOC, s.calls.get(1).get("document"));
        Map<String, Object> feedback = (Map<String, Object>) s.calls.get(1).get("feedback");
        assertEquals(bad, feedback.get("previous"));
        assertEquals(List.of("ungrounded:vendor"), kinds((List<Map<String, Object>>) feedback.get("errors")));
        for (Map<String, Object> e : (List<Map<String, Object>>) feedback.get("errors")) assertFalse(((String) e.get("message")).isEmpty());
        Map<String, Object> mixed = record("vendor", "Acme Corp", "currency", "unclear", "provenance", prov("vendor", "Invoice from Acme Corp", "stated_total", "Total due: 120.50 EUR"));
        Scripted t = new Scripted(List.of(mixed, good()));
        run(t, DOC, List.of("currency"), 2);
        assertEquals(List.of("ungrounded:vendor"), kinds((List<Map<String, Object>>) ((Map<String, Object>) t.calls.get(1).get("feedback")).get("errors")), "the absent currency is not something a second look can fix");
    }

    @Test
    void e3_aRequiredValueThatTheModelReportsAsAbsentIsNotRetriedAndGoesToReview() {
        Map<String, Object> nothing = record("stated_total", null, "provenance", prov("vendor", "Invoice from Acme Tools", "currency", "120.50 EUR"));
        Scripted s = new Scripted(List.of(nothing, good()));
        Map<String, Object> result = run(s, DOC, List.of("stated_total"), 2);
        assertEquals("needs_review", result.get("status"));
        assertEquals(1, result.get("attempts"));
        assertEquals(1, s.calls.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) result.get("errors");
        assertEquals(List.of("absent:stated_total"), kinds(errors));
        assertEquals("valid", run(new Scripted(List.of(nothing)), DOC, List.of(), 2).get("status"), "a value that is not required may stay null");
    }

    @SuppressWarnings("unchecked")
    @Test
    void e4_retriesStopAfterTheLimitAndTheDocumentIsMarkedFailed() {
        Map<String, Object> bad = record("currency", "dollars");
        Scripted s = new Scripted(List.of(bad));
        Map<String, Object> result = run(s, DOC, List.of(), 2);
        assertEquals("failed", result.get("status"));
        assertEquals(3, result.get("attempts"));
        assertEquals(3, s.calls.size());
        assertEquals(List.of("syntax:currency"), kinds((List<Map<String, Object>>) result.get("errors")));
        result = run(new Scripted(List.of(bad)), DOC, List.of(), 0);
        assertEquals("failed", result.get("status"));
        assertEquals(1, result.get("attempts"));
        result = run(new Scripted(List.of(bad, good())), DOC, List.of(), 1);
        assertEquals("valid", result.get("status"));
        assertEquals(2, result.get("attempts"));
        assertEquals("failed", run(new Scripted(List.of(map("vendor", "x"))), DOC, List.of(), 2).get("status"));
    }

    @Test
    void e5_aTotalThatDiffersFromTheLineItemsIsASemanticErrorAndAFlaggedConflictGoesToReviewWithoutARetry() {
        Map<String, Object> wrongSum = record("calculated_total", 100.0, "stated_total", 100.0);
        assertEquals(List.of("semantic:calculated_total"), kinds(Extraction.validate(wrongSum, DOC)));
        assertEquals(List.of("semantic:stated_total"), kinds(Extraction.validate(record("stated_total", 130.0), DOC)));
        Map<String, Object> flagged = record("stated_total", 130.0, "conflict_detected", true);
        assertEquals(List.of(), kinds(Extraction.validate(flagged, DOC)), "a conflict the model flagged is information, not an error");
        Scripted s = new Scripted(List.of(flagged));
        Map<String, Object> result = run(s, DOC, List.of(), 2);
        assertEquals("needs_review", result.get("status"));
        assertEquals(1, result.get("attempts"));
        assertEquals(List.of(), result.get("errors"));
        assertEquals(1, s.calls.size());
    }

    @Test
    void e6_currencyTakesUnclearAndOtherWithADetailAndRejectsAnythingElseAsASyntaxError() {
        Map<String, Object> noCurrency = record("currency", "unclear", "provenance", prov("vendor", "Invoice from Acme Tools", "stated_total", "Total due: 120.50 EUR"));
        assertEquals(List.of(), kinds(Extraction.validate(noCurrency, DOC)));
        String chf = "Invoice from Acme Tools.\nTotal due: 120.50 CHF";
        Map<String, Object> other = record("currency", "other", "currency_detail", "CHF", "provenance", prov("vendor", "Invoice from Acme Tools", "currency", "120.50 CHF", "stated_total", "Total due: 120.50 CHF"));
        assertEquals(List.of(), kinds(Extraction.validate(other, chf)));
        Map<String, Object> noDetail = new LinkedHashMap<>(other);
        noDetail.put("currency_detail", null);
        assertEquals(List.of("syntax:currency_detail"), kinds(Extraction.validate(noDetail, chf)));
        noDetail.put("currency_detail", "  ");
        assertEquals(List.of("syntax:currency_detail"), kinds(Extraction.validate(noDetail, chf)));
        assertEquals(List.of("syntax:currency"), kinds(Extraction.validate(record("currency", "dollars"), DOC)));
        assertEquals(List.of("syntax:line_items"), kinds(Extraction.validate(record("line_items", "100"), DOC)));
        Map<String, Object> missing = record();
        missing.remove("provenance");
        assertEquals(List.of("syntax:provenance"), kinds(Extraction.validate(missing, DOC)));
    }

    private static Map<String, Object> chunk(Object... over) {
        Map<String, Object> c = map("vendor", null, "currency", "unclear", "currency_detail", null, "line_items", List.of(), "stated_total", null, "calculated_total", 0, "conflict_detected", false, "provenance", map());
        for (int i = 0; i < over.length; i += 2) c.put((String) over[i], over[i + 1]);
        return c;
    }

    @Test
    void e7_chunkResultsMergeByKeepingTheFirstValueAndRecordingAConflictWhenTwoChunksDisagree() {
        Map<String, Object> one = chunk("vendor", "Acme Tools", "line_items", List.of(100.0), "provenance", prov("vendor", "Invoice from Acme Tools"));
        Map<String, Object> two = chunk("currency", "EUR", "line_items", List.of(20.5), "stated_total", 120.5, "provenance", prov("currency", "120.50 EUR", "stated_total", "Total due: 120.50 EUR"));
        Map<String, Object> three = chunk("vendor", "Acme Tool Ltd", "stated_total", 120.5, "provenance", prov("vendor", "Acme Tool Ltd", "stated_total", "Total due: 120.50 EUR"));
        Map<String, Object> merged = Extraction.mergeChunks(List.of(one, two, three));
        assertNotNull(merged, "mergeChunks returned nothing");
        assertEquals(map("vendor", "Acme Tools", "currency", "EUR", "currency_detail", null, "line_items", List.of(100.0, 20.5), "stated_total", 120.5, "calculated_total", 120.5, "conflict_detected", true,
                "provenance", map("vendor", "Invoice from Acme Tools", "currency", "120.50 EUR", "stated_total", "Total due: 120.50 EUR"), "conflicts", List.of("vendor")), merged);
        Map<String, Object> clean = Extraction.mergeChunks(List.of(one, two));
        assertEquals(List.of(), clean.get("conflicts"));
        assertEquals(false, clean.get("conflict_detected"));
        assertEquals("Acme Tools", clean.get("vendor"));
        assertNull(Extraction.mergeChunks(List.of(chunk(), chunk())).get("vendor"));
        assertEquals(true, Extraction.mergeChunks(List.of(chunk("conflict_detected", true))).get("conflict_detected"));
    }

    @Test
    void e8_accuracyCountsEveryDocumentAndNotOnlyTheValidatedOnes() {
        Map<String, Map<String, Object>> labels = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) labels.put("d" + i, map("vendor", "V" + i, "stated_total", (double) i));
        Map<String, Map<String, Object>> results = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) results.put("d" + i, map("status", "valid", "record", map("vendor", "V" + i, "stated_total", (double) i)));
        results.put("d5", map("status", "valid", "record", map("vendor", "wrong", "stated_total", 5.0)));
        results.put("d6", map("status", "needs_review", "record", map("vendor", "V6", "stated_total", 6.0)));
        results.put("d7", map("status", "needs_review", "record", map("vendor", "V7", "stated_total", 7.0)));
        results.put("d8", map("status", "failed", "record", map("vendor", "V8", "stated_total", 8.0)));
        Map<String, Object> report = Extraction.accuracy(results, labels);
        assertNotNull(report, "accuracy returned nothing");
        assertEquals(map("all_documents", 0.5, "validated_only", 0.83, "validated", 6, "total", 10), report);
        assertEquals(map("all_documents", 0.0, "validated_only", 0.0, "validated", 0, "total", 0), Extraction.accuracy(Map.of(), Map.of()));
    }

    private static Map<String, Object> choice(Map<String, Object> toolChoice, boolean verify) {
        return map("tool_choice", toolChoice, "strict", true, "verify_reply", verify);
    }

    @Test
    void e9_theRequestForcesAToolWhereTheModelAllowsItAndFallsBackToAutoWithAReplyCheckWhereItDoesNot() {
        List<String> two = List.of("extract_invoice", "extract_receipt");
        assertEquals(choice(map("type", "any"), false), Extraction.requestChoice("claude-haiku-4-5", two, null));
        assertEquals(choice(map("type", "tool", "name", "extract_invoice"), false), Extraction.requestChoice("claude-haiku-4-5", List.of("extract_invoice"), null));
        assertEquals(choice(map("type", "tool", "name", "extract_metadata"), false), Extraction.requestChoice("claude-haiku-4-5", two, "extract_metadata"));
        for (String model : List.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1")) {
            assertEquals(choice(map("type", "auto"), true), Extraction.requestChoice(model, two, null));
            assertEquals(choice(map("type", "auto"), true), Extraction.requestChoice(model, two, "extract_metadata"));
        }
    }
}
