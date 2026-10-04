import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CiReviewTest {
    // config.dir is the project folder that holds the workflow, CLAUDE.md, the schema and the gate: starter, reference or a planted wrong solution.
    private static final Path ROOT = Path.of(System.getProperty("config.dir", "starter"));
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNode REVIEW_SCHEMA = CiGate.REVIEW_SCHEMA;

    private static Map<String, Object> policy(List<String> failOn) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("min_severity", "medium");
        p.put("disabled_categories", List.of("style"));
        p.put("fail_on", failOn);
        return p;
    }

    private static final Map<String, Object> POLICY = policy(List.of("high"));
    private static final List<String> VAGUE = List.of("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment");

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> finding(Object... overrides) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("file", "api.py");
        f.put("line", 12);
        f.put("category", "bug");
        f.put("severity", "medium");
        f.put("issue", "Unchecked None.");
        f.put("suggested_fix", "Return early.");
        f.put("detected_pattern", "missing-none-check");
        for (int i = 0; i + 1 < overrides.length; i += 2) f.put((String) overrides[i], overrides[i + 1]);
        return f;
    }

    private static Map<String, Object> run(List<Map<String, Object>> findings, int code, Map<String, Object> policy, Map<String, Object> over) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("structured_output", Map.of("findings", findings));
        merged.putAll(over);
        Map<String, Object> result = ReviewGate.gate(CiGate.envelope(merged), code, REVIEW_SCHEMA, policy);
        assertNotNull(result, "gate returned nothing");
        return result;
    }

    private static Map<String, Object> run(List<Map<String, Object>> findings) {
        return run(findings, 0, POLICY, Map.of());
    }

    private static Map<String, Object> decision(int exit, List<Map<String, Object>> comments, List<String> problems) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exit", exit);
        out.put("problems", problems);
        out.put("comments", comments);
        return out;
    }

    private static Map<String, Object> comment(String file, int line, String severity, String body) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("file", file);
        c.put("line", line);
        c.put("severity", severity);
        c.put("body", body);
        return c;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> comments(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("comments");
    }

    @SuppressWarnings("unchecked")
    private static List<String> problems(Map<String, Object> result) {
        return (List<String>) result.get("problems");
    }

    private static boolean has(String regex, String text) {
        return Pattern.compile(regex).matcher(text).find();
    }

    @Test
    void m1_aValidRunPostsTheFindingsAboveTheFloorAndOutsideTheDisabledCategories() {
        Map<String, Object> result = run(List.of(finding(), finding("category", "style", "severity", "medium"), finding("line", 3, "severity", "low"),
            finding("file", "ui.py", "line", 7, "category", "security", "severity", "medium", "issue", "Unescaped input.", "suggested_fix", "Escape it.")));
        assertEquals(decision(0, List.of(comment("api.py", 12, "medium", "Unchecked None. Suggested fix: Return early."), comment("ui.py", 7, "medium", "Unescaped input. Suggested fix: Escape it.")), List.of()), result);
        assertEquals(decision(0, List.of(), List.of()), run(List.of()));
    }

    @Test
    void e1_aFailedRunFailsTheJobInsteadOfPassingItSilently() {
        Object[][] cases = {
            {CiGate.envelope(Map.of("subtype", "error_max_turns", "is_error", true)), 1, "error_max_turns"},
            {CiGate.envelope(Map.of("subtype", "error_max_structured_output_retries", "is_error", true)), 1, "retries"},
            {CiGate.envelope(), 0, "structured_output"},
            {CiGate.envelope(Map.of("is_error", true, "structured_output", Map.of("findings", List.of()))), 0, "ended"},
            {"Error: no key", 1, "not a JSON"}, {"[1]", 0, "not a JSON"}, {"", 0, "not a JSON"}};
        for (Object[] c : cases) {
            Map<String, Object> result = ReviewGate.gate((String) c[0], (Integer) c[1], REVIEW_SCHEMA, POLICY);
            assertNotNull(result);
            assertTrue(Integer.valueOf(1).equals(result.get("exit")) && comments(result).isEmpty() && problems(result).stream().anyMatch(p -> p.contains((String) c[2])), c[0] + " -> " + result);
        }
        Map<String, Object> failed = run(List.of(), 2, POLICY, Map.of());
        assertTrue(Integer.valueOf(1).equals(failed.get("exit")) && problems(failed).stream().anyMatch(p -> p.contains("exited with status 2")), failed.toString());
    }

    @Test
    void e2_anAnswerThatBreaksTheSchemaFailsTheJobAndNamesThePathOfTheProblem() {
        Map<String, Object> noFile = finding();
        noFile.remove("file");
        Map<String, Object> extra = finding();
        extra.put("confidence", 0.9);
        Object[][] cases = {{List.of(finding("line", "12")), "$.findings[0].line"}, {List.of(finding("severity", "critical")), "$.findings[0].severity"},
            {List.of(noFile), "$.findings[0].file"}, {List.of(extra), "$.findings[0].confidence"}};
        for (Object[] c : cases) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = run((List<Map<String, Object>>) c[0]);
            assertTrue(Integer.valueOf(1).equals(result.get("exit")) && comments(result).isEmpty() && problems(result).stream().anyMatch(p -> p.startsWith("schema " + c[1])), c[0] + " -> " + result);
        }
    }

    @Test
    void e3_aFindingAtTheFailingSeverityBlocksTheMergeAndLowerOnesOnlyComment() {
        Map<String, Object> blocked = run(List.of(finding("severity", "high"), finding("line", 30)));
        assertEquals(1, blocked.get("exit"));
        assertEquals(List.of("high", "medium"), comments(blocked).stream().map(c -> c.get("severity")).toList());
        assertEquals(List.of(), problems(blocked));
        assertEquals(0, run(List.of(finding("severity", "medium"))).get("exit"));
        assertEquals(1, run(List.of(finding("severity", "medium")), 0, policy(List.of("medium", "high")), Map.of()).get("exit"));
        assertEquals(0, run(List.of(finding("severity", "high", "category", "style"))).get("exit"), "a disabled category cannot block");
    }

    @Test
    void e4_thePromptListsEarlierFindingsAndExistingTestsAndAsksForNewOrUnaddressedIssuesOnly() {
        List<Map<String, Object>> prior = List.of(Map.of("file", "api.py", "line", 12, "category", "bug", "issue", "Unchecked None."),
            Map.of("file", "ui.py", "line", 7, "category", "security", "issue", "Unescaped input."));
        String full = ReviewGate.reviewPrompt("+ x = 1", prior, List.of("test_empty_cart", "test_two_items"));
        assertEquals(String.join("\n", List.of("<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed.",
            "Do not repeat a finding listed in <already_reported>.", "Do not suggest a test for a behaviour that an existing test in <existing_tests> already covers.", "</instructions>",
            "<already_reported>", "- api.py:12 [bug] Unchecked None.", "- ui.py:7 [security] Unescaped input.", "</already_reported>", "<existing_tests>", "- test_empty_cart", "- test_two_items",
            "</existing_tests>", "<diff>", "+ x = 1", "</diff>")), full);
        String first = ReviewGate.reviewPrompt("+ x = 1", List.of(), List.of());
        assertEquals(String.join("\n", List.of("<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed.", "</instructions>",
            "<diff>", "+ x = 1", "</diff>")), first);
    }

    private static Set<String> keywords(JsonNode node) {
        Set<String> found = new HashSet<>();
        if (node.isObject()) {
            for (var it = node.fields(); it.hasNext();) {
                var e = it.next();
                if (e.getKey().equals("properties") && e.getValue().isObject()) {
                    for (JsonNode sub : e.getValue()) found.addAll(keywords(sub));
                } else {
                    found.add(e.getKey());
                    found.addAll(keywords(e.getValue()));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode v : node) found.addAll(keywords(v));
        }
        return found;
    }

    private static Set<String> texts(JsonNode array) {
        Set<String> out = new HashSet<>();
        if (array != null) for (JsonNode n : array) out.add(n.asText());
        return out;
    }

    private static JsonNode tree(Object o) {
        return JSON.valueToTree(o);
    }

    @Test
    void e5_theSchemaFileIsValidDraft07AndRequiresEveryFieldOfAFinding() {
        JsonNode schema;
        try {
            schema = JSON.readTree(read("review-schema.json"));
        } catch (IOException error) {
            throw new AssertionError("review-schema.json is not valid JSON: " + error.getMessage());
        }
        assertEquals("http://json-schema.org/draft-07/schema#", schema.path("$schema").asText("http://json-schema.org/draft-07/schema#"), "the SDK validates draft-07 and rejects a newer $schema");
        Set<String> banned = Set.of("minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf", "minLength", "maxLength");
        assertTrue(keywords(schema).stream().noneMatch(banned::contains), "structured outputs do not support numeric or string constraints");
        JsonNode items = schema.path("properties").path("findings").path("items");
        assertEquals(Set.of("file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"), texts(items.get("required")), "every field of a finding is required");
        assertTrue(items.path("additionalProperties").isBoolean() && !items.path("additionalProperties").asBoolean()
            && schema.path("additionalProperties").isBoolean() && !schema.path("additionalProperties").asBoolean() && tree(List.of("findings")).equals(schema.get("required")));
        JsonNode props = items.path("properties");
        assertEquals(Set.of("low", "medium", "high"), texts(props.path("severity").get("enum")));
        assertTrue(texts(props.path("category").get("enum")).containsAll(Set.of("bug", "security", "style", "other")));
        assertEquals("integer", props.path("line").path("type").asText(null));
        assertEquals(List.of(), SchemaCheck.schemaCheck(tree(Map.of("findings", List.of(finding()))), schema));
        assertEquals(List.of(), SchemaCheck.schemaCheck(tree(Map.of("findings", List.of())), schema));
        Map<String, Object> noPattern = finding();
        noPattern.remove("detected_pattern");
        Map<String, Object> extra = finding();
        extra.put("confidence", 0.9);
        for (Map<String, Object> bad : List.of(noPattern, finding("severity", "critical"), finding("line", "12"), extra)) {
            assertFalse(SchemaCheck.schemaCheck(tree(Map.of("findings", List.of(bad))), schema).isEmpty(), "the schema accepted " + bad);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @Test
    void e6_theWorkflowRunsClaudeHeadlessWithJsonOutputASchemaATurnLimitReadOnlyToolsAndTheContextFile() throws IOException {
        Map<String, Object> wf = WorkflowLint.parseYaml(read(".github/workflows/claude-review.yml"));
        Object on = wf.get("on");
        assertTrue(on instanceof Map<?, ?> m ? m.containsKey("pull_request") : "pull_request".equals(on), "run the review on pull requests");
        Map<String, Object> job = asMap(asMap(wf.get("jobs")).get("review"));
        assertTrue(job.get("timeout-minutes") instanceof Integer t && t <= 30, "a stuck run must not hold the runner");
        assertEquals("read", asMap(job.get("permissions")).get("contents"), "a review reads the repository");
        Map<String, Object> step = null;
        for (Object s : (List<?>) job.getOrDefault("steps", List.of())) if (String.valueOf(asMap(s).getOrDefault("run", "")).contains("claude")) { step = asMap(s); break; }
        assertNotNull(step, "a step runs claude");
        assertTrue(String.valueOf(asMap(step.get("env")).getOrDefault("ANTHROPIC_API_KEY", "")).startsWith("${{ secrets."), "the key comes from the secrets context");
        String command = String.valueOf(step.get("run"));
        assertEquals(List.of(), CiGate.lintCommand(command));
        Matcher turns = Pattern.compile("--max-turns[ =](\\d+)").matcher(command);
        assertTrue(turns.find() && Integer.parseInt(turns.group(1)) <= 10);
        Matcher allowed = Pattern.compile("--allowed[Tt]ools[ =](\"[^\"]*\"|\\S+)").matcher(command);
        assertTrue(allowed.find());
        String listed = allowed.group(1).replaceAll("^\"+|\"+$", "");
        List<String> tools = new ArrayList<>();
        Matcher t = Pattern.compile("[^\\s,(]+(?:\\([^)]*\\))?").matcher(listed);
        while (t.find()) tools.add(t.group());
        assertTrue(!tools.isEmpty() && tools.stream().allMatch(x -> List.of("Read", "Grep", "Glob").contains(x) || x.matches("Bash\\(git (diff|log|show|status)( \\*)?\\)")), "a review needs read-only tools: " + tools);
        assertTrue(has("--json-schema\\s+\"\\$\\(cat review-schema\\.json\\)\"", command), "pass the schema file to --json-schema");
        assertTrue(has("--append-system-prompt-file\\s+CLAUDE\\.md", command), "--bare skips CLAUDE.md, so pass it by hand");
    }

    private static List<String> section(String text, String title) {
        Matcher m = Pattern.compile("^## " + Pattern.quote(title) + "\\s*\\n(.*?)(?=^## |\\z)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL).matcher(text);
        assertTrue(m.find(), "CLAUDE.md has no section '## " + title + "'");
        return m.group(1).lines().filter(l -> l.startsWith("- ")).map(l -> l.substring(2)).toList();
    }

    @Test
    void e7_theProjectFileStatesWhatToReportAndWhatToSkipWithASeverityExampleForEachLevel() {
        String text = read("CLAUDE.md");
        assertTrue(VAGUE.stream().noneMatch(text.toLowerCase()::contains), "a general instruction like be conservative does not improve precision: name the patterns");
        List<String> report = section(text, "Report"), skip = section(text, "Skip");
        assertTrue(report.size() >= 3 && report.stream().anyMatch(r -> r.toLowerCase().contains("bug")) && report.stream().anyMatch(r -> r.toLowerCase().contains("security")), "list at least three categories to report, among them bugs and security");
        assertTrue(skip.size() >= 2 && skip.stream().anyMatch(s -> s.toLowerCase().contains("style")), "list what to skip, minor style among it");
        List<String> severity = section(text, "Severity");
        for (String level : List.of("high", "medium", "low")) {
            String line = severity.stream().filter(s -> s.toLowerCase().startsWith(level + ":")).findFirst().orElse(null);
            assertTrue(line != null && has("`[^`]+`", line), "severity " + level + " needs a concrete example in code");
        }
        assertTrue(section(text, "Testing standards").stream().anyMatch(s -> s.contains("tests/fixtures/")), "name the fixtures folder in the testing standards");
    }

    @Test
    void e8_noFileHoldsAPersonalPathAnAddressOrAKey() throws IOException {
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
