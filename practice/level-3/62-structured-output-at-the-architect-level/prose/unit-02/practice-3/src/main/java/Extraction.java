import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/** An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md. Results are JSON-like maps. */
final class Extraction {
    private static final System.Logger LOG = System.getLogger(Extraction.class.getName());
    private Extraction() {}

    static final List<String> CURRENCIES = List.of("USD", "EUR", "GBP", "other", "unclear");
    private static final List<String> KEYS = List.of("vendor", "currency", "currency_detail", "line_items", "stated_total", "calculated_total", "conflict_detected", "provenance");
    private static final List<String> RETRYABLE = List.of("syntax", "semantic", "ungrounded");
    /** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
    private static final Set<String> NO_FORCING = Set.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1");

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static boolean isNumber(Object value) {
        return value instanceof Number;
    }

    private static double round2(double x) {
        return Math.round(x * 100) / 100.0;
    }

    private static void err(List<Map<String, Object>> errors, String kind, String field, String message) {
        errors.add(map("kind", kind, "field", field, "message", message));
    }

    private static boolean quoteFound(Object quote, String document) {
        // TODO 1 of 10 (finish this to pass e1): is a provenance quote real?
        // Receives the quote the model gave for a field and the document text. Returns true only when the quote is a non-empty String that
        // appears in the document, so an invented value cannot be supported by an invented quote.
        // Example: quoteFound("Acme Ltd", "Invoice from Acme Ltd") -> true, quoteFound("Zed Corp", "Invoice from Acme Ltd") -> false
        return true;
    }

    private static boolean currencyOk(Object currency) {
        // TODO 2 of 10 (finish this to pass e6): is this a currency the schema allows?
        // Receives the value of "currency". Returns true when it is one of CURRENCIES ("unclear" and "other" count, anything else does not).
        // Example: currencyOk("unclear") -> true, currencyOk("dollars") -> false
        return true;
    }

    private static boolean detailMissing(Object currency, Object detail) {
        // TODO 3 of 10 (finish this to pass e6): is a required currency detail missing?
        // Receives "currency" and "currency_detail". Returns true when the currency is "other" and the detail is not a non-blank String.
        // Example: detailMissing("other", "  ") -> true, detailMissing("other", "CHF") -> false, detailMissing("USD", null) -> false
        return false;
    }

    private static void checkSemantics(Map<String, Object> record, List<Map<String, Object>> errors) {
        // TODO 10 of 10 (finish this to pass e5): report what a schema cannot check about the numbers.
        // Receives a record whose types are already valid and the `errors` list; add errors with err(errors, "semantic", field, message).
        // Add one on "calculated_total" when it is not the sum of "line_items" (to half a cent), and one on "stated_total" when it is not
        // null, differs from calculated_total (by more than half a cent) and "conflict_detected" is false; a conflict the model flagged
        // is information, not an error. It returns nothing.
        // Example: line_items [10, 5], calculated_total 15, stated_total 20, conflict_detected false -> one semantic error on stated_total
    }

    static List<Map<String, Object>> validate(Map<String, Object> record, String document) {
        LOG.log(System.Logger.Level.DEBUG, "validate input: {0}", record);
        return validate(record, document, List.of());
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> validate(Map<String, Object> record, String document, List<String> required) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (String key : KEYS) if (!record.containsKey(key)) err(errors, "syntax", key, "is missing");
        if (!errors.isEmpty()) return errors;
        if (record.get("vendor") != null && !(record.get("vendor") instanceof String)) err(errors, "syntax", "vendor", "must be a string or null");
        if (!currencyOk(record.get("currency"))) err(errors, "syntax", "currency", "'" + record.get("currency") + "' is not one of " + CURRENCIES);
        Object detail = record.get("currency_detail");
        if (detailMissing(record.get("currency"), detail)) err(errors, "syntax", "currency_detail", "is required when the currency is other");
        Object items = record.get("line_items");
        if (!(items instanceof List<?> list && list.stream().allMatch(Extraction::isNumber))) err(errors, "syntax", "line_items", "must be a list of numbers");
        if (record.get("stated_total") != null && !isNumber(record.get("stated_total"))) err(errors, "syntax", "stated_total", "must be a number or null");
        if (!isNumber(record.get("calculated_total"))) err(errors, "syntax", "calculated_total", "must be a number");
        if (!(record.get("conflict_detected") instanceof Boolean)) err(errors, "syntax", "conflict_detected", "must be true or false");
        if (!(record.get("provenance") instanceof Map)) err(errors, "syntax", "provenance", "must be an object");
        if (!errors.isEmpty()) return errors;
        checkSemantics(record, errors);
        Map<String, Object> provenance = (Map<String, Object>) record.get("provenance");
        for (String field : List.of("vendor", "currency", "stated_total")) {
            Object value = record.get(field);
            if (value == null || "unclear".equals(value)) continue;
            Object quote = provenance.get(field);
            if (!quoteFound(quote, document)) err(errors, "ungrounded", field, field + " has no quote that appears in the document");
        }
        for (String field : required) {
            Object value = record.get(field);
            if (value == null || "unclear".equals(value)) err(errors, "absent", field, "the document gave no value");
        }
        return errors;
    }

    private static List<Object> retryableErrors(List<Map<String, Object>> errors) {
        // TODO 4 of 10 (finish this to pass e2, e3 and e4): keep the errors a second look can fix.
        // Receives a list of {kind, field, message} errors. Returns those whose kind is in RETRYABLE (syntax, semantic, ungrounded), in
        // order; an "absent" error is never retried. Example: [{kind=absent}, {kind=syntax}] -> [{kind=syntax}]
        return new ArrayList<>();
    }

    private static String status(List<Map<String, Object>> errors, Map<String, Object> record) {
        // TODO 5 of 10 (finish this to pass m1, e3 and e5): the status of a finished extraction.
        // Receives the errors left after the last attempt and the last record. Returns "needs_review" when the only errors are "absent"
        // ones, or when there are no errors and the model flagged a conflict ("conflict_detected"); "failed" for any other error; "valid"
        // otherwise. Example: no errors and conflict_detected false -> "valid"; one absent error -> "needs_review"; one syntax error -> "failed"
        return "failed";
    }

    static Map<String, Object> extractDocument(String document, BiFunction<String, Map<String, Object>, Map<String, Object>> callModel, List<String> required, int maxRetries) {
        Map<String, Object> feedback = null;
        int attempts = 0;
        Map<String, Object> record;
        List<Map<String, Object>> errors;
        while (true) {
            attempts++;
            record = callModel.apply(document, feedback);
            errors = validate(record, document, required);
            if (errors.isEmpty()) break;
            List<Object> retryable = retryableErrors(errors);
            if (retryable.isEmpty()) break;
            if (attempts > maxRetries) break;
            feedback = map("previous", record, "errors", retryable);
        }
        String status = status(errors, record);
        return map("status", status, "record", record, "attempts", attempts, "errors", errors);
    }

    private static boolean isUnset(Object value) {
        // TODO 6 of 10 (finish this to pass e7): has a merged field no real value yet?
        // Receives a value. Returns true for null and for "unclear", so the first real value from a later chunk is kept.
        // Example: isUnset(null) -> true, isUnset("unclear") -> true, isUnset("Acme Ltd") -> false
        return false;
    }

    private static boolean isConflict(Object current, Object value, Object field, List<Object> conflicts) {
        // TODO 7 of 10 (finish this to pass e7): do two chunks disagree about a field, not yet recorded?
        // Receives the value kept so far, a later chunk's value, the field name and the fields already in `conflicts`. Returns true when
        // the values differ and the field is not yet in that list, so a conflict is recorded once.
        // Example: isConflict("A", "B", "vendor", List.of()) -> true
        return false;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> mergeChunks(List<Map<String, Object>> records) {
        Map<String, Object> provenance = new LinkedHashMap<>();
        List<Object> lineItems = new ArrayList<>();
        List<Object> conflicts = new ArrayList<>();
        Map<String, Object> merged = map("vendor", null, "currency", "unclear", "currency_detail", null, "line_items", lineItems, "stated_total", null, "calculated_total", 0.0,
                "conflict_detected", false, "provenance", provenance, "conflicts", conflicts);
        for (Map<String, Object> record : records) {
            for (String field : List.of("vendor", "currency", "stated_total")) {
                Object value = record.get(field);
                if (value == null || "unclear".equals(value)) continue;
                Object current = merged.get(field);
                if (isUnset(current)) {
                    merged.put(field, value);
                    provenance.put(field, ((Map<String, Object>) record.get("provenance")).get(field));
                    if (field.equals("currency")) merged.put("currency_detail", record.get("currency_detail"));
                } else if (isConflict(current, value, field, conflicts)) {
                    conflicts.add(field);
                }
            }
            if (record.get("line_items") != null) lineItems.addAll((List<Object>) record.get("line_items"));
            if (Boolean.TRUE.equals(record.get("conflict_detected"))) merged.put("conflict_detected", true);
        }
        double sum = 0;
        for (Object n : lineItems) sum += ((Number) n).doubleValue();
        merged.put("calculated_total", round2(sum));
        if (!conflicts.isEmpty()) merged.put("conflict_detected", true);
        return merged;
    }

    private static Map<String, Object> report(int correct, int valid, int total) {
        // TODO 8 of 10 (finish this to pass e8): the accuracy report, on every document and on the validated ones.
        // Receives the number of correct documents, the number of valid documents and the number of labelled documents. Returns a map with
        // all_documents (correct over total), validated_only (correct over valid), validated (= valid) and total; the two rates are rounded
        // with round2 and are 0.0 when the denominator is zero. Example: report(3, 6, 6) -> all_documents 0.5, validated_only 0.5
        return map("all_documents", 0.0, "validated_only", 0.0, "validated", valid, "total", total);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> accuracy(Map<String, Map<String, Object>> results, Map<String, Map<String, Object>> labels) {
        int valid = 0;
        int correct = 0;
        for (Map.Entry<String, Map<String, Object>> entry : labels.entrySet()) {
            Map<String, Object> result = results.get(entry.getKey());
            boolean isValid = result != null && "valid".equals(result.get("status"));
            if (isValid) valid++;
            if (isValid) {
                Map<String, Object> record = (Map<String, Object>) result.get("record");
                Map<String, Object> label = entry.getValue();
                if (Objects.equals(record.get("vendor"), label.get("vendor")) && ((Number) record.get("stated_total")).doubleValue() == ((Number) label.get("stated_total")).doubleValue()) correct++;
            }
        }
        int total = labels.size();
        return report(correct, valid, total);
    }

    private static Map<String, Object> forcedChoice(List<String> tools, String forced) {
        // TODO 9 of 10 (finish this to pass e9): the tool_choice for a model that accepts a forced choice.
        // Receives the list of tool names and the name to force, or null. Returns a map with tool_choice, strict true and verify_reply false:
        // the forced tool when `forced` is given (type tool, name forced), type any when there are several tools, otherwise the one tool by name.
        // Example: (List.of("a", "b"), null) -> {tool_choice={type=any}, strict=true, verify_reply=false}
        return map("tool_choice", map("type", "auto"), "strict", true, "verify_reply", false);
    }

    static Map<String, Object> requestChoice(String model, List<String> tools, String forced) {
        if (NO_FORCING.contains(model)) return map("tool_choice", map("type", "auto"), "strict", true, "verify_reply", true);
        return forcedChoice(tools, forced);
    }
}
