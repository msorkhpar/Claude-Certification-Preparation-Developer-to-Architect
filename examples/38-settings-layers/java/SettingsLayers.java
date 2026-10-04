import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Claude Code settings layers, permission rules and memory files, resolved offline.
 *
 * <p>Three small functions that follow what the Claude Code documentation says (read on 2026-10-03): settings merge from five levels with
 * the highest level winning a scalar key and lists combining; permission rules are checked deny, then ask, then allow, with the first
 * match deciding; and CLAUDE.md files are concatenated from the broadest scope to the most specific, with @imports expanded. It is a
 * teaching model of the documented behaviour, not the product's code: Read and Edit patterns use a reduced form of the gitignore rules.
 * The settings files are JSON, read with Jackson into maps.
 */
public final class SettingsLayers {
    static final List<String> LEVELS = List.of("managed", "command line", "local", "project", "user"); // highest precedence first
    static final List<String> REPO_LEVELS = List.of("project", "local"); // files that live in the repository
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A JSON object (a settings file) as a map. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> json(String text) {
        try {
            return JSON.readValue(text, LinkedHashMap.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) {
        return (List<Object>) o;
    }

    static Map<String, Object> merge(Map<String, Object> low, Map<String, Object> high) {
        Map<String, Object> out = new LinkedHashMap<>(low);
        for (Map.Entry<String, Object> e : high.entrySet()) {
            Object value = e.getValue();
            Object current = out.get(e.getKey());
            if (value instanceof Map<?, ?> && current instanceof Map<?, ?>) {
                out.put(e.getKey(), merge(map(current), map(value)));
            } else if (value instanceof List<?> && current instanceof List<?>) {
                List<Object> combined = new ArrayList<>(list(current));
                for (Object v : list(value)) if (!list(current).contains(v)) combined.add(v);
                out.put(e.getKey(), combined);
            } else {
                out.put(e.getKey(), value);
            }
        }
        return out;
    }

    /** Merge the five levels. A repository file cannot set defaultMode auto or bypassPermissions, and its allow rules wait for trust. */
    static Map<String, Object> effectiveSettings(Map<String, Map<String, Object>> layers, boolean trusted) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = LEVELS.size() - 1; i >= 0; i--) {
            String level = LEVELS.get(i);
            Map<String, Object> part = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : layers.getOrDefault(level, Map.of()).entrySet()) {
                part.put(e.getKey(), e.getValue() instanceof Map<?, ?> m ? new LinkedHashMap<>(map(m)) : e.getValue());
            }
            Map<String, Object> perms = part.get("permissions") == null ? null : map(part.get("permissions"));
            if (REPO_LEVELS.contains(level) && perms != null && !perms.isEmpty()) {
                if ("auto".equals(perms.get("defaultMode")) || "bypassPermissions".equals(perms.get("defaultMode"))) perms.remove("defaultMode");
                if (!trusted) perms.remove("allow");
            }
            result = merge(result, part);
        }
        return result;
    }

    private static Pattern bashRegex(String ruleText) {
        String rule = ruleText.endsWith(":*") ? ruleText.substring(0, ruleText.length() - 2) + " *" : ruleText;
        if (rule.endsWith(" *") && !rule.substring(0, rule.length() - 2).contains("*")) {
            return Pattern.compile(Pattern.quote(rule.substring(0, rule.length() - 2)) + "(?: .*)?", Pattern.DOTALL);
        }
        return Pattern.compile(Arrays.stream(rule.split("\\*", -1)).map(Pattern::quote).collect(Collectors.joining(".*")), Pattern.DOTALL);
    }

    private static String glob(String p) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < p.length()) {
            if (p.startsWith("**/", i)) {
                out.append("(?:.*/)?");
                i += 3;
            } else if (p.startsWith("**", i)) {
                out.append(".*");
                i += 2;
            } else if (p.charAt(i) == '*') {
                out.append("[^/]*");
                i += 1;
            } else {
                out.append(Pattern.quote(String.valueOf(p.charAt(i))));
                i += 1;
            }
        }
        return out.toString();
    }

    private static String withoutDotSlash(String s) {
        return s.startsWith("./") ? s.substring(2) : s;
    }

    private static boolean pathMatches(String patternText, String pathText, String kind) {
        String pattern = withoutDotSlash(patternText);
        String path = withoutDotSlash(pathText);
        if (pattern.startsWith("/")) return path.matches(glob(pattern.substring(1))); // "//x" and "/x" both drop one slash
        if (!pattern.contains("/")) return path.matches("(?:.*/)?" + glob(pattern));
        String first = pattern.split("/")[0];
        long slashes = pattern.chars().filter(c -> c == '/').count();
        boolean deep = !kind.equals("allow") && !first.contains("*") && slashes >= 1 && !pattern.startsWith("**");
        return path.matches((deep ? "(?:.*/)?" : "") + glob(pattern));
    }

    private static final Pattern RULE = Pattern.compile("(\\w+)(?:\\((.*)\\))?", Pattern.DOTALL);

    private static boolean ruleMatches(String rule, String tool, String arg, String kind) {
        Matcher m = RULE.matcher(rule);
        if (!m.matches() || !m.group(1).equals(tool)) return false;
        if (m.group(2) == null) return true;
        return tool.equals("Bash") ? bashRegex(m.group(2)).matcher(arg).matches() : pathMatches(m.group(2), arg, kind);
    }

    /**
     * allow, ask or deny. Deny first, then ask, then allow; a Bash call is split at && || ; | & and newlines and every part must pass.
     * In acceptEdits mode an edit that no rule decided is accepted instead of asked.
     */
    static String decide(Map<String, Object> settings, String tool, String arg, String mode) {
        Map<String, Object> perms = settings.get("permissions") == null ? Map.of() : map(settings.get("permissions"));
        Map<String, List<String>> rules = new LinkedHashMap<>();
        for (String kind : List.of("deny", "ask", "allow")) {
            rules.put(kind, list(perms.getOrDefault(kind, List.of())).stream().map(String.class::cast).toList());
        }
        List<String> parts = tool.equals("Bash")
            ? Arrays.stream(arg.split("&&|\\|\\||;|\\||&|\n")).map(String::strip).filter(p -> !p.isEmpty()).toList()
            : List.of(arg);
        for (String kind : List.of("deny", "ask")) {
            if (rules.get(kind).stream().anyMatch(r -> parts.stream().anyMatch(p -> ruleMatches(r, tool, p, kind)))) return kind;
        }
        if ((tool.equals("Edit") || tool.equals("Write"))
            && rules.get("deny").stream().filter(r -> r.startsWith("Read(")).anyMatch(r -> ruleMatches("Edit(" + r.substring(5), "Edit", arg, "deny"))) {
            return "deny"; // a Read deny rule also blocks edits and writes on the same path
        }
        if (parts.stream().allMatch(p -> rules.get("allow").stream().anyMatch(r -> ruleMatches(r, tool, p, "allow")))) return "allow";
        if (List.of("Read", "Grep", "Glob").contains(tool)) return "allow"; // read-only tools inside the working directory need no approval
        if (mode.equals("acceptEdits") && (tool.equals("Edit") || tool.equals("Write"))) return "allow";
        return "ask"; // nothing matched: Manual mode asks
    }

    static String decide(Map<String, Object> settings, String tool, String arg) {
        return decide(settings, tool, arg, "default");
    }

    /**
     * The memory files in the order they reach the context: managed, user, then each directory from the root down to cwd, where
     * CLAUDE.md comes before CLAUDE.local.md. files maps a path to its text; @path lines import files (relative to the importing file,
     * at most four hops deep) and a path inside backticks is not an import.
     */
    static List<String> loadMemory(Map<String, String> files, String cwd, String managed, String user) {
        List<String> order = new ArrayList<>();
        for (String p : new String[] {managed, user}) if (p != null && files.containsKey(p)) order.add(p);
        String[] parts = cwd.replaceAll("^/+|/+$", "").split("/");
        for (int depth = 0; depth <= parts.length; depth++) {
            String base = ("/" + String.join("/", Arrays.copyOfRange(parts, 0, depth))).replaceAll("/+$", "");
            for (String p : new String[] {base + "/CLAUDE.md", base + "/CLAUDE.local.md"}) if (files.containsKey(p)) order.add(p);
        }
        List<String> loaded = new ArrayList<>();
        for (String path : order) add(files, loaded, path, 0);
        return loaded;
    }

    private static final Pattern IMPORT = Pattern.compile("(?<![`\\w])@([\\w./-]+)");

    private static void add(Map<String, String> files, List<String> loaded, String path, int hops) {
        loaded.add(path);
        if (hops == 4) return;
        Matcher m = IMPORT.matcher(files.get(path).replaceAll("`[^`]*`", ""));
        while (m.find()) {
            String token = m.group(1);
            String target = token.startsWith("/") ? token : join(path, token);
            if (files.containsKey(target)) add(files, loaded, target, hops + 1);
        }
    }

    private static String join(String path, String rel) {
        List<String> stack = new ArrayList<>(Arrays.asList(path.split("/")));
        stack.remove(stack.size() - 1);
        for (String part : rel.split("/")) {
            if (part.equals("..")) stack.remove(stack.size() - 1);
            else if (!part.equals(".")) stack.add(part);
        }
        return String.join("/", stack);
    }

    /** Python's repr of strings and lists of strings, so every language of the course prints the same text. */
    static String py(Object v) {
        if (v instanceof List<?> l) return l.stream().map(SettingsLayers::py).collect(Collectors.joining(", ", "[", "]"));
        String s = String.valueOf(v);
        String q = s.contains("'") && !s.contains("\"") ? "\"" : "'";
        return q + s.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q;
    }

    public static void main(String[] args) {
        Map<String, Map<String, Object>> layers = new LinkedHashMap<>();
        layers.put("managed", json("{\"permissions\": {\"deny\": [\"Bash(curl *)\"]}}"));
        layers.put("user", json("{\"model\": \"sonnet\", \"permissions\": {\"allow\": [\"Bash(git status *)\"]}}"));
        layers.put("project", json("""
            {"model": "opus", "permissions": {"defaultMode": "bypassPermissions", "allow": ["Bash(npm run *)", "Bash(curl *)"],
                                              "ask": ["Bash(git push *)"], "deny": ["Read(./.env)"]}}"""));
        layers.put("local", json("{\"permissions\": {\"allow\": [\"Bash(git push *)\"]}}"));
        Map<String, Object> s = effectiveSettings(layers, true);
        System.out.println("model: " + s.get("model") + " | defaultMode: " + map(s.get("permissions")).getOrDefault("defaultMode", "(not set)"));
        System.out.println("allow rules: " + py(map(s.get("permissions")).get("allow")));
        String[][] calls = {{"Bash", "npm run build"}, {"Bash", "npm run build && git push origin main"}, {"Bash", "curl https://example.com"},
            {"Read", "./.env"}, {"Edit", ".env"}, {"Bash", "git status"}, {"Bash", "rm -rf build"}};
        for (String[] call : calls) System.out.println(call[0] + "(" + call[1] + ") -> " + decide(s, call[0], call[1]));
        System.out.println("project not trusted yet: npm run build -> " + decide(effectiveSettings(layers, false), "Bash", "npm run build"));
        Map<String, String> files = new LinkedHashMap<>();
        files.put("/etc/claude-code/CLAUDE.md", "managed");
        files.put("/home/dev/.claude/CLAUDE.md", "user");
        files.put("/repo/CLAUDE.md", "See @docs/git.md and `@README` here");
        files.put("/repo/docs/git.md", "git rules");
        files.put("/repo/svc/CLAUDE.md", "service");
        files.put("/repo/svc/CLAUDE.local.md", "mine");
        files.put("/repo/other/CLAUDE.md", "other");
        System.out.println("memory order for /repo/svc: " + py(loadMemory(files, "/repo/svc", "/etc/claude-code/CLAUDE.md", "/home/dev/.claude/CLAUDE.md")));
    }
}
