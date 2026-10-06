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
        // TODO 1 of 8 (finish this to pass e1): the rule ids a tool name breaks.
        // Receives the tool's name. Returns a set with "bad-name" when the name does not match NAME in full, and "vague-name" when the
        // name in lower case is in VAGUE. Example: nameRules("Helper") -> [vague-name], nameRules("my tool") -> [bad-name]
        Set<String> found = new TreeSet<>();
        return found;
    }

    private static Set<String> descriptionRules(String description) {
        // TODO 2 of 8 (finish this to pass e2): the rule ids a description breaks.
        // Receives the description text. Returns a set with "short-description" when it has fewer than 3 sentences (a `.`, `!` or `?`
        // followed by white space or the end) and "no-boundary" when the lower-cased text has none of "do not use", "not for",
        // "instead of". Example: descriptionRules("Gets stuff.") -> [no-boundary, short-description]
        Set<String> found = new TreeSet<>();
        return found;
    }

    private static Set<String> parameterRules(Map<String, Object> properties, List<?> required) {
        // TODO 3 of 8 (finish this to pass e3): the rule ids the parameters break.
        // Receives the schema's `properties` (name -> spec map) and the `required` list. Returns a set with "param-undescribed" when a
        // property has no description or a blank one, and "required-unknown" when `required` names something that is not a property.
        // Example: a property q without description and required ["limit"] -> [param-undescribed, required-unknown]
        Set<String> found = new TreeSet<>();
        return found;
    }

    private static Set<String> listAndHintRules(String name, Map<String, Object> properties, Map<String, Object> hints) {
        // TODO 4 of 8 (finish this to pass e4): the rule ids a list tool and its annotations break.
        // Receives the name, the `properties` and the tool's `annotations` map. Returns a set with "list-unbounded" when the name starts
        // with a READ_PREFIXES entry and `properties` lacks "limit" or "cursor", and "hint-contradicts-name" when readOnlyHint is true
        // and the name starts with a WRITE_PREFIXES entry, or destructiveHint is false and it starts with a DELETE_PREFIXES entry.
        // Example: listAndHintRules("list_users", Map.of("limit", Map.of()), Map.of()) -> [list-unbounded]
        Set<String> found = new TreeSet<>();
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
        // TODO 5 of 8 (finish this to pass e5): do two descriptions overlap?
        // Receives two sets of words. Returns true when the share of common words over all the words is at least OVERLAP (0.6), and
        // false when both sets are empty. Example: similar(Set.of("a", "b", "c"), Set.of("a", "b", "c", "d")) -> true (3 of 4),
        // similar(Set.of("a", "b"), Set.of("c", "d")) -> false
        return false;
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
        // TODO 6 of 8 (finish this to pass e6): validate and clamp a page limit.
        // Receives the requested limit. Throws IllegalArgumentException unless it is at least 1; otherwise returns it cut to MAX_LIMIT.
        // Example: checkLimit(500) -> 50, checkLimit(0) throws IllegalArgumentException
        return limit;
    }

    private static boolean overCap(List<String> page, int used, String item, int maxChars) {
        // TODO 7 of 8 (finish this to pass e7): would this item push the page over the size cap?
        // Receives the items already in the page, their total length `used`, the next item and `maxChars`. Returns true when the page is
        // not empty and adding the item would make the total longer than maxChars; the first item is always taken, however long.
        // Example: overCap(List.of("aaaaa"), 5, "bbbbbb", 10) -> true, overCap(List.of(), 0, "b".repeat(99), 10) -> false
        return false;
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
        // TODO 8 of 8 (finish this to pass e8): the four annotation hints a client acts on.
        // `hints` holds the defaults. When the server is trusted, the tool's own Boolean value in its "annotations" map replaces the
        // default for each of the four keys (values that are not Booleans are ignored); an untrusted server keeps the defaults.
        // Example: a tool with readOnlyHint true -> readOnlyHint stays false when untrusted, becomes true when trusted.
        return hints;
    }

    static List<String> parallelSafe(List<Map<String, Object>> tools, Set<String> trustedServers) {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> tool : tools) if (effectiveHints(tool, trustedServers.contains(str(tool.get("server")))).get("readOnlyHint")) names.add(str(tool.get("name")));
        return names;
    }
}
