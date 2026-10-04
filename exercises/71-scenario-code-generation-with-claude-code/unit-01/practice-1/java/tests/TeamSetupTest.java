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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TeamSetupTest {
    // the folder that holds CLAUDE.md, .claude/ and docs/: the starter, the reference or a planted wrong solution
    private static final Path ROOT = Path.of(System.getProperty("solution.dir", "starter"));

    private static final String UI = "src/ui/Button.tsx", UI_TEST = "src/ui/Button.spec.tsx";
    private static final String HANDLER = "server/handlers/orders.ts", HANDLER_TEST = "server/handlers/orders.spec.ts";
    private static final String DB = "server/db/orderRepo.ts", DB_TEST = "server/db/orderRepo.spec.ts";
    private static final String DOC = "docs/readme.md";
    private static final List<String> SAMPLES = List.of(UI, UI_TEST, HANDLER, HANDLER_TEST, DB, DB_TEST, DOC);

    /** The words a convention is written with, the files it must reach and the files it must not reach. */
    record Area(String marker, List<String> expected, List<String> forbidden) {}

    private static final List<Area> AREAS = List.of(
        new Area("hooks", List.of(UI), List.of(HANDLER, DB, DOC)),
        new Area("async/await", List.of(HANDLER), List.of(UI, DB, DOC)),
        new Area("repository", List.of(DB), List.of(UI, HANDLER, DOC)),
        new Area("describe", List.of(UI_TEST, HANDLER_TEST, DB_TEST), List.of(UI, HANDLER, DB, DOC)));

    private record Rule(String name, String text, List<String> paths) {}

    private static Pattern globRegex(String glob) {
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

    private static String read(String rel) {
        Path path = ROOT.resolve(rel);
        assertTrue(Files.isRegularFile(path), rel + " is missing");
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String[] frontMatter(String text) {
        Matcher head = Pattern.compile("^---\\n(.*?)\\n---\\n", Pattern.DOTALL).matcher(text);
        return head.find() ? new String[] {head.group(1), text.substring(head.end())} : new String[] {"", text};
    }

    private static List<String> rulePaths(String head) {
        Matcher listed = Pattern.compile("^paths:\\s*\\n((?:[ \\t]+-[ \\t]+.*\\n?)+)", Pattern.MULTILINE).matcher(head + "\n");
        if (!listed.find()) return null;
        List<String> paths = new ArrayList<>();
        for (String line : listed.group(1).split("\n")) {
            if (!line.isBlank()) paths.add(line.replaceFirst("^\\s*-\\s+", "").strip().replaceAll("^[\"']|[\"']$", ""));
        }
        return paths;
    }

    private static List<Rule> rules() {
        Path folder = ROOT.resolve(".claude/rules");
        if (!Files.isDirectory(folder)) return List.of();
        try (Stream<Path> s = Files.list(folder)) {
            List<Rule> found = new ArrayList<>();
            for (Path p : s.filter(f -> f.toString().endsWith(".md")).sorted().toList()) {
                String text = Files.readString(p);
                found.add(new Rule(p.getFileName().toString(), text.toLowerCase(), rulePaths(frontMatter(text)[0])));
            }
            return found;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean reaches(List<String> paths, String file) {
        return paths == null || paths.stream().anyMatch(g -> globRegex(g).matcher(file).matches());
    }

    @Test
    void m1_eachConventionLoadsForExactlyTheFilesOfItsArea() {
        for (Area area : AREAS) {
            List<Rule> holders = rules().stream().filter(r -> r.text().contains(area.marker())).toList();
            assertFalse(holders.isEmpty(), "no rule file holds the '" + area.marker() + "' convention");
            for (String file : area.expected()) assertTrue(holders.stream().anyMatch(r -> reaches(r.paths(), file)), "the '" + area.marker() + "' convention does not load for " + file);
            for (String file : area.forbidden()) assertFalse(holders.stream().anyMatch(r -> reaches(r.paths(), file)), "the '" + area.marker() + "' convention loads for " + file + ", which is not its area");
        }
    }

    @Test
    void e1_theRootFileIsShortAndHoldsOnlyWhatEveryTaskNeeds() {
        String text = read("CLAUDE.md");
        assertTrue(text.lines().filter(l -> !l.isBlank()).count() <= 25, "keep the root file to 25 lines that matter");
        assertEquals(List.of(), AREAS.stream().map(Area::marker).filter(m -> text.toLowerCase().contains(m)).toList(), "area conventions belong in rule files, not in the root file");
    }

    @Test
    void e2_theReviewCommandIsSharedReadOnlyAndSaysWhatItDoes() {
        String[] parts = frontMatter(read(".claude/commands/review.md"));
        assertTrue(Pattern.compile("^description:\\s*\\S", Pattern.MULTILINE).matcher(parts[0]).find(), "the command needs a description");
        Matcher allowed = Pattern.compile("^allowed-tools:\\s*(.*)$", Pattern.MULTILINE).matcher(parts[0]);
        assertTrue(allowed.find(), "the command lists the tools it pre-approves");
        List<String> tools = new ArrayList<>();
        Matcher t = Pattern.compile("[^\\s,(]+(?:\\([^)]*\\))?").matcher(allowed.group(1));
        while (t.find()) tools.add(t.group());
        assertTrue(tools.contains("Read") && tools.stream().noneMatch(x -> List.of("Bash", "Edit", "Write", "MultiEdit").contains(x)), "a review reads: no bare Bash, no edits");
        assertTrue(parts[1].contains("git diff"), "the body says how to get the changes");
    }

    @Test
    void e3_theSettingsProtectTheEnvironmentFileAndApproveNoWholeTool() {
        JsonNode perms;
        try {
            perms = new ObjectMapper().readTree(read(".claude/settings.json")).path("permissions");
        } catch (IOException e) {
            throw new AssertionError(".claude/settings.json is not valid JSON: " + e.getMessage());
        }
        boolean denied = false;
        for (JsonNode n : perms.path("deny")) denied |= n.asText().equals("Read(./.env)");
        assertTrue(denied, "deny reading the environment file");
        for (JsonNode n : perms.path("allow")) assertFalse(List.of("Bash", "Bash(*)", "Edit", "Write").contains(n.asText()), "a bare allow rule approves every call of that tool");
    }

    @Test
    void e4_theModesTableSendsOpenDesignWorkToPlanModeAndClearSmallWorkToDirect() {
        Map<String, String> rows = new LinkedHashMap<>();
        for (String line : read("docs/working-modes.md").split("\n")) {
            String[] cells = line.strip().replaceAll("^\\||\\|$", "").split("\\|");
            if (line.startsWith("|") && cells.length >= 2 && List.of("plan", "direct").contains(cells[1].strip().toLowerCase())) rows.put(cells[0].strip().toLowerCase(), cells[1].strip().toLowerCase());
        }
        String[][] expected = {{"typo", "direct"}, {"monolith", "plan"}, {"validation", "direct"}, {"auth library", "plan"}, {"rename", "direct"}, {"unclear", "plan"}};
        for (String[] e : expected) {
            String task = rows.keySet().stream().filter(k -> k.contains(e[0])).findFirst().orElse(null);
            assertNotNull(task, "the table has no row for the '" + e[0] + "' task");
            assertEquals(e[1], rows.get(task), "'" + e[0] + "' should be " + e[1]);
        }
    }

    @Test
    void e5_everyRuleScopesItselfWithAGlobThatMatchesAFile() {
        List<Rule> found = rules();
        assertFalse(found.isEmpty(), "write the rule files");
        for (Rule r : found) {
            assertTrue(r.paths() != null && !r.paths().isEmpty(), r.name() + " has no paths list, so it loads in every session");
            assertTrue(r.paths().stream().allMatch(g -> g.contains("*")), r.name() + ": a path without * is not a glob (a bare folder name matches no file)");
            assertTrue(SAMPLES.stream().anyMatch(f -> reaches(r.paths(), f)), r.name() + " matches none of the sample files");
        }
    }

    @Test
    void e6_noFileHoldsAPersonalPathAnAddressOrAKey() throws IOException {
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
