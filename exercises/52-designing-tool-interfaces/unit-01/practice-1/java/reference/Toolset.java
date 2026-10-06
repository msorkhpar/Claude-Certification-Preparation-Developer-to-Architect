import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tool interfaces graded on rules: lint a tool and a tool set, page large results, and weigh a tool's annotations. See ../../statement.md. Tools are JSON-like maps. */
final class Toolset {
    private static final System.Logger LOG = System.getLogger(Toolset.class.getName());
    private Toolset() {}

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final Set<String> VAGUE = Set.of("tool", "helper", "do", "run", "process", "handle", "data", "util", "utils", "query");
    private static final List<String> READ_PREFIXES = List.of("list_", "search_", "find_");
    private static final List<String> WRITE_PREFIXES = List.of("create_", "update_", "delete_", "remove_", "send_", "write_");
    private static final List<String> DELETE_PREFIXES = List.of("delete_", "remove_");
    private static final double OVERLAP = 0.6; // token overlap of two descriptions from which the tools count as overlapping
    private static final int MAX_LIMIT = 50;

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> obj(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    private static boolean startsWithAny(String text, List<String> prefixes) {
        return prefixes.stream().anyMatch(text::startsWith);
    }

    private static boolean valid(Object value, Map<String, Object> schema) {
        String kind = str(schema.get("type"));
        if (kind.equals("string") && !(value instanceof String)) return false;
        if (kind.equals("integer") && !(value instanceof Integer || value instanceof Long)) return false;
        if (kind.equals("number") && !(value instanceof Number)) return false;
        if (kind.equals("boolean") && !(value instanceof Boolean)) return false;
        if (kind.equals("array") && !(value instanceof List)) return false;
        if (kind.equals("object") && !(value instanceof Map)) return false;
        return !schema.containsKey("enum") || ((Collection<?>) schema.get("enum")).contains(value);
    }

    private static boolean exampleOk(Object example, Map<String, Object> properties, List<?> required) {
        if (!(example instanceof Map<?, ?> given) || required.stream().anyMatch(name -> !given.containsKey(name))) return false;
        return given.entrySet().stream().allMatch(e -> properties.containsKey(e.getKey()) && valid(e.getValue(), obj(properties.get(e.getKey()))));
    }

    private static Set<String> nameRules(String name) {
        Set<String> found = new TreeSet<>();
        if (!NAME.matcher(name).matches()) found.add("bad-name");
        if (VAGUE.contains(name.toLowerCase(Locale.ROOT))) found.add("vague-name");
        return found;
    }

    private static Set<String> descriptionRules(String description) {
        Set<String> found = new TreeSet<>();
        String low = description.toLowerCase(Locale.ROOT);
        Matcher sentences = Pattern.compile("[.!?](?:\\s|$)").matcher(description);
        int count = 0;
        while (sentences.find()) count++;
        if (count < 3) found.add("short-description");
        if (List.of("do not use", "not for", "instead of").stream().noneMatch(low::contains)) found.add("no-boundary");
        return found;
    }

    private static Set<String> parameterRules(Map<String, Object> properties, List<?> required) {
        Set<String> found = new TreeSet<>();
        List<Map<String, Object>> specs = properties.values().stream().map(Toolset::obj).toList();
        if (specs.stream().anyMatch(spec -> str(spec.get("description")).isBlank())) found.add("param-undescribed");
        if (required.stream().anyMatch(item -> !properties.containsKey(item))) found.add("required-unknown");
        return found;
    }

    private static Set<String> listAndHintRules(String name, Map<String, Object> properties, Map<String, Object> hints) {
        Set<String> found = new TreeSet<>();
        if (startsWithAny(name, READ_PREFIXES) && !(properties.containsKey("limit") && properties.containsKey("cursor"))) found.add("list-unbounded");
        if ((Boolean.TRUE.equals(hints.get("readOnlyHint")) && startsWithAny(name, WRITE_PREFIXES)) || (Boolean.FALSE.equals(hints.get("destructiveHint")) && startsWithAny(name, DELETE_PREFIXES))) found.add("hint-contradicts-name");
        return found;
    }

    static List<String> lintTool(Map<String, Object> tool) {
        LOG.log(System.Logger.Level.DEBUG, "lintTool input: {0}", tool);
        Set<String> found = new TreeSet<>();
        String name = str(tool.get("name"));
        String description = str(tool.get("description"));
        Map<String, Object> schema = obj(tool.get("input_schema"));
        Map<String, Object> properties = obj(schema.get("properties"));
        List<?> required = schema.get("required") instanceof List<?> r ? r : List.of();
        List<Map<String, Object>> specs = properties.values().stream().map(Toolset::obj).toList();
        found.addAll(nameRules(name));
        found.addAll(descriptionRules(description));
        if (!description.toLowerCase(Locale.ROOT).contains("use when")) found.add("no-use-when");
        found.addAll(parameterRules(properties, required));
        if (specs.stream().anyMatch(spec -> str(spec.get("type")).equals("string") && !spec.containsKey("enum") && Pattern.compile("one of|either").matcher(str(spec.get("description")).toLowerCase(Locale.ROOT)).find())) found.add("open-set");
        if (properties.entrySet().stream().anyMatch(e -> Pattern.compile("reasoning|thinking").matcher((e.getKey() + " " + str(obj(e.getValue()).get("description"))).toLowerCase(Locale.ROOT)).find())) found.add("reasoning-param");
        if (tool.get("input_examples") instanceof List<?> examples && examples.stream().anyMatch(example -> !exampleOk(example, properties, required))) found.add("bad-example");
        found.addAll(listAndHintRules(name, properties, obj(tool.get("annotations"))));
        return new ArrayList<>(found);
    }

    private static Set<String> words(Object text) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = Pattern.compile("[a-z]{3,}").matcher(str(text).toLowerCase(Locale.ROOT));
        while (m.find()) out.add(m.group());
        return out;
    }

