import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Audit a team's Claude Code setup for the exam's code generation scenario: which instructions load for which files, who gets the shared command and what is protected.
 *
 * <p>The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
 * documented behaviour (checked 2026-10-04): a rule file under .claude/rules/ with a paths list loads when Claude works with a matching file and one without paths loads
 * at launch; a command file under .claude/commands/ in the project is shared through version control; permission rules sit in .claude/settings.json. Nothing here starts Claude Code.
 */
public final class SetupAudit {
    static final Path HERE = Path.of("..").toAbsolutePath().normalize();

    /** A glob as a regular expression: ** crosses folders, * stays inside one, ? is one character. */
    static Pattern globRegex(String glob) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < glob.length()) {
            if (glob.startsWith("**/", i)) { out.append("(?:.*/)?"); i += 3; }
            else if (glob.startsWith("**", i)) { out.append(".*"); i += 2; }
            else if (glob.charAt(i) == '*') { out.append("[^/]*"); i += 1; }
            else if (glob.charAt(i) == '?') { out.append("[^/]"); i += 1; }
            else { out.append(Pattern.quote(String.valueOf(glob.charAt(i)))); i += 1; }
        }
        return Pattern.compile(out.toString());
    }

    /** The paths list of a rule file's front matter, or null when it has none (the rule then loads at launch). */
    static List<String> rulePaths(String text) {
        Matcher head = Pattern.compile("^---\\n(.*?)\\n---\\n", Pattern.DOTALL).matcher(text);
        if (!head.find()) return null;
        Matcher listed = Pattern.compile("^paths:\\s*\\n((?:[ \\t]+-[ \\t]+.*\\n?)+)", Pattern.MULTILINE).matcher(head.group(1) + "\n");
        if (!listed.find()) return null;
        List<String> paths = new ArrayList<>();
        for (String line : listed.group(1).split("\n")) {
            if (!line.isBlank()) paths.add(line.replaceFirst("^\\s*-\\s+", "").strip().replaceAll("^[\"']|[\"']$", ""));
        }
        return paths;
    }

    record Project(List<String> files, String memory, Map<String, List<String>> rules, List<String> commands, JsonNode settings) {}

    static List<String> list(Path dir, String suffix) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(suffix)).sorted().toList();
        }
    }

    static Project load(Path root) {
        try {
            Map<String, List<String>> rules = new LinkedHashMap<>();
            for (String name : list(root.resolve(".claude/rules"), ".md")) rules.put(name, rulePaths(Files.readString(root.resolve(".claude/rules").resolve(name))));
            List<String> files = List.of(Files.readString(root.resolve("files.txt")).strip().split("\\s+"));
            return new Project(files, Files.readString(root.resolve("CLAUDE.md")), rules, list(root.resolve(".claude/commands"), ".md"),
                new ObjectMapper().readTree(Files.readString(root.resolve(".claude/settings.json"))));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean matches(List<String> paths, String file) {
        return paths != null && paths.stream().anyMatch(g -> globRegex(g).matcher(file).matches());
    }

    static List<String> rulesFor(Map<String, List<String>> rules, String file) {
        return rules.entrySet().stream().filter(e -> e.getValue() == null || matches(e.getValue(), file)).map(Map.Entry::getKey).toList();
    }

    static boolean has(JsonNode array, String value) {
        if (array == null) return false;
        for (JsonNode n : array) if (n.asText().equals(value)) return true;
        return false;
    }

    static List<String> audit(Path root) {
        Project p = load(root);
        List<String> found = new ArrayList<>();
        int sections = (int) Pattern.compile("^## ", Pattern.MULTILINE).matcher(p.memory()).results().count();
        if (p.rules().isEmpty() && sections >= 3) found.add("all-in-root: " + sections + " sections in CLAUDE.md and no rule files");
        p.rules().forEach((name, paths) -> {
            if (paths == null) found.add("rule-loads-always: " + name);
            else if (p.files().stream().noneMatch(f -> matches(paths, f))) found.add("rule-matches-nothing: " + name);
        });
        for (String f : p.files()) {
            if (f.matches(".*\\.test\\.tsx?") && p.rules().values().stream().noneMatch(paths -> matches(paths, f))) found.add("test-uncovered: " + f);
        }
        if (p.commands().isEmpty()) found.add("no-shared-command");
        JsonNode perms = p.settings().path("permissions");
        if (!has(perms.get("deny"), "Read(./.env)")) found.add("env-readable");
        if (has(perms.get("allow"), "Bash")) found.add("bare-bash-allowed");
        return found;
    }

    public static void main(String[] args) {
        for (String name : List.of("project-before", "project-after")) {
            Path root = HERE.resolve(name);
            Project p = load(root);
            System.out.println(name + ": CLAUDE.md " + p.memory().stripTrailing().split("\n").length + " lines, " + p.rules().size() + " rule files, " + p.commands().size() + " shared commands");
            List<String> found = audit(root);
            for (String finding : found) System.out.println("  finding: " + finding);
            if (found.isEmpty()) {
                System.out.println("  no findings");
                for (String f : p.files()) {
                    List<String> names = rulesFor(p.rules(), f);
                    System.out.println("  " + f + " <- " + (names.isEmpty() ? "CLAUDE.md only" : String.join(", ", names)));
                }
            }
        }
    }
}
