import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md. JSON values are Map, List, String, Long, Double, Boolean or null. */
final class Extractor {
    private static final System.Logger LOG = System.getLogger(Extractor.class.getName());
    private Extractor() {}

    /** The index of the first { and of the last } in the text; -1 for a brace that is not there. */
    private static int[] objectSpan(String body) {
        // TODO 1 of 8 (finish this to pass m1, e1 and e3): where the JSON object sits in the text.
        // Receives the text. Returns {first, last}: the index of the first '{' and of the last '}', or -1 for a brace that is not there.
        // Example: objectSpan("Sure! {\"a\": 1} Done.") -> {6, 13}
        return new int[] {-1, -1};
    }

    /** The JSON value in a model reply: the body of a code fence, else the span from the first { to the last }. */
    static Object parseJson(String text) {
        LOG.log(System.Logger.Level.DEBUG, "parseJson input: {0}", text);
        String body = text;
        int fence = text.indexOf("```");
        if (fence != -1) {
            int start = text.indexOf('\n', fence);
            int end = text.indexOf("```", start != -1 ? start : fence + 3);
            if (start != -1 && end != -1) body = text.substring(start + 1, end);
        }
        int[] span = objectSpan(body);
        int first = span[0];
        int last = span[1];
        if (first == -1 || last < first) throw new ParseError("no JSON object found in the reply");
        try {
            return Json.parse(body.substring(first, last + 1));
        } catch (IllegalArgumentException e) {
            throw new ParseError("invalid JSON: " + e.getMessage());
        }
    }

    /** Is this value an integer for the schema? A whole number, or a Double with no fraction; never a Boolean. */
    private static boolean isInteger(Object v) {
        // TODO 2 of 8 (finish this to pass e6): is this value an integer for the schema?
        // Receives any value. Returns true for a Long, Integer, Short or Byte and for a Double with no fraction; false otherwise (2.5, a Boolean).
        // Example: isInteger(2.0) -> true, isInteger(2.5) -> false, isInteger(true) -> false
        return false;
    }

    private static boolean typeMatches(String type, Object v) {
        return switch (type) {
            case "string" -> v instanceof String;
            case "integer" -> isInteger(v);
            case "number" -> v instanceof Number;
            case "boolean" -> v instanceof Boolean;
            case "array" -> v instanceof List;
            case "object" -> v instanceof Map;
            case "null" -> v == null;
            default -> throw new IllegalArgumentException("unknown type " + type);
        };
    }

    private static Map<String, Object> problem(String path, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("path", path);
        m.put("message", message);
        return m;
    }

    static List<Map<String, Object>> validate(Map<String, Object> schema, Object value) {
        return validate(schema, value, "$");
    }

    /** The problems of a number outside minimum and maximum. */
    private static List<Map<String, Object>> rangeErrors(Map<String, Object> schema, Number n, String path) {
        // TODO 3 of 8 (finish this to pass e2): the problems of a number outside minimum and maximum.
        // Receives the schema, a number and its path. Returns problems {path, message}: "must be at least <minimum>" first, then
        // "must be at most <maximum>", each only when the schema has that key and the value breaks it.
        // Example: rangeErrors(Map.of("minimum", 0L), -1, "$.total") -> [{path=$.total, message=must be at least 0}]
        return new ArrayList<>();
    }

