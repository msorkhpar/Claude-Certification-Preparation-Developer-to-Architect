import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProjectSetupTest {
    // config.dir is the project folder that holds CLAUDE.md and .claude/: starter, reference or a planted wrong solution.
    private static final Path ROOT = Path.of(System.getProperty("config.dir", "starter"));
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Map<String, Object> MANAGED = SettingsLayers.json("{\"permissions\": {\"deny\": [\"Bash(sudo *)\"]}}"); // a fictional organisation policy
    private static final Map<String, Object> USER = SettingsLayers.json("{\"model\": \"haiku\", \"permissions\": {\"allow\": [\"Bash(ls *)\"]}}");

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readJson(String rel) {
        try {
            Object data = JSON.readValue(read(rel), Object.class);
            assertTrue(data instanceof Map, rel + " must hold a JSON object");
            return (Map<String, Object>) data;
        } catch (IOException error) {
            throw new AssertionError(rel + " is not valid JSON: " + error.getMessage());
        }
    }

    private static Map<String, Map<String, Object>> layers(boolean withLocal) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        out.put("managed", MANAGED);
        out.put("user", USER);
        out.put("project", readJson(".claude/settings.json"));
        if (withLocal && Files.isRegularFile(ROOT.resolve(".claude/settings.local.json"))) out.put("local", readJson(".claude/settings.local.json"));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> permissions(Map<String, Object> settings) {
        Object perms = settings.get("permissions");
        return perms == null ? Map.of() : (Map<String, Object>) perms;
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> l) for (Object o : l) out.add(String.valueOf(o));
        return out;
    }

    private static String flat(String script) {
        return script.replaceAll("\\\\\\n", " ");
    }

    @Test
    void m1_theMemoryFileIsShortConcreteAndPullsInTheArchitectureNotes() {
        String text = read("CLAUDE.md");
        assertTrue(text.lines().count() <= 200, "keep CLAUDE.md under 200 lines");
        assertTrue(text.contains("`make test`") && text.contains("`make lint`"), "name the test and lint commands in backticks");
        assertTrue(Pattern.compile("^[^`\\n]*@docs/architecture\\.md", Pattern.MULTILINE).matcher(text).find(), "import docs/architecture.md with an @ line");
        long emphasis = text.lines().filter(line -> Pattern.compile("\\b(IMPORTANT|MUST|NEVER|ALWAYS)\\b").matcher(line).find()).count();
        assertTrue(emphasis <= 2, "emphasis on many lines makes none of them stand out");
        Map<String, String> files = new LinkedHashMap<>();
        files.put("/p/CLAUDE.md", text);
        files.put("/p/docs/architecture.md", read("docs/architecture.md"));
        assertEquals(List.of("/p/CLAUDE.md", "/p/docs/architecture.md"), SettingsLayers.loadMemory(files, "/p", null, null), "the import must resolve to the architecture file");
    }

    @Test
    void e1_thePermissionRulesAllowTheDailyCommandsAskBeforeCommitsAndDenySecretsAndPushes() {
        Map<String, Object> s = SettingsLayers.effectiveSettings(layers(true), true);
        String mode = String.valueOf(permissions(s).getOrDefault("defaultMode", "default"));
        String[][] table = {
            {"Bash", "make test", "allow"}, {"Bash", "make lint", "allow"}, {"Bash", "make deploy", "ask"}, {"Bash", "git status", "allow"},
            {"Bash", "git diff HEAD~1", "allow"}, {"Bash", "git commit -m 'AB-1 fix'", "ask"}, {"Bash", "git push origin main", "deny"},
            {"Bash", "make test && git push origin main", "deny"}, {"Bash", "curl https://example.com", "deny"}, {"Bash", "sudo make test", "deny"},
            {"Bash", "ls -la", "allow"}, {"Read", "./.env", "deny"}, {"Read", "secrets/key.pem", "deny"}, {"Edit", ".env", "deny"},
            {"Read", "src/api/app.py", "allow"}, {"Edit", "src/api/app.py", "allow"}};
        List<String> wrong = new ArrayList<>();
        for (String[] row : table) {
            String got = SettingsLayers.decide(s, row[0], row[1], mode);
            if (!got.equals(row[2])) wrong.add(row[0] + " " + row[1] + ": got " + got + ", want " + row[2]);
        }
        assertEquals(List.of(), wrong);
    }

    @Test
    void e2_theSharedFileSetsAModeItMaySetAndUsesOnlyRulesThatAreConsulted() {
        Map<String, Object> shared = readJson(".claude/settings.json");
        Map<String, Object> perms = permissions(shared);
        assertEquals("acceptEdits", perms.get("defaultMode"), "set defaultMode to acceptEdits; auto and bypassPermissions are ignored in a repository file");
        Map<String, Map<String, Object>> only = new LinkedHashMap<>();
        only.put("project", shared);
        assertEquals("acceptEdits", permissions(SettingsLayers.effectiveSettings(only, true)).get("defaultMode"));
        List<String> rules = new ArrayList<>();
        for (String kind : List.of("allow", "ask", "deny")) rules.addAll(strings(perms.get(kind)));
        assertEquals(List.of(), rules.stream().filter(r -> r.matches("(Write|NotebookEdit|MultiEdit)\\(.*")).toList(), "path rules for Write are never consulted: use Edit or Read");
        assertEquals(List.of(), strings(perms.get("allow")).stream().filter(r -> r.equals("Bash") || r.equals("Bash(*)")).toList(), "a bare Bash allow rule approves every command");
        assertTrue(List.of("defaultMode", "allow", "ask", "deny").containsAll(perms.keySet()));
    }

    @Test
    void e3_personalSettingsStayLocalAndTheLocalFileWins() {
        List<String> ignore = read(".gitignore").lines().map(String::strip).toList();
        assertTrue(ignore.contains(".claude/settings.local.json") && ignore.contains("CLAUDE.local.md"), "git must ignore both personal files");
        assertEquals("opus", readJson(".claude/settings.json").get("model"), "the team default model is opus");
        assertEquals("sonnet", SettingsLayers.effectiveSettings(layers(true), true).get("model"), "the local file overrides the team model");
        assertEquals("opus", SettingsLayers.effectiveSettings(layers(false), true).get("model"));
        Map<String, Object> local = readJson(".claude/settings.local.json");
        assertTrue(!local.containsKey("permissions") && !local.containsKey("env"), "keep the local file to the model override");
    }

    @Test
    void e4_theCustomCommandIsASkillThatOnlyAPersonCanStart() throws IOException {
        String text = read(".claude/skills/fix-issue/SKILL.md");
        assertTrue(text.startsWith("---") && text.indexOf("\n---", 3) >= 0, "the skill needs front matter between two --- lines");
        Map<String, Object> fm = HookGate.frontmatter(text);
        String body = text.substring(text.indexOf("\n---", 3) + 4);
        assertTrue("fix-issue".equals(fm.get("name")) && fm.get("description") != null && !String.valueOf(fm.get("description")).isBlank(), "name and description are required");
        assertEquals(Boolean.TRUE, fm.get("disable-model-invocation"), "a command that edits code and calls gh is started by a person");
        assertTrue(fm.get("argument-hint") != null && !String.valueOf(fm.get("argument-hint")).isEmpty(), "show the argument in the menu");
        assertTrue(body.contains("$ARGUMENTS") && body.contains("`make test`"));
    }

    @Test
    void e5_theHeadlessScriptIsBoundedAndDoesNotSkipPermissions() {
        String flat = flat(read("scripts/ci-review.sh"));
        assertTrue(Pattern.compile("\\bclaude -p\\b").matcher(flat).find() && flat.contains("--output-format json") && flat.contains("--bare"));
        var turns = Pattern.compile("--max-turns (\\d+)").matcher(flat);
        assertTrue(turns.find() && Integer.parseInt(turns.group(1)) >= 1 && Integer.parseInt(turns.group(1)) <= 10, "cap the turns at 10 or fewer");
        assertTrue(Pattern.compile("--max-budget-usd \\d").matcher(flat).find(), "cap the spend");
        assertTrue(Pattern.compile("--permission-mode (dontAsk|plan)\\b").matcher(flat).find(), "start from a mode that never prompts and never auto-approves");
        var allowed = Pattern.compile("--allowedTools \"([^\"]*)\"").matcher(flat);
        assertTrue(allowed.find(), "list the pre-approved tools");
        List<String> tools = Stream.of(allowed.group(1).split(",(?![^()]*\\))")).map(String::strip).toList();
        assertTrue(!tools.isEmpty() && !tools.contains("Bash") && !tools.contains("Edit") && !tools.contains("Write"), "pre-approve patterns, not whole tools that change things");
        assertTrue(!flat.contains("dangerously-skip-permissions") && !flat.contains("bypassPermissions"));
    }

    @Test
    void e6_noFileHoldsAPersonalPathAnAddressOrAKey() throws IOException {
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
