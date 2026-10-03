import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md. JSON values are Map, List, String, Long, Double, Boolean or null. */
final class Extractor {
    private Extractor() {}

    /** The JSON value in a model reply: the body of a code fence, else the span from the first { to the last }. */
    static Object parseJson(String text) {
        String body = text;
        int fence = text.indexOf("```");
        if (fence != -1) {
            int start = text.indexOf('\n', fence);
            int end = text.indexOf("```", start != -1 ? start : fence + 3);
            if (start != -1 && end != -1) body = text.substring(start + 1, end);
        }
        int first = 0;
        int last = body.length() - 1;
        if (first == -1 || last < first) throw new ParseError("no JSON object found in the reply");
        try {
            return Json.parse(body.substring(first, last + 1));
        } catch (IllegalArgumentException e) {
            throw new ParseError("invalid JSON: " + e.getMessage());
        }
    }

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
        if (value instanceof Number n && !(value instanceof Boolean)) {
            if (schema.containsKey("minimum") && n.doubleValue() < ((Number) schema.get("minimum")).doubleValue()) errors.add(problem(path, "must be at least " + schema.get("minimum")));
            if (schema.containsKey("maximum") && n.doubleValue() > ((Number) schema.get("maximum")).doubleValue()) errors.add(problem(path, "must be at most " + schema.get("maximum")));
        }
        if (value instanceof Map<?, ?> obj) {
            for (Object key : (List<Object>) schema.getOrDefault("required", List.of())) {
                if (!obj.containsKey(key)) errors.add(problem(path + "." + key, "is required"));
            }
            Map<String, Object> properties = (Map<String, Object>) schema.getOrDefault("properties", Map.of());
            for (Map.Entry<String, Object> e : properties.entrySet()) {
                if (obj.containsKey(e.getKey())) errors.addAll(validate((Map<String, Object>) e.getValue(), obj.get(e.getKey()), path + "." + e.getKey()));
            }
            if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
                for (Object key : obj.keySet()) if (!properties.containsKey(key)) errors.add(problem(path + "." + key, "is not allowed"));
            }
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
            if ("refusal".equals(reply.get("stop_reason"))) return result("refused", null, attempt, new ArrayList<>());
            if ("max_tokens".equals(reply.get("stop_reason"))) return result("truncated", null, attempt, new ArrayList<>());
            String text = textOf(reply);
            Object value = null;
            try {
                value = parseJson(text);
                errors = validate(schema, value);
                for (String name : evidenceFields) {
                    Object quoted = value instanceof Map<?, ?> m ? m.get(name) : null;
                    if (quoted instanceof String s && !document.contains(s)) errors.add(problem("$." + name, "is not found in the document"));
                }
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
