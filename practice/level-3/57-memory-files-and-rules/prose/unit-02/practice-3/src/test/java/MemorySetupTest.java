import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MemorySetupTest {
    // config.dir is the project folder that holds CLAUDE.md, .claude/rules/ and docs/: starter, reference or a planted wrong solution.
    private static final Path ROOT = Path.of(System.getProperty("config.dir", "starter"));

    private static final List<String> T = List.of("Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test.",
        "Every test builds its data from a fixture in `tests/fixtures/`; no ad-hoc data inline.",
        "Mock the network with `msw`; a test never calls a live service.",
        "Each test checks one behaviour and its name states that behaviour.");
    private static final List<String> A = List.of("Every handler under `src/api/` validates its input with the shared `parse()` helper before it uses it.",
        "Return errors as `{ code, message }` with the HTTP status from `src/api/status.ts`.",
        "Handlers are `async` and never swallow a rejected promise.",
        "Document each endpoint with an OpenAPI comment above its handler.");
    private static final List<String> R = List.of("Every resource carries the `owner` and `cost-centre` tags.",
        "Run `terraform fmt` and `terraform validate` before proposing a change.",
        "Pin provider versions with `~>` constraints.");
    private static final List<String> U = List.of("Commit messages use the imperative mood and stay under 72 characters in the first line.",
        "Run `npm test` before saying a task is done.",
        "Do not add dependencies without asking.");
    private static final Map<String, List<String>> GROUPS = new LinkedHashMap<>();
    private static final String P1 = "I prefer short answers with no preamble.";
    private static final String P2 = "My sandbox API is at http://localhost:4010 with the dev token from my shell profile.";
    private static final List<String> FILES = List.of("src/api/users.ts", "src/api/orders.ts", "src/api/status.ts", "src/ui/Button.tsx", "src/ui/Button.test.tsx", "src/api/users.test.ts",
        "tools/cli/run.test.ts", "terraform/prod/main.tf", "terraform/modules/net/vpc.tf", "db/migrations/0001_init.sql", "README.md", "package.json");
    private static final List<String> TESTS = List.of("src/ui/Button.test.tsx", "src/api/users.test.ts", "tools/cli/run.test.ts");

    static {
        GROUPS.put("U", U);
        GROUPS.put("T", T);
        GROUPS.put("A", A);
        GROUPS.put("R", R);
    }

    /** A rule file's `paths` (null when it has none) and its body. */
    record Rule(List<String> paths, String body) {}

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String rel(Path path) {
        return ROOT.relativize(path).toString().replace('\\', '/');
    }

    private static List<String> ruleFiles() {
        Path folder = ROOT.resolve(".claude").resolve("rules");
        if (!Files.isDirectory(folder)) return List.of();
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".md")).map(MemorySetupTest::rel).sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bodyOf(String text) {
        Matcher m = Pattern.compile("^---\\n(.*?)\\n---\\n?(.*)$", Pattern.DOTALL).matcher(text);
        return m.matches() ? m.group(2) : text;
    }

    /** The `paths` of a rule as Claude Code reads it: a YAML list or a comma-separated string; null when there are none. */
    private static List<String> pathsOf(String text) throws IOException {
        Object value = HookGate.frontmatter(text).get("paths");
        if (value == null) return null;
        if (value instanceof String s) return Arrays.stream(s.split(",")).map(String::strip).toList();
        List<String> out = new ArrayList<>();
        for (Object o : (List<?>) value) out.add(String.valueOf(o));
        return out;
    }

    private static Map<String, Rule> ruleBodies() throws IOException {
        Map<String, Rule> out = new LinkedHashMap<>();
        for (String rel : ruleFiles()) out.put(rel, new Rule(pathsOf(read(rel)), bodyOf(read(rel))));
        return out;
    }

    /** Every Markdown file outside .claude/, by project-relative path: what an @import can reach. */
    private static Map<String, String> projectTexts() throws IOException {
        Map<String, String> out = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(ROOT)) {
            for (Path p : walk.filter(x -> Files.isRegularFile(x) && x.toString().endsWith(".md")).collect(Collectors.toList())) {
                if (!Arrays.asList(rel(p).split("/")).contains(".claude")) out.put(rel(p), Files.readString(p));
            }
        }
        return out;
    }

    /** CLAUDE.md and everything it imports: all of it is in context from the first message. */
    private static String launchText() throws IOException {
        Map<String, String> texts = projectTexts();
        List<String> files = new ArrayList<>(List.of("CLAUDE.md"));
        files.addAll(MemoryLoading.importsOf("CLAUDE.md", texts));
        return files.stream().map(texts::get).collect(Collectors.joining("\n"));
    }

    /** The text in context at launch plus the rules that the touched files bring in. */
    private static String contextFor(List<String> touched) throws IOException {
        Map<String, Rule> bodies = ruleBodies();
        Map<String, List<String>> paths = new LinkedHashMap<>();
        bodies.forEach((rel, rule) -> paths.put(rel, rule.paths()));
        List<String> parts = new ArrayList<>(List.of(launchText()));
        for (String rel : MemoryLoading.rulesLoaded(paths, touched)) parts.add(bodies.get(rel).body());
        return String.join("\n", parts);
    }

    private static Set<String> groupsIn(String text) {
        Set<String> found = new TreeSet<>();
        GROUPS.forEach((name, lines) -> {
            boolean all = lines.stream().allMatch(text::contains), any = lines.stream().anyMatch(text::contains);
            assertTrue(all || !any, "group " + name + " is only partly there: move whole conventions, not some of them");
            if (all) found.add(name);
        });
        return found;
    }

    @Test
    void m1_theConventionsOfEachAreaLoadForExactlyTheFilesThatAreaGoverns() throws IOException {
        Map<String, Set<String>> table = new LinkedHashMap<>();
        table.put("src/api/users.ts", Set.of("U", "A"));
        table.put("src/ui/Button.tsx", Set.of("U"));
        table.put("src/ui/Button.test.tsx", Set.of("U", "T"));
        table.put("src/api/users.test.ts", Set.of("U", "A", "T"));
        table.put("terraform/prod/main.tf", Set.of("U", "R"));
        table.put("README.md", Set.of("U"));
        table.put("tools/cli/run.test.ts", Set.of("U", "T"));
        Map<String, Set<String>> got = new LinkedHashMap<>();
        for (String touched : table.keySet()) got.put(touched, groupsIn(contextFor(List.of(touched))));
        List<String> wrong = new ArrayList<>();
        table.forEach((k, v) -> {
            if (!new TreeSet<>(v).equals(got.get(k))) wrong.add(k + ": got " + got.get(k) + ", want " + new TreeSet<>(v));
        });
        assertEquals(List.of(), wrong);
    }

    @Test
    void e1_theRootFileIsShortAndHoldsOnlyWhatEveryTaskNeeds() throws IOException {
        String root = read("CLAUDE.md");
        assertTrue(root.lines().count() <= 50, "the root file is long: it is read in every session, so only what every task needs belongs in it");
        assertTrue(U.stream().allMatch(root::contains), "the three rules that every task needs stay in the root file");
        String launch = launchText();
        for (String name : List.of("T", "A", "R")) assertFalse(GROUPS.get(name).stream().anyMatch(launch::contains), "the " + name + " conventions are in the launch context: an import loads at launch too");
    }

    @Test
    void e2_theTestingRuleFollowsTheFileTypeAndNotTheFolder() throws IOException {
        List<List<String>> testing = ruleBodies().values().stream().filter(r -> r.body().contains(T.get(0))).map(Rule::paths).toList();
        assertTrue(testing.size() == 1 && testing.get(0) != null && !testing.get(0).isEmpty(), "put the testing conventions in one rule file with paths");
        for (String file : TESTS) assertTrue(testing.get(0).stream().anyMatch(p -> MemoryLoading.globMatch(p, file)), file + " is a test file and the rule must cover it");
        for (String file : FILES.stream().filter(f -> !TESTS.contains(f)).toList()) assertFalse(testing.get(0).stream().anyMatch(p -> MemoryLoading.globMatch(p, file)), file + " is not a test file");
    }

    @Test
    void e3_theImportNamesAFileThatExistsAndLoadsAtLaunch() throws IOException {
        read("CLAUDE.md");
        Map<String, String> texts = projectTexts();
        assertEquals(List.of(), MemoryLoading.unresolvedImports("CLAUDE.md", texts), "an import that names no file imports nothing: check the spelling");
        assertEquals(List.of("docs/standards/architecture.md"), MemoryLoading.importsOf("CLAUDE.md", texts), "import the architecture notes with @docs/standards/architecture.md outside a code span");
    }

    @Test
    void e4_personalLinesSitInPersonalFilesAndTheLocalFileIsIgnored() {
        String user = read("user-memory.example.md"), local = read("CLAUDE.local.example.md");
        assertTrue(user.contains(P1) && !user.contains(P2) && local.contains(P2) && !local.contains(P1), "a preference of yours goes in the user file, a sandbox note in the local file");
        List<String> files = new ArrayList<>(List.of("CLAUDE.md", "docs/standards/architecture.md"));
        files.addAll(ruleFiles());
        assertFalse(files.stream().map(MemorySetupTest::read).anyMatch(text -> text.contains(P1) || text.contains(P2)), "a teammate would load your personal lines from the shared files");
        assertTrue(read(".gitignore").lines().map(String::strip).toList().contains("CLAUDE.local.md"), "CLAUDE.local.md must be ignored by git");
    }

    @Test
    void e5_aRuleThatMustAlwaysHoldIsAPermissionRuleAndNotASentence() {
        assertTrue(Files.isRegularFile(ROOT.resolve(".claude").resolve("settings.json")), ".claude/settings.json is missing");
        Map<String, Object> settings = SettingsLayers.json(read(".claude/settings.json"));
        assertEquals("deny", SettingsLayers.decide(settings, BuiltinTools.ruleTool("Edit"), "db/migrations/0001_init.sql"), "memory is context, not enforcement: deny the edit in settings");
        assertNotEquals("deny", SettingsLayers.decide(settings, BuiltinTools.ruleTool("Edit"), "src/api/users.ts"), "only the migrations are denied");
    }

    private static int count(String s, char c) {
        return (int) s.chars().filter(x -> x == c).count();
    }

    @Test
    void e6_everyRuleScopesItselfWithPathsThatAreValidAndMatchSomething() throws IOException {
        Map<String, Rule> bodies = ruleBodies();
        assertTrue(bodies.size() >= 3, "write the three rule files: testing, API and Terraform");
        for (Map.Entry<String, Rule> e : bodies.entrySet()) {
            List<String> paths = e.getValue().paths();
            assertTrue(paths != null && !paths.isEmpty(), e.getKey() + " has no usable paths: without them (or with frontmatter that does not parse) the rule loads in every session");
            for (String pattern : paths) {
                assertTrue(count(pattern, '{') == count(pattern, '}') && count(pattern, '[') == count(pattern, ']'), e.getKey() + ": the pattern '" + pattern + "' is not balanced");
                assertTrue(FILES.stream().anyMatch(f -> MemoryLoading.globMatch(pattern, f)), e.getKey() + ": the pattern '" + pattern + "' matches no file of the project: a bare folder name is not a glob");
            }
        }
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
