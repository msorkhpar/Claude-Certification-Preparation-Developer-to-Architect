import static harness.Show.py;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which instruction files are in Claude Code's context, and when: the launch set, the files that load on demand, path-scoped rules, imports and AGENTS.md.
 *
 * <p>The model follows the memory documentation read on 2026-10-03 (Claude Code v2.1.286): files in the directories above the working directory load at
 * launch, root first; files below it load when Claude reads there; a rule with `paths` loads when a matching file is read, written or edited; an
 * import expands at launch to at most four hops; AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above it.
 * Nothing here starts Claude Code: the "project" is a list of file paths and a map of file texts.
 */
public final class MemoryLoading {
    private static final System.Logger LOG = System.getLogger(MemoryLoading.class.getName());
    static final int MAX_IMPORT_HOPS = 4;
    static final Pattern IMPORT = Pattern.compile("(?<![\\w`])@([\\w./-]+)");
    private static final Pattern CODE = Pattern.compile("```.*?```|`[^`]*`", Pattern.DOTALL);
    private static final Pattern BRACES = Pattern.compile("\\{([^{}]*)\\}");

    static List<String> expandBraces(String pattern) {
        Matcher match = BRACES.matcher(pattern);
        if (!match.find()) return List.of(pattern);
        List<String> out = new ArrayList<>();
        for (String option : match.group(1).split(",", -1)) out.addAll(expandBraces(pattern.substring(0, match.start()) + option + pattern.substring(match.end())));
        return out;
    }

