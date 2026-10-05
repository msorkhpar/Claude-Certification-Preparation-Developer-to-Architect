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
        return new int[] {body.indexOf('{'), body.lastIndexOf('}')};
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
        if (v instanceof Long || v instanceof Integer || v instanceof Short || v instanceof Byte) return true;
        return v instanceof Double d && !d.isInfinite() && !d.isNaN() && d == Math.rint(d);
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
        List<Map<String, Object>> errors = new ArrayList<>();
        if (schema.containsKey("minimum") && n.doubleValue() < ((Number) schema.get("minimum")).doubleValue()) errors.add(problem(path, "must be at least " + schema.get("minimum")));
        if (schema.containsKey("maximum") && n.doubleValue() > ((Number) schema.get("maximum")).doubleValue()) errors.add(problem(path, "must be at most " + schema.get("maximum")));
        return errors;
    }

    /** The problems of missing required keys. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> requiredErrors(Map<String, Object> schema, Map<?, ?> obj, String path) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (Object key : (List<Object>) schema.getOrDefault("required", List.of())) {
            if (!obj.containsKey(key)) errors.add(problem(path + "." + key, "is required"));
        }
        return errors;
    }

    /** The problems of keys the schema does not list, when additionalProperties is false. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extraErrors(Map<String, Object> schema, Map<?, ?> obj, String path) {
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<String, Object> properties = (Map<String, Object>) schema.getOrDefault("properties", Map.of());
        if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            for (Object key : obj.keySet()) if (!properties.containsKey(key)) errors.add(problem(path + "." + key, "is not allowed"));
        }
        return errors;
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
        StringBuilder lines = new StringBuilder();
        for (Map<String, Object> e : errors) lines.append("- ").append(e.get("path")).append(": ").append(e.get("message")).append('\n');
        return "Your reply was rejected:\n" + lines + "Return the corrected JSON only.";
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
        List<Map<String, Object>> errors = new ArrayList<>();
        for (String name : evidenceFields) {
            Object quoted = value instanceof Map<?, ?> m ? m.get(name) : null;
            if (quoted instanceof String s && !document.contains(s)) errors.add(problem("$." + name, "is not found in the document"));
        }
        return errors;
    }

    /** "refused" or "truncated" for a reply that must not be retried, else null. */
    private static String earlyStatus(Map<String, Object> reply) {
        if ("refusal".equals(reply.get("stop_reason"))) return "refused";
        if ("max_tokens".equals(reply.get("stop_reason"))) return "truncated";
        return null;
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
            if (early != null) return result(early, null, attempt, new ArrayList<>());
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
