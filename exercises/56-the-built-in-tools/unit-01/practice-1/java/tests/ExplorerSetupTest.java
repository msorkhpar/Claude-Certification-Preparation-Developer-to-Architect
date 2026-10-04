import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ExplorerSetupTest {
    // config.dir is the project folder that holds .claude/, agent-options.json and docs/: starter, reference or a planted wrong solution.
    private static final Path ROOT = Path.of(System.getProperty("config.dir", "starter"));

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> readJson(String rel) {
        try {
            return SettingsLayers.json(read(rel));
        } catch (IllegalArgumentException error) {
            throw new AssertionError(rel + " is not valid JSON: " + error.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) out.add(String.valueOf(x));
        return out;
    }

    private static Map<String, Object> settings() {
        return readJson(".claude/settings.json");
    }

    private static void check(String[][] table) {
        Map<String, Object> s = settings();
        List<String> wrong = new ArrayList<>();
        for (String[] row : table) {
            String got = SettingsLayers.decide(s, BuiltinTools.ruleTool(row[0]), row[1]);
            if (!got.equals(row[2])) wrong.add(row[0] + " " + row[1] + ": got " + got + ", want " + row[2]);
        }
        assertEquals(List.of(), wrong);
    }

    /** The text before the fallback heading, and the numbered steps before and after it. */
    record Sections(String head, List<String> steps, List<String> fallback) {}

    private static List<String> steps(String block) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("^\\d+\\. (.*)$", Pattern.MULTILINE).matcher(block);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static Sections sections(String text) {
        String sep = "## When an edit does not apply";
        int at = text.indexOf(sep);
        String head = at < 0 ? text : text.substring(0, at);
        String tail = at < 0 ? "" : text.substring(at + sep.length());
        return new Sections(head, steps(head), steps(tail));
    }

    private static boolean has(String regex, String text) {
        return Pattern.compile(regex).matcher(text).find();
    }

    @Test
    void m1_thePermissionRulesLetAnExplorerReadSearchAndTakeNotesButNotChangeTheSource() {
        check(new String[][] {
            {"Read", "src/inventory/stock.py", "allow"}, {"Grep", "src/inventory", "allow"}, {"Glob", "src/**/handlers/*.py", "allow"},
            {"Edit", "src/inventory/stock.py", "deny"}, {"Write", "src/inventory/new.py", "deny"}, {"Edit", "notes/findings.md", "allow"},
            {"Write", "notes/findings.md", "allow"}, {"Edit", "docs/readme.md", "ask"}, {"Bash", "git log --oneline", "allow"},
            {"Bash", "git diff HEAD~1", "allow"}, {"Bash", "git status", "allow"}, {"Bash", "rm -rf build", "ask"}, {"Bash", "git push origin main", "ask"}});
    }

    @Test
    void e1_aReadRuleProtectsSecretsFromReadingSearchingAndWriting() {
        check(new String[][] {
            {"Read", "./.env", "deny"}, {"Read", "secrets/prod.key", "deny"}, {"Grep", "secrets/prod.key", "deny"}, {"Glob", "secrets/prod.key", "deny"},
            {"Edit", ".env", "deny"}, {"Write", "secrets/new.key", "deny"}, {"Edit", "secrets/prod.key", "deny"}, {"Read", "vendor/secrets/a.txt", "deny"}});
    }

    @Test
    void e2_onlyRuleFormsThatAreConsultedAreUsedAndNoWholeToolThatChangesThingsIsAllowed() {
        Map<String, Object> perms = asMap(settings().get("permissions"));
        List<String> rules = new ArrayList<>();
        for (String kind : List.of("allow", "ask", "deny")) rules.addAll(strings(perms.get(kind)));
        assertFalse(rules.isEmpty(), "write the permission rules");
        assertEquals(List.of(), rules.stream().filter(r -> r.matches("(Write|NotebookEdit|MultiEdit)\\(.*")).toList(), "path rules for Write are never matched: write them as Edit(...)");
        assertEquals(List.of(), rules.stream().filter(r -> r.matches("(Grep|Glob)\\(.*")).toList(), "path rules for the search tools are written as Read(...)");
        assertEquals(List.of(), strings(perms.get("allow")).stream().filter(r -> List.of("Bash", "Bash(*)", "Edit", "Write").contains(r)).toList(), "a bare allow rule approves every call of that tool");
        assertTrue(Set.of("allow", "ask", "deny").containsAll(perms.keySet()));
    }

    @Test
    void e3_theExplorerAgentReadsAndSearchesAndSaysWhenToUseIt() throws IOException {
        String text = read(".claude/agents/explorer.md");
        Map<String, Object> fm = HookGate.frontmatter(text);
        Matcher m = Pattern.compile("^---\\n(.*?)\\n---\\n?(.*)$", Pattern.DOTALL).matcher(text);
        String body = m.matches() ? m.group(2) : text;
        assertEquals("explorer", fm.get("name"));
        List<String> tools = new ArrayList<>();
        for (String t : String.valueOf(fm.getOrDefault("tools", "")).split(",")) if (!t.strip().isEmpty()) tools.add(t.strip());
        assertEquals(List.of("Glob", "Grep", "Read"), tools.stream().sorted().toList(), "an explorer lists Read, Grep and Glob and nothing that edits or runs commands");
        assertTrue(has("(?:^|\\. )Use when\\b", String.valueOf(fm.getOrDefault("description", ""))), "the description starts a sentence with Use when");
        assertTrue(fm.get("maxTurns") instanceof Integer turns && turns >= 1 && turns <= 15 && fm.get("model") != null && !String.valueOf(fm.get("model")).isEmpty(), "bound the turns and choose a model");
        assertFalse(body.isBlank(), "the body holds the agent's instructions");
    }

    @Test
    void e4_theSdkOptionsMakeTheSearchToolsAvailableAndRemoveTheToolsThatChangeThings() {
        Map<String, Object> o = readJson("agent-options.json");
        List<String> tools = o.get("tools") == null ? null : strings(o.get("tools"));
        List<String> allowed = o.get("allowedTools") == null ? null : strings(o.get("allowedTools"));
        List<String> disallowed = o.get("disallowedTools") == null ? null : strings(o.get("disallowedTools"));
        assertEquals(List.of("Glob", "Grep", "Read"), (tools == null ? List.<String>of() : tools).stream().sorted().toList(),
            "tools lists the three the agent may have; naming Grep and Glob puts them back on macOS, Linux and WSL");
        assertEquals(List.of("Read", "Grep", "Glob"), BuiltinTools.toolSet("linux", tools, allowed == null ? List.of() : allowed, disallowed == null ? List.of() : disallowed));
        assertTrue(allowed != null && tools.containsAll(allowed), "allowedTools pre-approves only listed tools");
        assertTrue(!allowed.contains("Bash") && !allowed.contains("Edit") && !allowed.contains("Write"));
        assertTrue((disallowed == null ? List.<String>of() : disallowed).containsAll(List.of("Bash", "Edit", "Write")), "a bare name in disallowedTools removes the tool from the model's context");
    }

    @Test
    void e5_theExplorationPlanStartsWithASearchThenReadsAndNeverReadsEverythingFirst() {
        Sections s = sections(read("docs/exploration-plan.md"));
        assertTrue(has("(?i)do not read every file", s.head()), "say that the whole repository is not read first");
        assertTrue(s.steps().size() >= 4, "write the steps as a numbered list");
        int grep = -1, read = -1;
        for (int i = 0; i < s.steps().size(); i++) {
            if (grep < 0 && has("\\bGrep\\b", s.steps().get(i))) grep = i;
            if (read < 0 && has("\\bRead\\b", s.steps().get(i))) read = i;
        }
        assertTrue(grep >= 0 && read >= 0 && grep < read, "search for entry points before reading anything");
        assertTrue(s.steps().stream().anyMatch(t -> has("\\bGlob\\b", t) && has("`[^`]*(\\*\\*/|\\*\\.)[^`]*`", t)), "use Glob with a name pattern");
        final int afterRead = read;
        boolean traced = false;
        for (int i = 0; i < s.steps().size(); i++) {
            String t = s.steps().get(i);
            if (i > afterRead && has("export", t) && has("\\beach\\b", t) && has("\\bGrep\\b", t)) traced = true;
        }
        assertTrue(traced, "to trace usage through wrappers, list the exported names and search for each");
        assertTrue(s.steps().stream().anyMatch(t -> t.contains("notes/")), "findings go to notes/, the only place the agent may write");
    }

    @Test
    void e6_theEditFallbackWidensTheAnchorThenReplacesAllThenRewritesTheFile() {
        List<String> fallback = sections(read("docs/exploration-plan.md")).fallback();
        assertTrue(fallback.size() >= 3, "write the three remedies as a numbered list under 'When an edit does not apply'");
        assertTrue(has("surrounding|more context|longer", fallback.get(0)) && fallback.get(0).contains("unique"));
        assertTrue(fallback.get(1).contains("replace_all"));
        assertTrue(has("\\bRead\\b", fallback.get(2)) && has("\\bWrite\\b", fallback.get(2)) && has("(?i)whole file", fallback.get(2)));
    }

    @Test
    void e7_noFileHoldsAPersonalPathAnAddressOrAKey() throws IOException {
        List<String> hits = new ArrayList<>();
        String[][] patterns = {{"home path", "(/home/\\w+|/Users/\\w+|C:\\\\Users)"}, {"email address", "[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+"}, {"key", "sk-ant-[\\w-]{6,}"}};
        try (Stream<Path> walk = Files.walk(ROOT)) {
            for (Path path : walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList())) {
                String text = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
                for (String[] p : patterns) if (Pattern.compile(p[1]).matcher(text).find()) hits.add(ROOT.relativize(path) + ": " + p[0]);
            }
        }
        assertEquals(List.of(), hits);
    }
}
