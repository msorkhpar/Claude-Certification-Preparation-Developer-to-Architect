package harness;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Formats a value the way the Python edition of an example prints it (single-quoted strings, True and False, None,
 * `{'key': value}` and `[a, b]`), so the output of every language edition of an example reads the same.
 */
public final class Show {
    private Show() {}

    public static String py(Object v) {
        if (v == null) return "None";
        if (v instanceof JsonNode node) return fromJson(node);
        if (v instanceof Boolean b) return b ? "True" : "False";
        if (v instanceof String s) return quote(s);
        if (v instanceof Map<?, ?> m) return m.entrySet().stream().map(e -> py(e.getKey()) + ": " + py(e.getValue())).collect(Collectors.joining(", ", "{", "}"));
        if (v instanceof Iterable<?> it) return join(it.iterator());
        return String.valueOf(v);
    }

    private static String join(Iterator<?> it) {
        StringBuilder out = new StringBuilder("[");
        while (it.hasNext()) {
            out.append(py(it.next()));
            if (it.hasNext()) out.append(", ");
        }
        return out.append("]").toString();
    }

    private static String fromJson(JsonNode n) {
        if (n.isNull() || n.isMissingNode()) return "None";
        if (n.isTextual()) return quote(n.asText());
        if (n.isBoolean()) return n.asBoolean() ? "True" : "False";
        if (n.isArray()) return join(n.elements());
        if (n.isObject()) {
            StringBuilder out = new StringBuilder("{");
            Iterator<Map.Entry<String, JsonNode>> fields = n.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> f = fields.next();
                out.append(quote(f.getKey())).append(": ").append(fromJson(f.getValue()));
                if (fields.hasNext()) out.append(", ");
            }
            return out.append("}").toString();
        }
        return n.asText();
    }

    /** Python's repr of a string: single quotes, unless the text has a single quote and no double quote. */
    private static String quote(String s) {
        String q = s.contains("'") && !s.contains("\"") ? "\"" : "'";
        return q + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace(q, "\\" + q) + q;
    }

    /** Python's True or False. */
    public static String py(boolean value) {
        return value ? "True" : "False";
    }

    /** A list of strings as Python prints it. */
    public static String py(List<String> items) {
        return py((Object) items);
    }
}
