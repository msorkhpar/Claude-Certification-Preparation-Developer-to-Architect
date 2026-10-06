import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SkillSetupTest {
    // config.dir is the project folder that holds .claude/ and personal/: starter, reference or a planted wrong solution.
    private static final Path ROOT = Path.of(System.getProperty("config.dir", "starter"));
    private static final String REVIEW = ".claude/skills/review-pr/SKILL.md", TAG = ".claude/skills/release-tag/SKILL.md", STANDUP = ".claude/commands/standup.md", MINE = "personal/review-pr-mine/SKILL.md";
    private static final List<String> ALL = List.of(REVIEW, TAG, STANDUP, MINE);

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static SkillModel.Parsed load(String rel) {
        try {
            return SkillModel.parse(read(rel));
        } catch (IOException e) {
            throw new AssertionError(rel + " is not readable: " + e.getMessage());
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static boolean has(String regex, String text) {
        return Pattern.compile(regex).matcher(text).find();
    }

    @Test
    void m1_theReviewSkillIsAForkedSkillWithAnExplicitTaskAndAnArgument() {
        SkillModel.Parsed p = load(REVIEW);
        assertNotNull(SkillModel.forkAgent(p.meta()), "a review that prints a lot belongs in a forked context: set context: fork");
        assertFalse(str(p.meta().get("agent")).isBlank(), "name the subagent type in agent");
        assertTrue(Pattern.compile("^1\\. ", Pattern.MULTILINE).matcher(p.body()).find(), "a forked skill is given its content as the task: write numbered steps, not guidelines");
        assertFalse(str(p.meta().get("argument-hint")).isBlank(), "show what to type with argument-hint");
        assertTrue(has("\\$0|\\$ARGUMENTS", p.body()), "use the pull request number in the steps with $0");
    }

    @Test
    void e1_theReleaseSkillIsStartedOnlyByAPersonAndPreApprovesPatterns() {
        Map<String, Object> meta = load(TAG).meta();
        Map<String, Boolean> expected = new LinkedHashMap<>();
        expected.put("you", true);
        expected.put("claude", false);
        expected.put("description_in_context", false);
        assertEquals(expected, SkillModel.invocation(meta), "a skill with side effects sets disable-model-invocation: true");
        List<String> items = new ArrayList<>();
        Matcher m = Pattern.compile("[^\\s,(]+(?:\\([^)]*\\))?").matcher(str(meta.get("allowed-tools")));
        while (m.find()) items.add(m.group());
        assertTrue(!items.isEmpty() && items.stream().allMatch(i -> i.contains("(")), "pre-approve patterns such as Bash(git tag *), never a whole tool");
        assertTrue(SkillModel.preApproved(meta, "Bash", "git tag -a v1.2.0 -m x") && SkillModel.preApproved(meta, "Bash", "git push origin v1.2.0"));
        assertTrue(!SkillModel.preApproved(meta, "Bash", "git push --force origin main") && !SkillModel.preApproved(meta, "Bash", "rm -rf build"));
    }

    @Test
    void e2_toolsAreTakenAwayWithDisallowedToolsAndAllowedToolsOnlyPreApproves() {
        Map<String, Object> meta = load(REVIEW).meta();
        assertTrue(SkillModel.toolStatus(meta, "Edit").equals("removed") && SkillModel.toolStatus(meta, "Write").equals("removed"), "list Edit and Write by bare name in disallowed-tools");
        assertNotEquals("removed", SkillModel.toolStatus(meta, "Read"), "only the tools that change things are removed");
        assertTrue(SkillModel.preApproved(meta, "Bash", "gh pr diff 12") && SkillModel.preApproved(meta, "Bash", "gh pr view 12"));
        assertTrue(!SkillModel.preApproved(meta, "Bash", "gh pr merge 12") && !SkillModel.preApproved(meta, "Bash", "rm -rf build"), "allowed-tools pre-approves the read-only gh commands and no more");
    }

    @Test
    void e3_argumentsFillThePlaceholdersOfEveryFile() {
        String out = SkillModel.render(load(REVIEW).body(), "123", List.of());
        assertTrue(!out.contains("$0") && out.contains("gh pr diff 123") && !out.contains("ARGUMENTS:"));
        SkillModel.Parsed tag = load(TAG);
        Object names = tag.meta().get("arguments");
        assertEquals(List.of("version"), names, "declare the named argument in arguments");
        out = SkillModel.render(tag.body(), "v1.4.0", List.of("version"));
        assertTrue(!out.contains("$version") && out.contains("git tag -a v1.4.0 -m \"Release v1.4.0\"") && !out.contains("ARGUMENTS:"));
        out = SkillModel.render(load(STANDUP).body(), "ana");
        assertTrue(!out.contains("$ARGUMENTS") && out.contains("by ana since") && !out.contains("ARGUMENTS:"));
    }

    @Test
    void e4_everyFileCreatesItsOwnSlashCommandAndThePersonalVariantHasANewName() {
        Map<String, String> names = new LinkedHashMap<>();
        for (String rel : ALL) names.put(rel, SkillModel.commandName(rel, load(rel).meta()));
        assertTrue(names.get(STANDUP).equals("standup") && names.get(REVIEW).equals("review-pr") && names.get(TAG).equals("release-tag"));
        assertEquals(4, new LinkedHashSet<>(names.values()).size(), "two files create one command: " + names);
        assertNotEquals("review-pr", names.get(MINE), "a personal skill with the team's name replaces it for you: give the variant its own name");
        Map<String, Object> meta = load(STANDUP).meta();
        assertTrue(!str(meta.get("description")).isBlank() && !str(meta.get("argument-hint")).isBlank(), "the old command file keeps working: give it a description and a hint");
    }

    private static String strip(String s, char c) {
        int from = 0, to = s.length();
        while (from < to && s.charAt(from) == c) from++;
        while (to > from && s.charAt(to - 1) == c) to--;
        return s.substring(from, to);
    }

    @Test
    void e5_eachPieceOfGuidanceLivesWhereItLoadsTheWayItIsUsed() {
        Map<String, String> rows = new LinkedHashMap<>();
        for (String line : read("docs/placement.md").lines().toList()) {
            List<String> cells = new ArrayList<>();
            for (String c : strip(strip(line.strip(), '|'), '|').split("\\|", -1)) cells.add(strip(c.strip(), '`').strip());
            if (cells.size() == 2 && !cells.get(0).equals("Guidance") && !cells.get(0).equals("---")) rows.put(cells.get(0), cells.get(1));
        }
        Map<String, String> want = new LinkedHashMap<>();
        want.put("The team's pull request review checklist, run on demand", ".claude/skills/review-pr/SKILL.md");
        want.put("My own variant of that review with extra style notes", "~/.claude/skills/review-pr-mine/SKILL.md");
        want.put("Coding standards that apply to every task in the repository", "CLAUDE.md");
        want.put("Test file conventions for test files in many folders", ".claude/rules/testing.md");
        want.put("A release procedure with side effects that only a person starts", ".claude/skills/release-tag/SKILL.md");
        assertEquals(want, rows);
    }

    @Test
    void e6_everySkillSaysWhenToUseItAndStaysInsideTheListingBudget() {
        for (String rel : List.of(REVIEW, TAG, MINE)) {
            Map<String, Object> meta = load(rel).meta();
            String description = str(meta.get("description"));
            assertTrue(has("(?:^|\\. )Use when\\b", description), rel + ": the description starts a sentence with Use when");
            assertTrue(description.length() + str(meta.get("when_to_use")).length() <= 1536, rel + ": the listing keeps 1,536 characters of description and when_to_use");
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
