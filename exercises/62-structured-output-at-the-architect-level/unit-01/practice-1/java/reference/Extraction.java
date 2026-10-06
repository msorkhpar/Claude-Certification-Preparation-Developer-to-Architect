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
        return quote instanceof String q && !q.isEmpty() && document.contains(q);
    }

    private static boolean currencyOk(Object currency) {
        return CURRENCIES.contains(currency);
    }

    private static boolean detailMissing(Object currency, Object detail) {
        return "other".equals(currency) && !(detail instanceof String d && !d.isBlank());
    }

    @SuppressWarnings("unchecked")
    private static void checkSemantics(Map<String, Object> record, List<Map<String, Object>> errors) {
        Object items = record.get("line_items");
        double sum = 0;
        for (Object n : (List<Object>) items) sum += ((Number) n).doubleValue();
        double calculated = ((Number) record.get("calculated_total")).doubleValue();
        if (Math.abs(sum - calculated) > 0.005) err(errors, "semantic", "calculated_total", calculated + " is not the sum of the line items, " + sum);
        if (record.get("stated_total") != null) {
            double stated = ((Number) record.get("stated_total")).doubleValue();
            if (Math.abs(stated - calculated) > 0.005 && !((Boolean) record.get("conflict_detected"))) {
                err(errors, "semantic", "stated_total", "the stated total " + stated + " differs from the calculated total " + calculated + " but conflict_detected is false");
            }
        }
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
        List<Object> retryable = new ArrayList<>();
        for (Map<String, Object> e : errors) if (RETRYABLE.contains(e.get("kind"))) retryable.add(e);
        return retryable;
    }

    private static String status(List<Map<String, Object>> errors, Map<String, Object> record) {
        String status;
        if (!errors.isEmpty()) status = errors.stream().allMatch(e -> "absent".equals(e.get("kind"))) ? "needs_review" : "failed";
        else if (Boolean.TRUE.equals(record.get("conflict_detected"))) status = "needs_review";
        else status = "valid";
        return status;
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
        return value == null || "unclear".equals(value);
    }

    private static boolean isConflict(Object current, Object value, Object field, List<Object> conflicts) {
        return !current.equals(value) && !conflicts.contains(field);
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
        return map("all_documents", total > 0 ? round2((double) correct / total) : 0.0, "validated_only", valid > 0 ? round2((double) correct / valid) : 0.0, "validated", valid, "total", total);
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
        if (forced != null) return map("tool_choice", map("type", "tool", "name", forced), "strict", true, "verify_reply", false);
        if (tools.size() > 1) return map("tool_choice", map("type", "any"), "strict", true, "verify_reply", false);
        return map("tool_choice", map("type", "tool", "name", tools.get(0)), "strict", true, "verify_reply", false);
    }

    static Map<String, Object> requestChoice(String model, List<String> tools, String forced) {
        if (NO_FORCING.contains(model)) return map("tool_choice", map("type", "auto"), "strict", true, "verify_reply", true);
        return forcedChoice(tools, forced);
    }
}