    /** The problems of missing required keys. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> requiredErrors(Map<String, Object> schema, Map<?, ?> obj, String path) {
        // TODO 4 of 8 (finish this to pass e2): the problems of missing required keys.
        // Receives the schema, an object and its path. Returns one {path: "<path>.<key>", message: "is required"} per key of the schema's
        // "required" list that the object lacks, in the schema's order.
        // Example: requiredErrors(Map.of("required", List.of("a")), Map.of(), "$") -> [{path=$.a, message=is required}]
        return new ArrayList<>();
    }

    /** The problems of keys the schema does not list, when additionalProperties is false. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extraErrors(Map<String, Object> schema, Map<?, ?> obj, String path) {
        // TODO 5 of 8 (finish this to pass e2): the problems of keys the schema does not list.
        // Receives the schema, an object and its path. When "additionalProperties" is false, returns one
        // {path: "<path>.<key>", message: "is not allowed"} per key that is not in "properties"; otherwise an empty list.
        // Example: schema {additionalProperties: false, properties: {}}, object {x: 1}, path "$" -> [{path=$.x, message=is not allowed}]
        return new ArrayList<>();
    }

    /** One problem {path, message} per way value breaks schema; an empty list when it conforms. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> validate(Map<String, Object> schema, Object value, String path) {
        List<Map<String, Object>> errors = new ArrayList<>();
        Object type = schema.get("type");
        if (type != null && !typeMatches((String) type, value)) {
            errors.add(problem(path, "must be of type " + type));
            return errors;
        }
        if (schema.containsKey("enum") && !((List<Object>) schema.get("enum")).contains(value)) {
            errors.add(problem(path, "must be one of " + Json.stringify(schema.get("enum"))));
        }
        if (value instanceof Number n && !(value instanceof Boolean)) errors.addAll(rangeErrors(schema, n, path));
        if (value instanceof Map<?, ?> obj) {
            errors.addAll(requiredErrors(schema, obj, path));
            Map<String, Object> properties = (Map<String, Object>) schema.getOrDefault("properties", Map.of());
            for (Map.Entry<String, Object> e : properties.entrySet()) {
                if (obj.containsKey(e.getKey())) errors.addAll(validate((Map<String, Object>) e.getValue(), obj.get(e.getKey()), path + "." + e.getKey()));
            }
            errors.addAll(extraErrors(schema, obj, path));
        }
        if (value instanceof List<?> list && schema.containsKey("items")) {
            for (int i = 0; i < list.size(); i++) errors.addAll(validate((Map<String, Object>) schema.get("items"), list.get(i), path + "[" + i + "]"));
        }
        return errors;
    }

    @SuppressWarnings("unchecked")
    private static String textOf(Map<String, Object> reply) {
        StringBuilder out = new StringBuilder();
        for (Object block : (List<Object>) reply.get("content")) {
            Map<String, Object> b = (Map<String, Object>) block;
            if ("text".equals(b.get("type"))) out.append(b.get("text"));
        }
        return out.toString();
    }

    private static Map<String, Object> message(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private static String prompt(String document, Map<String, Object> schema) {
        return "Extract the data from the document as one JSON object that follows this JSON Schema. Reply with the JSON only.\n"
                + "<schema>" + Json.stringify(schema) + "</schema>\n<document>\n" + document + "\n</document>";
    }

    /** The message that sends the problems back to the model. */
    private static String feedback(List<Map<String, Object>> errors) {
        // TODO 6 of 8 (finish this to pass e2): the message that sends the problems back to the model.
        // Receives the list of problems. Returns "Your reply was rejected:", then one "- <path>: <message>" line per problem, then
        // "Return the corrected JSON only.", each on its own line (a newline after every line but the last).
        // Example: one problem {$.total, must be of type number} -> "Your reply was rejected:\n- $.total: must be of type number\nReturn the corrected JSON only."
        return "";
    }

    private static Map<String, Object> result(String status, Object value, int attempts, List<Map<String, Object>> errors) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("value", value);
        m.put("attempts", attempts);
        m.put("errors", errors);
        return m;
    }

    /** The quotes the document does not contain. */
    private static List<Map<String, Object>> groundingErrors(Object value, String document, List<String> evidenceFields) {
        // TODO 7 of 8 (finish this to pass e5): the quotes the document does not contain.
        // Receives the parsed value, the document text and the names of the evidence fields. For each name whose value in the object is a
        // String that does not occur in the document, returns {path: "$.<name>", message: "is not found in the document"}.
        // Example: value {quote: "x"}, document "abc", fields [quote] -> [{path=$.quote, message=is not found in the document}]
        return new ArrayList<>();
    }

    /** "refused" or "truncated" for a reply that must not be retried, else null. */
    private static String earlyStatus(Map<String, Object> reply) {
        // TODO 8 of 8 (finish this to pass e4): a reply that must not be retried.
        // Receives a reply. Returns "refused" when its stop_reason is "refusal", "truncated" when it is "max_tokens", else null.
        // Example: a reply with stop_reason "max_tokens" -> "truncated"
        // The starter says "truncated" for every reply, so it stops after one call; write the real rule.
        return "truncated";
    }

    static Map<String, Object> extract(Function<List<Map<String, Object>>, Map<String, Object>> ask, String document, Map<String, Object> schema) {
        return extract(ask, document, schema, 3, List.of());
    }

    /** Ask, parse, validate and, on a problem, re-prompt with the errors, at most maxAttempts calls. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> extract(Function<List<Map<String, Object>>, Map<String, Object>> ask, String document, Map<String, Object> schema,
            int maxAttempts, List<String> evidenceFields) {
        List<Map<String, Object>> messages = new ArrayList<>(List.of(message("user", prompt(document, schema))));
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, Object> reply = ask.apply(List.copyOf(messages));
            String early = earlyStatus(reply);
            if (early != null) return result(early, Map.of(), attempt, new ArrayList<>());
            String text = textOf(reply);
            Object value = null;
            try {
                value = parseJson(text);
                errors = validate(schema, value);
                errors.addAll(groundingErrors(value, document, evidenceFields));
            } catch (ParseError e) {
                value = null;
                errors = new ArrayList<>(List.of(problem("$", e.getMessage())));
            }
            if (errors.isEmpty()) return result("ok", value, attempt, new ArrayList<>());
            if (attempt < maxAttempts) {
                messages.add(message("assistant", text));
                messages.add(message("user", feedback(errors)));
            }
        }
        return result("failed", null, maxAttempts, errors);
    }
}
