import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tool interfaces graded on rules: lint a tool and a tool set, page large results, and weigh a tool's annotations. See ../../statement.md. Tools are JSON-like maps. */
final class Toolset {
    private Toolset() {}

    static List<String> lintTool(Map<String, Object> tool) {
        // TODO: the sorted list of rule ids this tool breaks.
        return null;
    }

    static List<List<String>> lintToolSet(List<Map<String, Object>> tools) {
        return lintToolSet(tools, 20);
    }

    static List<List<String>> lintToolSet(List<Map<String, Object>> tools, int maxTools) {
        // TODO: sorted [tool name, rule] pairs for the whole set.
        return null;
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
        // TODO: a map with items, next_cursor, truncated and note.
        return null;
    }

    static Map<String, Boolean> effectiveHints(Map<String, Object> tool, boolean trustedServer) {
        // TODO: the four hints a client acts on.
        return null;
    }

    static List<String> parallelSafe(List<Map<String, Object>> tools, Set<String> trustedServers) {
        // TODO: names of the tools that may run beside other read-only tools.
        return null;
    }
}
