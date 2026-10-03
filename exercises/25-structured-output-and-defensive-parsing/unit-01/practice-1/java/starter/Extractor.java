import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md. JSON values are Map, List, String, Long, Double, Boolean or null. */
final class Extractor {
    private Extractor() {}

    static Object parseJson(String text) {
        // TODO: the JSON value in a reply: a code fence's body, else the span from the first { to the last }. Throw ParseError when there is none.
        return null;
    }

    static List<Map<String, Object>> validate(Map<String, Object> schema, Object value) {
        // TODO: one {path, message} per way value breaks schema; an empty list when it conforms.
        return null;
    }

    static Map<String, Object> extract(Function<List<Map<String, Object>>, Map<String, Object>> ask, String document, Map<String, Object> schema) {
        return extract(ask, document, schema, 3, List.of());
    }

    static Map<String, Object> extract(Function<List<Map<String, Object>>, Map<String, Object>> ask, String document, Map<String, Object> schema,
            int maxAttempts, List<String> evidenceFields) {
        // TODO: ask, parse, validate and, on a problem, re-prompt with the errors, at most maxAttempts calls.
        return null;
    }
}
