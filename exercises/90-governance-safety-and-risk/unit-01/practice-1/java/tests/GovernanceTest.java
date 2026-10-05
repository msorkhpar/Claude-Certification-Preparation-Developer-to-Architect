import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GovernanceTest {
    // the folder that holds governance/ and docs/: the starter, the reference or a planted wrong solution
    private static final Path ROOT = Path.of(System.getProperty("solution.dir", "starter"));

    private static final Set<String> BAD_OWNERS = Set.of("", "tbd", "everyone", "team", "n/a", "none");

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

    private static JsonNode controls() {
        return loadJson("governance/controls.json").path("controls");
    }

    private static List<List<String>> registerRows() {
        List<List<String>> rows = new ArrayList<>();
        for (String line : read("docs/risk-register.md").split("\n")) {
            if (line.startsWith("|")) {
                List<String> cells = new ArrayList<>();
                for (String c : line.strip().replaceAll("^\\||\\|$", "").split("\\|", -1)) cells.add(c.strip());
                if (!cells.stream().allMatch(c -> c.matches("[-: ]*"))) rows.add(cells);
            }
        }
        return rows.subList(1, rows.size());
    }

    @Test
    void m1_noControlThatGuardsAnInputOrAnActionFailsOpen() {
        JsonNode found = controls();
        assertTrue(found.size() > 0, "list the controls");
        for (JsonNode c : found) {
            String id = c.path("id").asText();
            String onFailure = c.path("on_failure").asText();
            assertTrue(List.of("hold", "proceed-flagged").contains(onFailure), id + ": on_failure is hold or proceed-flagged");
            if (c.path("tier").asText().equals("high") || List.of("input", "action").contains(c.path("layer").asText())) {
                assertEquals("hold", onFailure, id + " guards an input or an action and must hold when it fails");
            }
        }
    }

    @Test
    void e1_everyHighConsequenceActionHasAHumanStepThatExistsAndHolds() {
        JsonNode data = loadJson("governance/controls.json");
        JsonNode actions = data.path("high_consequence_actions");
        assertTrue(actions.size() > 0, "name the high consequence actions");
        for (JsonNode a : actions) {
            String action = a.asText();
            String step = data.path("human_review").path(action).asText("");
            assertFalse(step.isEmpty(), action + " has no human review step");
            JsonNode control = null;
            for (JsonNode c : data.path("controls")) if (c.path("id").asText().equals(step)) control = c;
            assertNotNull(control, action + " names the control " + step + ", which is not defined");
            assertTrue(control.path("tier").asText().equals("high") && control.path("on_failure").asText().equals("hold"), step + " must be a high tier control that holds");
        }
    }

    @Test
    void e2_theAutomaticThresholdIsAtLeast95AndAHighConsequenceActionIsNeverAutomatic() {
        JsonNode routing = loadJson("governance/routing.json");
        JsonNode threshold = routing.path("auto_confidence_min");
        assertTrue(threshold.isInt() && threshold.asInt() >= 95 && threshold.asInt() <= 100, "auto_confidence_min is a whole number from 95 to 100");
        assertTrue(routing.path("high_consequence_auto").isBoolean() && !routing.path("high_consequence_auto").asBoolean(), "a high consequence action is never automatic");
        assertEquals("hold", routing.path("unsupported_answer").asText(), "an unsupported answer is held");
    }

    @Test
    void e3_retentionKeepsAtLeast90DaysWithinTheCeilingAndStoresNoContent() {
        JsonNode audit = loadJson("governance/retention.json").path("audit");
        int floor = audit.path("floor_days").asInt(0);
        int retain = audit.path("retain_days").asInt(-1);
        int ceiling = audit.path("ceiling_days").asInt(-1);
        assertTrue(floor >= 90, "the audit floor is at least 90 days");
        assertTrue(floor <= retain && retain <= ceiling, "retain between the floor and the ceiling");
        assertTrue(audit.path("store_content").isBoolean() && !audit.path("store_content").asBoolean(), "the audit log stores no content");
        assertTrue(audit.path("legal_hold_overrides_ceiling").asBoolean(false), "a legal hold outranks the ceiling");
    }

    @Test
    void e4_theRiskRegisterNamesAControlAndAnOwnerForEachOfFourFailureModes() {
        Set<String> ids = new HashSet<>();
        for (JsonNode c : controls()) ids.add(c.path("id").asText());
        List<List<String>> rows = registerRows();
        assertTrue(rows.size() >= 4, "the register has a row for each of four failure modes");
        StringBuilder modes = new StringBuilder();
        for (List<String> row : rows) {
            assertTrue(row.size() >= 5, row + " is missing a column");
            assertTrue(ids.contains(row.get(2)), "the control '" + row.get(2) + "' is not defined in controls.json");
            assertFalse(BAD_OWNERS.contains(row.get(3).toLowerCase()), row.get(1) + ": name an owner");
            modes.append(row.get(1).toLowerCase()).append(' ');
        }
        for (String word : List.of("hallucination", "prompt injection", "privacy", "unfair")) assertTrue(modes.toString().contains(word), "no row covers " + word);
    }

    @Test
    void e5_usersAreToldThatAiHelpedAndNoFileHoldsPersonalData() throws IOException {
        assertTrue(Pattern.compile("told that ai (helped|assisted)", Pattern.CASE_INSENSITIVE).matcher(read("docs/risk-register.md")).find(), "say that users are told that AI helped");
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

    @Test
    void e6_erasureRemovesTheVaultMappingWithin30Days() {
        JsonNode erasure = loadJson("governance/retention.json").path("erasure");
        assertTrue(erasure.path("remove_vault_mapping").asBoolean(false), "erasure removes the map from tokens to people");
        int days = erasure.path("max_days_to_complete").asInt(999);
        assertTrue(days >= 1 && days <= 30, "erasure completes within 30 days");
    }
}
