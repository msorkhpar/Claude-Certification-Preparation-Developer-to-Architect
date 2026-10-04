import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PipelineSetupTest {
    // config.dir is the repository folder: starter, reference or a planted wrong solution.
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

    /** A workflow's text and its parsed map; it needs a jobs map. */
    record Workflow(String text, Map<String, Object> map) {
        Map<String, Object> jobs() {
            return asMap(map.get("jobs"));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return o instanceof List<?> l ? (List<Object>) l : List.of();
    }

    private static Workflow workflow(String rel) {
        String text = read(rel);
        Map<String, Object> wf;
        try {
            wf = WorkflowLint.parseYaml(text);
        } catch (IOException | RuntimeException error) {
            throw new AssertionError(rel + " is not readable YAML: " + error.getMessage());
        }
        assertTrue(wf != null && wf.get("jobs") instanceof Map, rel + " needs a jobs map");
        return new Workflow(text, wf);
    }

    private static Map<String, Object> claudeStep(Map<String, Object> job) {
        List<Map<String, Object>> steps = new ArrayList<>();
        for (Object s : asList(job.get("steps"))) if (String.valueOf(asMap(s).getOrDefault("uses", "")).startsWith("anthropics/claude-code-action")) steps.add(asMap(s));
        assertEquals(1, steps.size(), "one claude-code-action step");
        return steps.get(0);
    }

    private static String str(Object o) {
        return o == null ? "None" : String.valueOf(o);
    }

    private static List<String> lint(String text) {
        try {
            return WorkflowLint.lint(text).stream().map(Object::toString).toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void m1_theMentionWorkflowAnswersOnlyClaudeCommentsAndHoldsNoKey() {
        Workflow w = workflow(".github/workflows/claude.yml");
        Map<String, Object> on = asMap(w.map().get("on"));
        assertEquals(java.util.Set.of("issue_comment", "pull_request_review_comment"), on.keySet(), "trigger on issue comments and review comments");
        for (Object v : on.values()) assertEquals(List.of("created"), asMap(v).get("types"), "only new comments");
        Map<String, Object> job = asMap(w.jobs().get("claude"));
        assertFalse(job.isEmpty(), "the job is called claude");
        assertTrue(str(job.get("if")).contains("@claude") && str(job.get("if")).contains("github.event.comment.body"), "start the runner only for comments that mention @claude");
        assertTrue(str(asMap(asList(job.get("steps")).get(0)).get("uses")).startsWith("actions/checkout@v"), "check the repository out first");
        Map<String, Object> step = claudeStep(job);
        assertEquals("anthropics/claude-code-action@v1", step.get("uses"));
        assertEquals("${{ secrets.ANTHROPIC_API_KEY }}", asMap(step.get("with")).get("anthropic_api_key"));
        Map<String, Object> perms = asMap(job.get("permissions"));
        assertEquals(List.of("write", "write", "write", "write"), List.of("contents", "pull-requests", "issues", "id-token").stream().map(perms::get).toList());
        assertEquals(List.of(), lint(w.text()));
    }

    @Test
    void e1_theReviewWorkflowReadsTheCodeAndPostsTheReview() {
        Workflow w = workflow(".github/workflows/review.yml");
        Map<String, Object> on = asMap(w.map().get("on"));
        assertEquals(List.of("pull_request"), new ArrayList<>(on.keySet()));
        assertEquals(List.of("opened", "synchronize", "ready_for_review", "reopened"), asMap(on.get("pull_request")).get("types"));
        Map<String, Object> job = asMap(w.jobs().get("review"));
        assertFalse(job.isEmpty(), "the job is called review");
        Map<String, Object> perms = asMap(job.get("permissions"));
        assertTrue("read".equals(perms.get("contents")) && "write".equals(perms.get("id-token")), "a review reads the code");
        List<String> uses = asList(job.get("steps")).stream().map(s -> String.valueOf(asMap(s).getOrDefault("uses", ""))).toList();
        assertTrue(uses.get(0).startsWith("actions/checkout@v") && uses.get(1).equals("anthropics/claude-code-action@v1"));
        Map<String, Object> with = asMap(claudeStep(job).get("with"));
        assertTrue(str(with.get("prompt")).contains("/code-review") && str(with.get("prompt")).contains("--comment"), "run the review skill and post its findings");
        assertTrue(str(with.get("claude_args")).contains("mcp__github_inline_comment__create_inline_comment"), "name the inline comment tool in claude_args");
        assertTrue(str(with.get("plugins")).matches("code-review@[\\w-]+") && with.get("plugin_marketplaces") != null && !str(with.get("plugin_marketplaces")).isEmpty(), "install the review plugin from a marketplace");
        assertEquals(List.of(), lint(w.text()));
    }

    @Test
    void e2_eachPromptFileCarriesAVersionThatItsChangelogExplains() throws IOException {
        Path dir = ROOT.resolve("prompts");
        List<Path> prompts = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                prompts = files.filter(p -> p.getFileName().toString().endsWith(".md") && !p.getFileName().toString().equals("CHANGELOG.md")).sorted().collect(Collectors.toList());
            }
        }
        assertFalse(prompts.isEmpty(), "prompts/ holds at least one prompt");
        String changelog = read("prompts/CHANGELOG.md");
        List<String> headings = new ArrayList<>();
        Matcher h = Pattern.compile("^## (\\S+)\\s*$", Pattern.MULTILINE).matcher(changelog);
        while (h.find()) headings.add(h.group(1));
        assertFalse(headings.isEmpty(), "the changelog has one ## heading per version, newest first");
        for (Path prompt : prompts) {
            String text = Files.readString(prompt);
            Matcher m = Pattern.compile("^---\\n(.*?)\\n---\\n?(.*)$", Pattern.DOTALL).matcher(text);
            Map<String, Object> fm = m.matches() ? WorkflowLint.parseYaml(m.group(1)) : Map.of();
            String body = m.matches() ? m.group(2) : text;
            String version = str(fm.get("version")).equals("None") ? "" : str(fm.get("version"));
            assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"), prompt.getFileName() + " needs a semantic version in its frontmatter");
            assertEquals(version, headings.get(0), "the newest changelog entry (" + headings.get(0) + ") must match " + prompt.getFileName() + " (" + version + ")");
            assertFalse(body.isBlank());
        }
    }

    @Test
    void e3_theReviewGuidanceAndTheGitWorkflowAreFilesTheReviewerAndClaudeRead() {
        String review = read("REVIEW.md");
        for (String heading : List.of("Always check", "Skip")) {
            Matcher m = Pattern.compile("^## " + heading + "\\s*\\n(.*?)(?=^## |\\z)", Pattern.MULTILINE | Pattern.DOTALL).matcher(review);
            assertTrue(m.find(), "REVIEW.md needs a '## " + heading + "' section");
            Matcher bullets = Pattern.compile("^- .+$", Pattern.MULTILINE).matcher(m.group(1));
            int count = 0;
            while (bullets.find()) count++;
            assertTrue(count >= 2, "'" + heading + "' needs at least two bullets");
            assertTrue(Pattern.compile("`[\\w./*-]+/`|`[\\w./*-]+\\.\\w+`").matcher(m.group(1)).find(), "'" + heading + "' names a path in backticks");
        }
        String memory = read("CLAUDE.md");
        assertTrue(memory.contains("`feature/<ticket>`") && Pattern.compile("commit message", Pattern.CASE_INSENSITIVE).matcher(memory).find()
            && Pattern.compile("pull request", Pattern.CASE_INSENSITIVE).matcher(memory).find(), "CLAUDE.md states the branch name, the commit message and the pull request rule");
        assertTrue(Pattern.compile("never commit to `main`", Pattern.CASE_INSENSITIVE).matcher(memory).find());
    }

    @Test
    void e4_everyRunIsBoundedByTurnsTimeAndConcurrency() {
        for (String[] target : new String[][] {{".github/workflows/claude.yml", "claude"}, {".github/workflows/review.yml", "review"}}) {
            Workflow w = workflow(target[0]);
            Map<String, Object> job = asMap(w.jobs().get(target[1]));
            assertTrue(job.get("timeout-minutes") instanceof Integer t && t >= 1 && t <= 30, target[0] + ": a timeout of 30 minutes or less");
            Matcher turns = Pattern.compile("--max-turns (\\d+)").matcher(str(asMap(claudeStep(job).get("with")).get("claude_args")));
            assertTrue(turns.find() && Integer.parseInt(turns.group(1)) >= 1 && Integer.parseInt(turns.group(1)) <= 10, target[0] + ": --max-turns of 10 or fewer");
            Object group = asMap(w.map().get("concurrency")).get("group");
            assertTrue(group != null && !String.valueOf(group).isEmpty(), target[0] + ": a concurrency group");
        }
        assertEquals(Boolean.TRUE, asMap(workflow(".github/workflows/review.yml").map().get("concurrency")).get("cancel-in-progress"), "a new push replaces a running review");
    }

    @Test
    void e5_noFileHoldsAKeyAPersonalPathOrAnAddress() throws IOException {
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