    static Pattern globRegex(String pattern) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            if (pattern.startsWith("**/", i)) { out.append("(?:.*/)?"); i += 3; }
            else if (pattern.startsWith("**", i)) { out.append(".*"); i += 2; }
            else if (pattern.charAt(i) == '*') { out.append("[^/]*"); i += 1; }
            else if (pattern.charAt(i) == '?') { out.append("[^/]"); i += 1; }
            else { out.append(Pattern.quote(String.valueOf(pattern.charAt(i)))); i += 1; }
        }
        return Pattern.compile(out.toString());
    }

    /** `*` stays inside one folder, a `**` folder segment crosses folders, braces expand: `*.md` is the project root only, any-folder-prefix plus `*.ts` is every folder. */
    static boolean globMatch(String pattern, String path) {
        return expandBraces(pattern).stream().anyMatch(p -> globRegex(p).matcher(path).matches());
    }

    /** Rule files in context after Claude read or edited the `touched` files. A rule with no `paths` is always loaded. */
    static List<String> rulesLoaded(Map<String, List<String>> rules, List<String> touched) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> r : rules.entrySet()) {
            if (r.getValue() == null || r.getValue().stream().anyMatch(p -> touched.stream().anyMatch(f -> globMatch(p, f)))) out.add(r.getKey());
        }
        return out;
    }

    private static List<String> segments(String path) {
        return Arrays.stream(path.split("/")).filter(p -> !p.isEmpty()).toList();
    }

    /** Memory files loaded when a session starts in `cwd`: the directories from the root down to cwd, CLAUDE.md then CLAUDE.local.md in each. */
    static List<String> launchFiles(Set<String> tree, String cwd) {
        List<String> parts = segments(cwd);
        List<String> folders = new ArrayList<>(List.of(""));
        for (int i = 0; i < parts.size(); i++) folders.add(String.join("/", parts.subList(0, i + 1)));
        List<String> found = new ArrayList<>();
        for (String folder : folders) {
            for (String name : List.of("CLAUDE.md", ".claude/CLAUDE.md", "CLAUDE.local.md")) {
                String path = (folder + "/" + name).replaceFirst("^/+", "");
                if (tree.contains(path)) found.add(path);
            }
        }
        return found;
    }

    /** Memory files below cwd that join the context when Claude reads a file in their folder (or below it), nearest to cwd first. */
    static List<String> onDemandFiles(Set<String> tree, String cwd, List<String> touched) {
        List<String> found = new ArrayList<>();
        for (String file : touched) {
            String[] all = file.split("/");
            List<String> parts = Arrays.asList(all).subList(0, all.length - 1);
            for (int i = segments(cwd).size() + 1; i <= parts.size(); i++) {
                for (String name : List.of("CLAUDE.md", "CLAUDE.local.md")) {
                    List<String> p = new ArrayList<>(parts.subList(0, i));
                    p.add(name);
                    String path = String.join("/", p);
                    if (tree.contains(path) && !found.contains(path)) found.add(path);
                }
            }
        }
        return found;
    }

    /** AGENTS.md is read only when no CLAUDE.md or CLAUDE.local.md is found in the working directory or above it. */
    static boolean agentsMdRead(Set<String> tree, String cwd) {
        return launchFiles(tree, cwd).isEmpty() && tree.contains("AGENTS.md");
    }

    static boolean agentsMdRead(Set<String> tree) {
        return agentsMdRead(tree, "");
    }

    private static String target(String file, String ref) {
        String base = file.contains("/") ? file.substring(0, file.lastIndexOf('/')) : "";
        return String.join("/", segments(base + "/" + ref));
    }

    private static void visit(String file, int left, Map<String, String> texts, List<String> found) {
        String text = CODE.matcher(texts.getOrDefault(file, "")).replaceAll("");
        Matcher m = IMPORT.matcher(text);
        while (m.find()) {
            String target = target(file, m.group(1));
            if (texts.containsKey(target) && !found.contains(target)) {
                found.add(target);
                if (left > 1) visit(target, left - 1, texts, found);
            }
        }
    }

    /** Files pulled in by @path imports, in load order, relative to the importing file, at most `hops` deep; code spans and fences are skipped. */
    static List<String> importsOf(String path, Map<String, String> texts, int hops) {
        List<String> found = new ArrayList<>();
        visit(path, hops, texts, found);
        return found;
    }

    static List<String> importsOf(String path, Map<String, String> texts) {
        return importsOf(path, texts, MAX_IMPORT_HOPS);
    }

    /** The @path references of a file that name no file: a typo here silently imports nothing. */
    static List<String> unresolvedImports(String path, Map<String, String> texts) {
        String text = CODE.matcher(texts.getOrDefault(path, "")).replaceAll("");
        List<String> out = new ArrayList<>();
        Matcher m = IMPORT.matcher(text);
        while (m.find()) if (!texts.containsKey(target(path, m.group(1)))) out.add(m.group(1));
        return out;
    }

    /** Lines the files put into context. An import does not save any: the imported file loads at launch too. */
    static long contextLines(List<String> paths, Map<String, String> texts) {
        return paths.stream().mapToLong(p -> texts.get(p).lines().count()).sum();
    }

    static Map<String, String> texts(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    public static void main(String[] args) {
        Set<String> tree = new LinkedHashSet<>(List.of("CLAUDE.md", "CLAUDE.local.md", "AGENTS.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md"));
        Map<String, List<String>> rules = new LinkedHashMap<>();
        rules.put("commit.md", null);
        rules.put("testing.md", List.of("**/*.test.{ts,tsx}"));
        rules.put("terraform.md", List.of("terraform/**/*"));
        System.out.println("launch in web/: " + py(launchFiles(tree, "web")));
        System.out.println("reading web/ui/Button.tsx adds: " + py(onDemandFiles(tree, "web", List.of("web/ui/Button.tsx"))));
        System.out.println("AGENTS.md read: " + py(agentsMdRead(tree)) + " - without any CLAUDE.md: " + py(agentsMdRead(Set.of("AGENTS.md"))));
        for (String touched : List.of("web/ui/Button.test.tsx", "terraform/main.tf", "README.md")) {
            System.out.println("touching " + touched + ": " + py(rulesLoaded(rules, List.of(touched))));
        }
        String[][] globs = {{"*.md", "README.md"}, {"*.md", "docs/guide.md"}, {"**/*.ts", "a/b/c.ts"}, {"src/**/*.{ts,tsx}", "src/ui/x.tsx"}};
        for (String[] g : globs) System.out.println(g[0] + " matches " + g[1] + ": " + py(globMatch(g[0], g[1])));
        Map<String, String> texts = texts("CLAUDE.md", "See @docs/a.md and `@not-an-import`", "docs/a.md", "@b.md", "docs/b.md", "@c.md", "docs/c.md", "@d.md", "docs/d.md", "@e.md", "docs/e.md", "x");
        System.out.println("imports: " + py(importsOf("CLAUDE.md", texts)));
        Map<String, String> withTypo = new LinkedHashMap<>(texts);
        withTypo.put("CLAUDE.md", texts.get("CLAUDE.md") + " and @docs/typo.md");
        System.out.println("unresolved: " + py(unresolvedImports("CLAUDE.md", withTypo)));
    }
}
