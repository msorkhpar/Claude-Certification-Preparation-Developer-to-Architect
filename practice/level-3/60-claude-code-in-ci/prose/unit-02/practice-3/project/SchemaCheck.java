import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Provided: the errors of a value against the JSON Schema subset the structured outputs support. Do not edit. */
public final class SchemaCheck {
    private SchemaCheck() {}

    private static boolean isType(JsonNode value, String kind) {
        return switch (kind) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "boolean" -> value.isBoolean();
            case "null" -> value.isNull();
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            default -> throw new IllegalArgumentException(kind);
        };
    }

    /** Errors of a value against the JSON Schema subset the structured outputs support: type, enum, required, properties, items and additionalProperties false. */
    public static List<String> schemaCheck(JsonNode value, JsonNode schema, String path) {
        List<String> wants = new ArrayList<>();
        JsonNode want = schema.get("type");
        if (want != null) {
            if (want.isArray()) want.forEach(w -> wants.add(w.asText()));
            else wants.add(want.asText());
        }
        if (!wants.isEmpty() && wants.stream().noneMatch(k -> isType(value, k))) return List.of(path + ": expected " + String.join(" or ", wants));
        List<String> errors = new ArrayList<>();
        JsonNode allowed = schema.get("enum");
        if (allowed != null) {
            boolean found = false;
            for (JsonNode e : allowed) found |= e.equals(value);
            if (!found) {
                String shown = value.isTextual() ? "'" + value.asText() + "'" : value.toString();
                errors.add(path + ": " + shown + " is not one of [" + java.util.stream.StreamSupport.stream(allowed.spliterator(), false).map(e -> e.isTextual() ? "'" + e.asText() + "'" : e.toString()).collect(Collectors.joining(", ")) + "]");
            }
        }
        if (value.isObject()) {
            JsonNode required = schema.get("required");
            if (required != null) for (JsonNode k : required) if (!value.has(k.asText())) errors.add(path + "." + k.asText() + ": is required");
            JsonNode properties = schema.get("properties");
            JsonNode extra = schema.get("additionalProperties");
            if (extra != null && extra.isBoolean() && !extra.asBoolean()) {
                for (var it = value.fieldNames(); it.hasNext();) {
                    String k = it.next();
                    if (properties == null || !properties.has(k)) errors.add(path + "." + k + ": is not allowed");
                }
            }
            if (properties != null) {
                for (var it = properties.fields(); it.hasNext();) {
                    var sub = it.next();
                    if (value.has(sub.getKey())) errors.addAll(schemaCheck(value.get(sub.getKey()), sub.getValue(), path + "." + sub.getKey()));
                }
            }
        }
        if (value.isArray() && schema.has("items")) {
            for (int i = 0; i < value.size(); i++) errors.addAll(schemaCheck(value.get(i), schema.get("items"), path + "[" + i + "]"));
        }
        return errors;
    }

    public static List<String> schemaCheck(JsonNode value, JsonNode schema) {
        return schemaCheck(value, schema, "$");
    }
}