    private static boolean similar(Set<String> wa, Set<String> wb) {
        Set<String> union = new LinkedHashSet<>(wa);
        union.addAll(wb);
        long shared = wa.stream().filter(wb::contains).count();
        return !union.isEmpty() && (double) shared / union.size() >= OVERLAP;
    }

    static List<List<String>> lintToolSet(List<Map<String, Object>> tools) {
        return lintToolSet(tools, 20);
    }

    static List<List<String>> lintToolSet(List<Map<String, Object>> tools, int maxTools) {
        Set<List<String>> found = new LinkedHashSet<>();
        for (Map<String, Object> tool : tools) for (String rule : lintTool(tool)) found.add(List.of(str(tool.get("name")), rule));
        List<String> names = tools.stream().map(t -> str(t.get("name"))).toList();
        for (String name : new LinkedHashSet<>(names)) if (names.stream().filter(name::equals).count() > 1) found.add(List.of(name, "duplicate-name"));
        for (int i = 0; i < tools.size(); i++) {
            for (int j = i + 1; j < tools.size(); j++) {
                Map<String, Object> a = tools.get(i), b = tools.get(j);
                Set<String> wa = words(a.get("description")), wb = words(b.get("description"));
                if (!str(a.get("name")).equals(str(b.get("name"))) && similar(wa, wb)) {
                    found.add(List.of(str(a.get("name")), "overlap:" + str(b.get("name"))));
                    found.add(List.of(str(b.get("name")), "overlap:" + str(a.get("name"))));
                }
            }
        }
        if (tools.size() > maxTools) found.add(List.of("*", "too-many-tools"));
        List<List<String>> sorted = new ArrayList<>(found);
        sorted.sort(Comparator.<List<String>, String>comparing(p -> p.get(0)).thenComparing(p -> p.get(1)));
        return sorted;
    }

    private static String encode(int offset) {
        return Base64.getEncoder().encodeToString(("offset:" + offset).getBytes(StandardCharsets.UTF_8));
    }

    private static int decode(String cursor, int total) {
        int offset = -1;
        try {
            String text = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (text.matches("offset:\\d{1,9}")) offset = Integer.parseInt(text.substring(7));
        } catch (IllegalArgumentException error) {
            offset = -1;
        }
        if (offset < 0 || offset > total) throw new IllegalArgumentException("invalid cursor");
        return offset;
    }

    private static int checkLimit(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be a whole number of at least 1");
        limit = Math.min(limit, MAX_LIMIT);
        return limit;
    }

    private static boolean overCap(List<String> page, int used, String item, int maxChars) {
        return !page.isEmpty() && used + item.length() > maxChars;
    }

    static Map<String, Object> pageResults(List<String> items) {
        return pageResults(items, null, 10, 2000);
    }

    static Map<String, Object> pageResults(List<String> items, String cursor) {
        return pageResults(items, cursor, 10, 2000);
    }

    static Map<String, Object> pageResults(List<String> items, String cursor, int limit) {
        return pageResults(items, cursor, limit, 2000);
    }

    static Map<String, Object> pageResults(List<String> items, String cursor, int limit, int maxChars) {
        limit = checkLimit(limit);
        int offset = cursor == null ? 0 : decode(cursor, items.size());
        List<String> page = new ArrayList<>();
        int used = 0;
        for (String item : items.subList(offset, Math.min(items.size(), offset + limit))) {
            if (overCap(page, used, item, maxChars)) break;
            page.add(item);
            used += item.length();
        }
        int taken = offset + page.size();
        String nextCursor = taken < items.size() ? encode(taken) : null;
        String note = nextCursor != null ? "Showing " + page.size() + " of " + items.size() + " results; pass next_cursor to continue, or narrow the query with a filter." : null;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", page);
        result.put("next_cursor", nextCursor);
        result.put("truncated", page.size() < Math.min(limit, items.size() - offset));
        result.put("note", note);
        return result;
    }

    static Map<String, Boolean> effectiveHints(Map<String, Object> tool, boolean trustedServer) {
        Map<String, Boolean> hints = new LinkedHashMap<>();
        hints.put("readOnlyHint", false);
        hints.put("destructiveHint", true);
        hints.put("idempotentHint", false);
        hints.put("openWorldHint", true);
        if (trustedServer) {
            Map<String, Object> own = obj(tool.get("annotations"));
            for (String key : new ArrayList<>(hints.keySet())) if (own.get(key) instanceof Boolean value) hints.put(key, value);
        }
        return hints;
    }

    static List<String> parallelSafe(List<Map<String, Object>> tools, Set<String> trustedServers) {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> tool : tools) if (effectiveHints(tool, trustedServers.contains(str(tool.get("server")))).get("readOnlyHint")) names.add(str(tool.get("name")));
        return names;
    }
}
