import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The given JSON helper: parse text into Map, List, String, Long, Double, Boolean or null, and write it back. */
final class Json {
    private Json() {}

    static Object parse(String text) {
        Parser p = new Parser(text);
        Object value = p.value();
        p.skip();
        if (p.i != text.length()) throw new IllegalArgumentException("unexpected data after the JSON value");
        return value;
    }

    static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value);
        return out.toString();
    }

    private static void write(StringBuilder out, Object v) {
        if (v == null) out.append("null");
        else if (v instanceof String s) quote(out, s);
        else if (v instanceof Number || v instanceof Boolean) out.append(v);
        else if (v instanceof Map<?, ?> m) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) out.append(',');
                first = false;
                quote(out, String.valueOf(e.getKey()));
                out.append(':');
                write(out, e.getValue());
            }
            out.append('}');
        } else if (v instanceof List<?> l) {
            out.append('[');
            for (int k = 0; k < l.size(); k++) {
                if (k > 0) out.append(',');
                write(out, l.get(k));
            }
            out.append(']');
        } else throw new IllegalArgumentException("not a JSON value: " + v.getClass());
    }

    private static void quote(StringBuilder out, String s) {
        out.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        final String s;
        int i;

        Parser(String s) {
            this.s = s;
        }

        void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        char peek() {
            skip();
            if (i >= s.length()) throw new IllegalArgumentException("unexpected end of JSON");
            return s.charAt(i);
        }

        void expect(char c) {
            if (peek() != c) throw new IllegalArgumentException("expected '" + c + "' at " + i);
            i++;
        }

        Object value() {
            char c = peek();
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return number();
        }

        Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            expect('{');
            if (peek() == '}') { i++; return m; }
            while (true) {
                String key = string();
                expect(':');
                m.put(key, value());
                if (peek() == ',') { i++; continue; }
                expect('}');
                return m;
            }
        }

        List<Object> array() {
            List<Object> l = new ArrayList<>();
            expect('[');
            if (peek() == ']') { i++; return l; }
            while (true) {
                l.add(value());
                if (peek() == ',') { i++; continue; }
                expect(']');
                return l;
            }
        }

        String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                if (i >= s.length()) throw new IllegalArgumentException("unterminated string");
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c != '\\') { b.append(c); continue; }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r');
                    case 't' -> b.append('\t');
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> b.append(e);
                }
            }
        }

        Object number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (start == i) throw new IllegalArgumentException("unexpected character at " + i);
            String t = s.substring(start, i);
            return t.contains(".") || t.contains("e") || t.contains("E") ? (Object) Double.valueOf(t) : (Object) Long.valueOf(t);
        }
    }
}
