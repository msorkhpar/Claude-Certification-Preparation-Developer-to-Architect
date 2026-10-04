import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CiSetupTest {
    // the folder that holds ci/: the starter, the reference or a planted wrong solution
    private static final Path ROOT = Path.of(System.getProperty("solution.dir", "starter"));

    private static final List<String> VAGUE = List.of("be conservative", "be careful", "only report important", "high confidence", "use good judgement", "use your judgment");
    private static final Set<String> READ_ONLY = Set.of("Read", "Grep", "Glob");

    private static List<String> words(String command) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        boolean quoted = false, started = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '\\' && i + 1 < command.length()) { word.append(command.charAt(++i)); started = true; }
            else if (c == '"') { quoted = !quoted; started = true; }
            else if (c == ' ' && !quoted) { if (started) out.add(word.toString()); word.setLength(0); started = false; }
            else { word.append(c); started = true; }
        }
        if (started) out.add(word.toString());
        return out;
    }

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode loadJson(String rel) {
        try {
            return new ObjectMapper().readTree(read(rel));
        } catch (IOException e) {
            throw new AssertionError(rel + " is not valid JSON: " + e.getMessage());
        }
    }

    private static Map<String, JsonNode> jobs() {
        Map<String, JsonNode> out = new LinkedHashMap<>();
        loadJson("ci/pipeline.json").path("jobs").forEach(j -> out.put(j.get("name").asText(), j));
        return out;
    }

    private static JsonNode job(String name) {
        JsonNode found = jobs().get(name);
        assertNotNull(found, "ci/pipeline.json has no job named " + name);
        return found;
    }

    private static List<String> strings(JsonNode node) {
        List<String> out = new ArrayList<>();
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    private static List<String> section(String text, String title) {
        Matcher match = Pattern.compile("^## " + title + "\\s*\\n(.*?)(?=^## |\\z)", Pattern.MULTILINE | Pattern.DOTALL).matcher(text);
        List<String> out = new ArrayList<>();
        if (match.find()) for (String line : match.group(1).split("\n")) if (line.startsWith("- ")) out.add(line.substring(2).strip());
        return out;
    }

    @Test
    void m1_whoWaitsDecidesBetweenRealTimeAndBatch() {
        assertEquals("realtime", job("pre-merge-review").get("api").asText(), "a developer waits for the merge check, so it runs in real time");
        assertEquals("batch", job("debt-report").get("api").asText(), "nobody waits for the overnight report, so it runs as a batch");
        for (JsonNode j : jobs().values()) assertEquals(j.get("audience").asText().equals("waiting") ? "realtime" : "batch", j.get("api").asText(), j.get("name").asText() + ": the audience decides the api");
    }

    @Test
    void e1_aReviewHasAPassForEachFileAndThenAnIntegrationPass() {
        assertEquals(List.of("per-file", "integration"), strings(job("pre-merge-review").get("passes")), "one pass per file for local issues, then one pass across files, in that order");
    }

    @Test
    void e2_aReviewRunsInAFreshSessionAndIsGivenTheEarlierFindings() {
        JsonNode review = job("pre-merge-review");
        assertEquals("fresh", review.get("session").asText(), "the session that wrote the code is biased toward it");
        assertTrue(strings(review.get("context")).contains("prior_findings"), "a re-run needs the earlier findings so that it reports only what is new");
    }

    @Test
    void e3_everyClaudeCommandIsHeadlessJsonAndBounded() {
        for (JsonNode j : jobs().values()) {
            List<String> tokens = words(j.get("command").asText());
            String name = j.get("name").asText();
            if (!tokens.get(0).equals("claude")) continue;
            assertTrue(tokens.contains("-p") || tokens.contains("--print"), name + ": without -p the run waits for input");
            int fmt = tokens.indexOf("--output-format");
            assertTrue(fmt >= 0 && tokens.get(fmt + 1).equals("json"), name + ": ask for json output");
            int turns = tokens.indexOf("--max-turns");
            assertTrue(turns >= 0 && tokens.get(turns + 1).matches("\\d+"), name + ": bound the run with --max-turns");
        }
        List<String> tokens = words(job("pre-merge-review").get("command").asText());
        assertTrue(tokens.contains("--json-schema"), "the review answers in a schema");
        JsonNode item = loadJson(tokens.get(tokens.indexOf("--json-schema") + 1)).path("properties").path("findings").path("items");
        assertTrue(item.path("properties").path("severity").has("enum"), "severity is a closed list in the schema");
        List<String> required = strings(item.path("required"));
        for (String field : List.of("file", "line", "severity", "issue", "suggestion")) assertTrue(required.contains(field), "a finding is required to say where, how bad, what and what to do");
    }

    @Test
    void e4_theReviewCanOnlyRead() {
        JsonNode review = job("pre-merge-review");
        List<String> tools = strings(review.get("tools"));
        assertTrue(!tools.isEmpty() && READ_ONLY.containsAll(tools), "read-only tools only, found " + tools);
        List<String> tokens = words(review.get("command").asText());
        int allowed = tokens.indexOf("--allowedTools");
        assertTrue(allowed >= 0, "list the allowed tools in the command");
        assertTrue(READ_ONLY.containsAll(List.of(tokens.get(allowed + 1).split(","))), "the command approves read tools only");
    }

    @Test
    void e5_theCriteriaNameWhatToReportWhatToSkipAndAnExampleForEachSeverity() {
        String text = read("ci/review-criteria.md");
        assertTrue(section(text, "Report").size() >= 2, "list at least two kinds of issue to report");
        assertTrue(section(text, "Skip").size() >= 2, "list at least two kinds of issue to skip");
        Map<String, String> levels = new LinkedHashMap<>();
        for (String line : section(text, "Severity")) levels.put(line.split(":")[0], line);
        for (String level : List.of("high", "medium", "low")) assertTrue(levels.containsKey(level) && levels.get(level).contains("Example:"), level + " needs a description and an Example:");
        assertEquals(List.of(), VAGUE.stream().filter(p -> text.toLowerCase().contains(p)).toList(), "a vague instruction does not make a review more precise: name the cases");
    }

    @Test
    void e6_theTestPromptPassesTheExistingTestsAndSaysWhatAUsefulTestIs() {
        String text = read("ci/testgen-prompt.md");
        assertTrue(text.contains("{{existing_tests}}") && text.contains("{{changed_files}}"), "the prompt carries the changed files and the existing tests");
        assertTrue(section(text, "A useful test").size() >= 3, "say what a useful test is, in at least three points");
        assertTrue(section(text, "Do not write").size() >= 2, "say what not to write, in at least two points");
    }

    @Test
    void e7_noFileHoldsAPersonalPathAnAddressOrAKey() throws IOException {
        List<String> hits = new ArrayList<>();
        Pattern home = Pattern.compile("(/home/\\w+|/Users/\\w+|C:\\\\Users)");
        Pattern email = Pattern.compile("[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+");
        Pattern key = Pattern.compile("sk-ant-[\\w-]{6,}");
        try (Stream<Path> s = Files.walk(ROOT)) {
            for (Path p : s.filter(Files::isRegularFile).sorted().toList()) {
                String text = Files.readString(p);
                String rel = ROOT.relativize(p).toString();
                if (home.matcher(text).find()) hits.add(rel + ": home path");
                if (email.matcher(text).find()) hits.add(rel + ": email address");
                if (key.matcher(text).find()) hits.add(rel + ": key");
            }
        }
        assertEquals(List.of(), hits);
    }
}
