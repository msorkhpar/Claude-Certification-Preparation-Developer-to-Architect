import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RolloutTest {
    // the folder that holds managed/, .claude/ and docs/: the starter, the reference or a planted wrong solution
    private static final Path ROOT = Path.of(System.getProperty("solution.dir", "starter"));

    private static final List<String> MANAGED_ONLY = List.of("allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags");
    private static final List<String> EFFORT = List.of("low", "medium", "high", "xhigh", "max");
    private static final List<String> PRECEDENCE = List.of("managed settings", "command line", "project local", "shared project", "user");

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

    private static JsonNode managed() {
        return loadJson("managed/managed-settings.json");
    }

    private static String section(String title) {
        String text = read("docs/rollout.md");
        Matcher m = Pattern.compile("^## (.*)$", Pattern.MULTILINE).matcher(text);
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (m.find()) {
            titles.add(m.group(1).strip());
            starts.add(m.start());
            ends.add(m.end());
        }
        for (int i = 0; i < titles.size(); i++) {
            if (titles.get(i).equals(title)) return text.substring(ends.get(i), i + 1 < titles.size() ? starts.get(i + 1) : text.length()).strip();
        }
        throw new AssertionError("the section '" + title + "' is missing");
    }

    private static List<List<String>> table(String text) {
        List<List<String>> rows = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.startsWith("|")) {
                List<String> cells = new ArrayList<>();
                for (String c : line.strip().replaceAll("^\\||\\|$", "").split("\\|", -1)) cells.add(c.strip());
                if (!cells.stream().allMatch(c -> c.matches("[-: ]*"))) rows.add(cells);
            }
        }
        return rows.subList(1, rows.size());
    }

    private static List<Integer> limits(List<List<String>> rows, String level) {
        return rows.stream().filter(r -> r.get(0).equals(level)).map(r -> Integer.parseInt(r.get(2).replace(",", ""))).toList();
    }

    @Test
    void m1_theManagedFileLocksPermissionsAndProtectsTheEnvironmentFile() {
        JsonNode settings = managed();
        assertTrue(settings.path("allowManagedPermissionRulesOnly").asBoolean(false), "make managed settings the only source of permission rules");
        assertEquals("disable", settings.path("permissions").path("disableBypassPermissionsMode").asText(""), "turn off bypass mode");
        boolean denied = false;
        for (JsonNode d : settings.path("permissions").path("deny")) if (d.asText().equals("Read(./.env)")) denied = true;
        assertTrue(denied, "deny reading the environment file");
    }

    @Test
    void e1_keysOnlyAnOrganisationCanSetLiveInTheManagedFileAndNeverInTheProjectFile() {
        JsonNode project = loadJson(".claude/settings.json");
        for (String key : MANAGED_ONLY) assertFalse(project.has(key), key + " has no effect in the project file: move it to the managed file");
        assertTrue(managed().path("allowManagedHooksOnly").asBoolean(false), "only managed hooks run");
    }

    @Test
    void e2_pluginsComeOnlyFromTheCompanysMarketplaceAndCannotBeSideloaded() {
        JsonNode settings = managed();
        JsonNode sources = settings.path("strictKnownMarketplaces");
        assertTrue(sources.isArray() && sources.size() > 0, "allow the company's marketplace; an empty list blocks every source, the official one too");
        for (JsonNode entry : sources) {
            boolean ok = (entry.path("source").asText().equals("github") && entry.path("repo").asText("").startsWith("example-org/"))
                || (entry.path("source").asText().equals("url") && entry.path("url").asText("").startsWith("https://plugins.example.com/"));
            assertTrue(ok, entry + " is not a source the company owns");
        }
        assertTrue(settings.path("disableSideloadFlags").asBoolean(false), "reject the flags that load plugins from a folder or an address");
    }

    @Test
    void e3_onlyTheManagedMcpAllowlistAppliesAndAServerIsNeverAllowedAndDenied() {
        JsonNode settings = managed();
        assertTrue(settings.path("allowManagedMcpServersOnly").asBoolean(false), "ignore allowlists in user, project and local files");
        JsonNode allowed = settings.path("allowedMcpServers");
        assertTrue(allowed.size() > 0, "allow at least one server");
        Set<String> names = new HashSet<>();
        for (JsonNode entry : allowed) {
            Iterator<String> keys = entry.fieldNames();
            List<String> list = new ArrayList<>();
            keys.forEachRemaining(list::add);
            assertTrue(list.size() == 1 && List.of("serverName", "serverCommand", "serverUrl").contains(list.get(0)), entry + ": exactly one of serverName, serverCommand or serverUrl");
            if (entry.has("serverName")) {
                assertTrue(entry.path("serverName").asText().matches("[A-Za-z0-9_-]+"), entry.path("serverName").asText() + ": letters, numbers, hyphens and underscores only");
                names.add(entry.path("serverName").asText());
            }
        }
        Set<String> denied = new HashSet<>();
        for (JsonNode entry : settings.path("deniedMcpServers")) if (entry.has("serverName")) denied.add(entry.path("serverName").asText());
        Set<String> both = new HashSet<>(names);
        both.retainAll(denied);
        assertEquals(Set.of(), both, both + " is allowed and denied: the denial wins");
    }

    @Test
    void e4_theModelChoiceIsLockedByAListAndTheEffortCapIsAtMostHigh() {
        JsonNode settings = managed();
        JsonNode models = settings.path("availableModels");
        assertTrue(models.isArray() && models.size() > 0, "a managed model is only a default: list availableModels to lock the choice");
        if (settings.has("model")) {
            boolean listed = false;
            for (JsonNode m : models) if (m.asText().equals(settings.path("model").asText())) listed = true;
            assertTrue(listed, "the default model is one of the available models");
        }
        String cap = settings.path("maxEffortLevel").asText("");
        assertTrue(EFFORT.contains(cap) && EFFORT.indexOf(cap) <= EFFORT.indexOf("high"), "cap the effort at high or lower; max sets no cap");
    }

    @Test
    void e5_theGroupSpendLimitsAddUpToTheOrganisationLimitAndNoMore() {
        String text = section("Spend limits");
        assertTrue(text.toLowerCase().contains("usage credits"), "say that usage credits are on");
        List<List<String>> rows = table(text);
        List<Integer> org = limits(rows, "organization");
        List<Integer> groups = limits(rows, "group");
        List<Integer> members = limits(rows, "member");
        assertTrue(org.size() == 1 && !groups.isEmpty() && !members.isEmpty(), "list the organisation, its groups and the member default");
        assertTrue(groups.stream().mapToInt(Integer::intValue).sum() <= org.get(0), "the group limits add up to the organisation limit or less");
        assertTrue(members.get(0) <= Collections.min(groups), "a member's limit is within the smallest group's");
    }

    @Test
    void e6_adoptionIsMeasuredByOutcomesAgainstABaselineOfAtLeast4Weeks() {
        String text = section("Adoption");
        Matcher baseline = Pattern.compile("Baseline:\\s*(\\d+) weeks").matcher(text);
        assertTrue(baseline.find() && Integer.parseInt(baseline.group(1)) >= 4, "take a baseline of at least 4 weeks before the rollout");
        Matcher block = Pattern.compile("Outcome targets:\\n((?:- .*\\n?)+)").matcher(text + "\n");
        assertTrue(block.find(), "list the outcome targets");
        List<String> targets = new ArrayList<>();
        for (String line : block.group(1).split("\n")) if (!line.isEmpty()) targets.add(line.substring(2).toLowerCase());
        assertTrue(targets.size() >= 2, "set at least two outcome targets");
        for (String target : targets) {
            assertTrue(Stream.of("pull requests", "time to merge", "review", "defects").anyMatch(target::contains), "'" + target + "' is not an outcome");
            assertFalse(Stream.of("lines accepted", "prompts", "suggestions accepted").anyMatch(target::contains), "'" + target + "' measures activity");
        }
    }

    @Test
    void e7_thePrecedenceTableIsInTheDocumentedOrderAndNoFileHoldsPersonalData() throws IOException {
        List<String> levels = new ArrayList<>();
        for (List<String> row : table(section("Precedence"))) levels.add(row.get(1).toLowerCase());
        assertEquals(PRECEDENCE, levels, "the levels are managed, command line, project local, shared project and user, in that order");
        String doc = read("docs/rollout.md");
        assertTrue(doc.contains("per-group") && doc.contains("not yet supported"), "say that server-managed settings cannot target a group yet");
        List<String> hits = new ArrayList<>();
        Pattern home = Pattern.compile("(/home/\\w+|/Users/\\w+|C:\\\\Users)");
        Pattern email = Pattern.compile("[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+");
        try (Stream<Path> s = Files.walk(ROOT)) {
            for (Path p : s.filter(Files::isRegularFile).sorted().toList()) {
                String text = Files.readString(p);
                String rel = ROOT.relativize(p).toString();
                if (home.matcher(text).find()) hits.add(rel + ": home path");
                if (email.matcher(text).find()) hits.add(rel + ": email address");
            }
        }
        assertEquals(List.of(), hits);
    }
}
