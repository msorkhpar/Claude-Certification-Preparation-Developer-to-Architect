import static harness.Show.py;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The built-in file tools of Claude Code, modelled offline: Edit's exact match, the way out when it cannot apply, which search tools exist
 * on which platform, and which permission rule covers which tool.
 *
 * <p>A teaching model of the "Tools reference" page of the Claude Code documentation (read on 2026-10-03), not the product's code. It covers six tools:
 * Read, Write, Edit, Bash, Grep and Glob.
 */
public final class BuiltinTools {
    static final List<String> SEARCH_TOOLS = List.of("Grep", "Glob");
    static final List<String> BASE_TOOLS = List.of("Read", "Write", "Edit", "Bash");
    static final Map<String, List<String>> RULE_COVERS = Map.of("Read", List.of("Read", "Grep", "Glob"), "Edit", List.of("Edit", "Write"), "Bash", List.of("Bash")); // a Write(path) rule is never matched
    static final List<String> RULE_ORDER = List.of("Read", "Edit", "Bash");

    /** The result of Edit: whether it applied, the new text and the number replaced, or the error. */
    record EditResult(boolean ok, String text, Integer replaced, String error) {}

    /** What to do for a change: the action and the string to edit with (null when none). */
    record Plan(String action, String anchor) {
        @Override
        public String toString() {
            return "(" + py(action) + ", " + py(anchor) + ")";
        }
    }

    static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) n++;
        return n;
    }

    /** Edit is an exact string replacement: no regex, no fuzzy match. old must be present, and appear once unless replaceAll is set. */
    static EditResult edit(String text, String old, String replacement, boolean replaceAll) {
        int count = count(text, old);
        if (count == 0) return new EditResult(false, null, null, "old_string not found");
        if (count > 1 && !replaceAll) return new EditResult(false, null, null, "old_string appears " + count + " times");
        int first = text.indexOf(old);
        String changed = replaceAll ? text.replace(old, replacement) : text.substring(0, first) + replacement + text.substring(first + old.length());
        return new EditResult(true, changed, replaceAll ? count : 1, null);
    }

    static EditResult edit(String text, String old, String replacement) {
        return edit(text, old, replacement, false);
    }

    /**
     * What to do for a change to `old`: Edit as it is, Edit with a longer unique string that holds it, replace_all for every occurrence,
     * and only when no unique anchor exists, read the file and write it back whole.
     */
    static Plan planEdit(String text, String old, boolean every, List<String> anchors) {
        int count = count(text, old);
        if (count == 0) return new Plan("read_again", null);
        if (count == 1) return new Plan("edit", old);
        if (every) return new Plan("replace_all", old);
        for (String anchor : anchors) if (anchor.contains(old) && count(text, anchor) == 1) return new Plan("edit", anchor);
        return new Plan("read_write", null);
    }

    static Plan planEdit(String text, String old) {
        return planEdit(text, old, false, List.of());
    }

    /**
     * The six tools a session has. Grep and Glob are in the default set on Windows only; elsewhere they return when named in `tools`
     * or `allowedTools` (naming either in allowedTools restores both), or when Bash is removed.
     */
    static List<String> toolSet(String platform, List<String> tools, List<String> allowedTools, List<String> disallowedTools) {
        List<String> have = new ArrayList<>();
        if (tools != null) {
            for (String t : tools) if (BASE_TOOLS.contains(t) || SEARCH_TOOLS.contains(t)) have.add(t);
        } else {
            have.addAll(BASE_TOOLS);
            if (platform.equals("windows") || allowedTools.stream().anyMatch(SEARCH_TOOLS::contains) || disallowedTools.contains("Bash")) have.addAll(SEARCH_TOOLS);
        }
        return have.stream().filter(t -> !disallowedTools.contains(t)).toList();
    }

    static List<String> toolSet(String platform) {
        return toolSet(platform, null, List.of(), List.of());
    }

    /** The tools that a permission rule such as Read(secrets/**) applies to. */
    static List<String> coveredBy(String rule) {
        return RULE_COVERS.getOrDefault(rule.split("\\(", 2)[0], List.of());
    }

    /** The tool name that a permission rule is written under: Read(...) covers Read, Grep and Glob; Edit(...) covers Edit and Write. */
    static String ruleTool(String tool) {
        for (String name : RULE_ORDER) if (RULE_COVERS.get(name).contains(tool)) return name;
        return tool;
    }

    static String join(List<String> items) {
        return String.join(", ", items);
    }

    public static void main(String[] args) {
        String text = "def a():\n    return 1\n\ndef b():\n    return 1\n";
        Object[][] cases = {{"unique", "def a():", false}, {"twice", "    return 1", false}, {"twice, every", "    return 1", true}, {"absent", "def c():", false}};
        for (Object[] c : cases) {
            EditResult r = edit(text, (String) c[1], "X", (Boolean) c[2]);
            System.out.println("edit " + c[0] + ": " + (!r.ok() ? r.error() : "replaced " + r.replaced()));
        }
        List<String> anchors = List.of("def b():\n    return 1");
        System.out.println("plan, unique: " + planEdit(text, "def a():"));
        System.out.println("plan, twice with an anchor: " + planEdit(text, "    return 1", false, anchors));
        System.out.println("plan, twice, every one: " + planEdit(text, "    return 1", true, List.of()));
        System.out.println("plan, twice, no unique anchor: " + planEdit(text, "    return 1", false, List.of("return 1")));
        for (String platform : List.of("linux", "windows")) System.out.println(platform + ", default: " + join(toolSet(platform)));
        System.out.println("linux, allowedTools Grep: " + join(toolSet("linux", null, List.of("Grep"), List.of())));
        System.out.println("linux, tools Read Grep Glob: " + join(toolSet("linux", List.of("Read", "Grep", "Glob"), List.of(), List.of())));
        System.out.println("linux, Bash removed: " + join(toolSet("linux", null, List.of(), List.of("Bash"))));
        List<String> written = new ArrayList<>();
        for (String t : List.of("Grep", "Glob", "Write", "Bash")) written.add(t + " as " + ruleTool(t));
        System.out.println("rules are written under: " + join(written));
        for (String rule : List.of("Read(secrets/**)", "Edit(src/**)", "Write(src/**)", "Bash(git log *)")) {
            List<String> covered = coveredBy(rule);
            System.out.println(rule + " covers: " + (covered.isEmpty() ? "nothing" : join(covered)));
        }
    }
}
