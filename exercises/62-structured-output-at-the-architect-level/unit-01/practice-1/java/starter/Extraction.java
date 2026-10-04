import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md. Results are JSON-like maps. */
final class Extraction {
    private Extraction() {}

    static final List<String> CURRENCIES = List.of("USD", "EUR", "GBP", "other", "unclear");

    /** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
    static final java.util.Set<String> NO_FORCING = java.util.Set.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1");

    static List<Map<String, Object>> validate(Map<String, Object> record, String document) {
        return validate(record, document, List.of());
    }

    static List<Map<String, Object>> validate(Map<String, Object> record, String document, List<String> required) {
        // TODO: the errors of a record, each a map with kind, field and message: syntax, semantic, ungrounded and absent.
        return null;
    }

    static Map<String, Object> extractDocument(String document, BiFunction<String, Map<String, Object>, Map<String, Object>> callModel, List<String> required, int maxRetries) {
        // TODO: call the model, validate, retry with feedback only for errors a second look can fix, and report a status.
        return null;
    }

    static Map<String, Object> mergeChunks(List<Map<String, Object>> records) {
        // TODO: merge the records of the chunks of one long document, keeping the first value and recording a conflict.
        return null;
    }

    static Map<String, Object> accuracy(Map<String, Map<String, Object>> results, Map<String, Map<String, Object>> labels) {
        // TODO: the share of documents extracted correctly, measured on all of them and on the validated ones only.
        return null;
    }

    static Map<String, Object> requestChoice(String model, List<String> tools, String forced) {
        // TODO: the tool_choice of the request, with the fallback for models that reject a forced choice.
        return null;
    }
}
