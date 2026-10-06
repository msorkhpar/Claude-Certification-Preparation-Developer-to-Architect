import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExtensionChoiceTest {
    private static ExtensionChoice.Choice choose(Map<String, Object> situation) {
        ExtensionChoice.Choice value = ExtensionChoice.choose(new LinkedHashMap<>(situation));
        assertNotNull(value, "choose returned nothing");
        return value;
    }

    private static boolean refused(Map<String, Object> situation) {
        try {
            choose(situation);
        } catch (IllegalArgumentException error) {
            return true;
        }
        return false;
    }

    private static Map<String, Object> s(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) m.put((String) keyValues[i], keyValues[i + 1]);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    private static Object[] axis(String key, Object... values) {
        Object[] out = new Object[values.length + 1];
        out[0] = key;
        System.arraycopy(values, 0, out, 1, values.length);
        return out;
    }

    /** Every combination of the axes, each added to the base situation. */
    private static List<Map<String, Object>> sweep(Map<String, Object> base, Object[]... axes) {
        List<Map<String, Object>> combos = new ArrayList<>(List.of(base));
        for (Object[] axis : axes) {
            List<Map<String, Object>> next = new ArrayList<>();
            for (Map<String, Object> c : combos)
                for (int i = 1; i < axis.length; i++) {
                    Map<String, Object> m = new LinkedHashMap<>(c);
                    m.put((String) axis[0], axis[i]);
                    next.add(m);
                }
            combos = next;
        }
        return combos;
    }

    // The scenario bank of the page: id, situation, expected mechanism and reason code.
    private static final Object[][] BANK = {
        {"s01", s("knowledge", "convention"), "claude-md", "always-known"},
        {"s02", s("guarantee", true, "knowledge", "convention"), "hook", "must-hold-every-time"},
        {"s03", s("guarantee", true), "hook", "must-hold-every-time"},
        {"s04", s("knowledge", "convention", "path_scoped", true), "path-rule", "scoped-convention"},
        {"s05", s("knowledge", "reference"), "skill", "on-demand-reference"},
        {"s06", s("knowledge", "procedure"), "skill", "repeatable-procedure"},
        {"s07", s("external_system", true), "mcp", "external-system"},
        {"s08", s("noisy", true), "subagent", "isolate-context"},
        {"s09", s(), "builtin-tool", "built-in-covers"},
        {"s10", s("external_system", true, "knowledge", "procedure", "repos", 6), "plugin", "shared-setup"},
        {"s11", s("knowledge", "procedure", "repos", 2), "plugin", "shared-setup"},
        {"s12", s("guarantee", true, "external_system", true), "hook", "must-hold-every-time"},
        {"s13", s("external_system", true, "noisy", true), "mcp", "external-system"},
        {"s14", s("knowledge", "convention", "repos", 6), "claude-md", "always-known"},
        {"s15", s("noisy", true, "repos", 4), "plugin", "shared-setup"},
        {"s16", s("surface", "api"), "api-tool", "own-schema-and-code"},
        {"s17", s("surface", "api", "builtin_covers", true), "builtin-tool", "provided-schema"},
        {"s18", s("surface", "api", "external_system", true, "remote_server", true), "mcp", "remote-server"},
        {"s19", s("timing", "interval"), "loop", "session-rhythm"},
        {"s20", s("timing", "interval", "presence", "away"), "routine", "runs-unattended"},
        {"s21", s("timing", "event"), "monitor", "push-not-poll"},
        {"s22", s("timing", "background"), "background-task", "work-while-it-runs"},
        {"s23", s("presence", "pipeline"), "headless-ci", "no-person-present"},
        {"s24", s("timing", "condition"), "goal", "until-condition-holds"},
        {"s25", s("personal", "voice"), "output-style", "response-voice"},
        {"s26", s("personal", "display"), "status-line", "personal-display"},
        {"s27", s("timing", "interval", "lasts_days", 30, "local_files", true), "desktop-task", "durable-and-local"},
        {"s28", s("personal", "keys"), "keybinding", "personal-keys"},
    };

    @Test
    void m1_everySituationOfTheBankGetsItsMechanismAndItsReason() {
        List<String> wrong = new ArrayList<>();
        for (Object[] row : BANK) if (!new ExtensionChoice.Choice((String) row[2], (String) row[3]).equals(choose(map(row[1])))) wrong.add((String) row[0]);
        assertEquals(List.of(), wrong, "these situations got the wrong mechanism or reason");
    }

    @Test
    void e1_aRuleThatMustHoldGoesToAHookWhateverElseIsTrue() {
        for (Map<String, Object> s : sweep(s("guarantee", true), axis("knowledge", "none", "convention", "reference", "procedure"), axis("external_system", false, true), axis("noisy", false, true), axis("path_scoped", false, true), axis("timing", "none", "interval", "event", "condition", "background"), axis("presence", "session", "pipeline", "away"), axis("personal", "none", "voice")))
            assertEquals(new ExtensionChoice.Choice("hook", "must-hold-every-time"), choose(s), s.toString());
    }

    @Test
    void e2_anOutsideSystemNeedsAServerAndNoisyWorkAloneNeedsASubagent() {
        for (Map<String, Object> s : sweep(s("external_system", true), axis("knowledge", "none", "convention", "reference", "procedure"), axis("noisy", false, true), axis("path_scoped", false, true), axis("timing", "none", "interval", "event", "condition", "background"), axis("presence", "session", "pipeline", "away")))
            assertEquals(new ExtensionChoice.Choice("mcp", "external-system"), choose(s), s.toString());
        for (Map<String, Object> s : sweep(s("noisy", true), axis("knowledge", "none", "convention", "reference", "procedure"), axis("path_scoped", false, true), axis("timing", "none", "interval", "event", "condition", "background"), axis("presence", "session", "pipeline", "away")))
            assertEquals(new ExtensionChoice.Choice("subagent", "isolate-context"), choose(s), s.toString());
    }

    @Test
    void e3_knowledgeGoesToTheFileOrSkillThatLoadsItAtTheRightTime() {
        assertEquals(new ExtensionChoice.Choice("claude-md", "always-known"), choose(s("knowledge", "convention")), "{\"knowledge\": \"convention\"}");
        assertEquals(new ExtensionChoice.Choice("path-rule", "scoped-convention"), choose(s("knowledge", "convention", "path_scoped", true)), "{\"knowledge\": \"convention\", \"path_scoped\": true}");
        assertEquals(new ExtensionChoice.Choice("skill", "on-demand-reference"), choose(s("knowledge", "reference")), "{\"knowledge\": \"reference\"}");
        assertEquals(new ExtensionChoice.Choice("skill", "repeatable-procedure"), choose(s("knowledge", "procedure")), "{\"knowledge\": \"procedure\"}");
        assertEquals(new ExtensionChoice.Choice("skill", "on-demand-reference"), choose(s("knowledge", "reference", "path_scoped", true)), "{\"knowledge\": \"reference\", \"path_scoped\": true}");
        assertEquals(new ExtensionChoice.Choice("builtin-tool", "built-in-covers"), choose(s("path_scoped", true)), "{\"path_scoped\": true}");
        assertEquals(new ExtensionChoice.Choice("skill", "repeatable-procedure"), choose(s("builtin_covers", true, "knowledge", "procedure")), "{\"builtin_covers\": true, \"knowledge\": \"procedure\"}");
    }

    @Test
    void e4_aPluginCarriesASkillHookSubagentOrServerToASecondRepositoryAndNothingElse() {
        assertEquals(new ExtensionChoice.Choice("skill", "repeatable-procedure"), choose(s("knowledge", "procedure", "repos", 1)), "{\"knowledge\": \"procedure\", \"repos\": 1}");
        assertEquals(new ExtensionChoice.Choice("hook", "must-hold-every-time"), choose(s("guarantee", true, "repos", 1)), "{\"guarantee\": true, \"repos\": 1}");
        assertEquals(new ExtensionChoice.Choice("mcp", "external-system"), choose(s("external_system", true, "repos", 1)), "{\"external_system\": true, \"repos\": 1}");
        assertEquals(new ExtensionChoice.Choice("subagent", "isolate-context"), choose(s("noisy", true, "repos", 1)), "{\"noisy\": true, \"repos\": 1}");
        assertEquals(new ExtensionChoice.Choice("plugin", "shared-setup"), choose(s("knowledge", "procedure", "repos", 2)), "{\"knowledge\": \"procedure\", \"repos\": 2}");
        assertEquals(new ExtensionChoice.Choice("plugin", "shared-setup"), choose(s("guarantee", true, "repos", 2)), "{\"guarantee\": true, \"repos\": 2}");
        assertEquals(new ExtensionChoice.Choice("plugin", "shared-setup"), choose(s("external_system", true, "repos", 3)), "{\"external_system\": true, \"repos\": 3}");
        assertEquals(new ExtensionChoice.Choice("plugin", "shared-setup"), choose(s("noisy", true, "repos", 2)), "{\"noisy\": true, \"repos\": 2}");
        assertEquals(new ExtensionChoice.Choice("claude-md", "always-known"), choose(s("knowledge", "convention", "repos", 2)), "{\"knowledge\": \"convention\", \"repos\": 2}");
        assertEquals(new ExtensionChoice.Choice("path-rule", "scoped-convention"), choose(s("knowledge", "convention", "path_scoped", true, "repos", 5)), "{\"knowledge\": \"convention\", \"path_scoped\": true, \"repos\": 5}");
        assertEquals(new ExtensionChoice.Choice("builtin-tool", "built-in-covers"), choose(s("repos", 9)), "{\"repos\": 9}");
    }

    @Test
    void e5_inAnApplicationThePlatformMaySupplyTheSchemaAndOnlyARemoteServerReplacesYourOwnTool() {
        assertEquals(new ExtensionChoice.Choice("api-tool", "own-schema-and-code"), choose(s("surface", "api")), "{\"surface\": \"api\"}");
        assertEquals(new ExtensionChoice.Choice("builtin-tool", "provided-schema"), choose(s("surface", "api", "builtin_covers", true, "external_system", true, "remote_server", true)), "{\"surface\": \"api\", \"builtin_covers\": true, \"external_system\": true, \"remote_server\": true}");
        assertEquals(new ExtensionChoice.Choice("mcp", "remote-server"), choose(s("surface", "api", "external_system", true, "remote_server", true)), "{\"surface\": \"api\", \"external_system\": true, \"remote_server\": true}");
        assertEquals(new ExtensionChoice.Choice("api-tool", "own-schema-and-code"), choose(s("surface", "api", "external_system", true)), "{\"surface\": \"api\", \"external_system\": true}");
        assertEquals(new ExtensionChoice.Choice("api-tool", "own-schema-and-code"), choose(s("surface", "api", "remote_server", true)), "{\"surface\": \"api\", \"remote_server\": true}");
        assertEquals(new ExtensionChoice.Choice("api-tool", "own-schema-and-code"), choose(s("surface", "api", "guarantee", true, "knowledge", "convention", "noisy", true, "repos", 4)), "{\"surface\": \"api\", \"guarantee\": true, \"knowledge\": \"convention\", \"noisy\": true, \"repos\": 4}");
    }

    @Test
    void e6_anUnknownValueIsAnErrorAndAMissingKeyTakesItsDefault() {
        assertEquals(new ExtensionChoice.Choice("builtin-tool", "built-in-covers"), choose(s()), "{}");
        assertTrue(refused(s("surface", "cli")), "{\"surface\": \"cli\"} must be an error");
        assertTrue(refused(s("knowledge", "tips")), "{\"knowledge\": \"tips\"} must be an error");
        assertTrue(refused(s("repos", 0)), "{\"repos\": 0} must be an error");
        assertTrue(refused(s("repos", -1)), "{\"repos\": -1} must be an error");
        assertTrue(refused(s("surface", "api", "knowledge", "tips")), "{\"surface\": \"api\", \"knowledge\": \"tips\"} must be an error");
        assertTrue(refused(s("timing", "weekly")), "{\"timing\": \"weekly\"} must be an error");
        assertTrue(refused(s("presence", "cloud")), "{\"presence\": \"cloud\"} must be an error");
        assertTrue(refused(s("personal", "theme")), "{\"personal\": \"theme\"} must be an error");
        assertTrue(refused(s("lasts_days", 0)), "{\"lasts_days\": 0} must be an error");
        assertTrue(refused(s("presence", "away", "local_files", true, "timing", "interval")), "{\"presence\": \"away\", \"local_files\": true, \"timing\": \"interval\"} must be an error");
        assertTrue(refused(s("surface", "api", "timing", "weekly")), "{\"surface\": \"api\", \"timing\": \"weekly\"} must be an error");
    }

    @Test
    void e7_aPipelineAConditionALongCommandAndAnEventAreNotIntervals() {
        assertEquals(new ExtensionChoice.Choice("headless-ci", "no-person-present"), choose(s("presence", "pipeline")), "{\"presence\": \"pipeline\"}");
        assertEquals(new ExtensionChoice.Choice("headless-ci", "no-person-present"), choose(s("presence", "pipeline", "timing", "interval", "lasts_days", 30)), "{\"presence\": \"pipeline\", \"timing\": \"interval\", \"lasts_days\": 30}");
        assertEquals(new ExtensionChoice.Choice("goal", "until-condition-holds"), choose(s("timing", "condition", "lasts_days", 30)), "{\"timing\": \"condition\", \"lasts_days\": 30}");
        assertEquals(new ExtensionChoice.Choice("goal", "until-condition-holds"), choose(s("timing", "condition", "presence", "away")), "{\"timing\": \"condition\", \"presence\": \"away\"}");
        assertEquals(new ExtensionChoice.Choice("background-task", "work-while-it-runs"), choose(s("timing", "background", "presence", "away")), "{\"timing\": \"background\", \"presence\": \"away\"}");
        assertEquals(new ExtensionChoice.Choice("monitor", "push-not-poll"), choose(s("timing", "event")), "{\"timing\": \"event\"}");
        assertEquals(new ExtensionChoice.Choice("routine", "runs-unattended"), choose(s("timing", "event", "presence", "away")), "{\"timing\": \"event\", \"presence\": \"away\"}");
    }

    @Test
    void e8_anIntervalIsALoopInTheSessionAndARoutineOrDesktopTaskWhenItMustOutliveIt() {
        assertEquals(new ExtensionChoice.Choice("loop", "session-rhythm"), choose(s("timing", "interval", "lasts_days", 7)), "{\"timing\": \"interval\", \"lasts_days\": 7}");
        assertEquals(new ExtensionChoice.Choice("routine", "runs-unattended"), choose(s("timing", "interval", "lasts_days", 8)), "{\"timing\": \"interval\", \"lasts_days\": 8}");
        assertEquals(new ExtensionChoice.Choice("desktop-task", "durable-and-local"), choose(s("timing", "interval", "lasts_days", 8, "local_files", true)), "{\"timing\": \"interval\", \"lasts_days\": 8, \"local_files\": true}");
        assertEquals(new ExtensionChoice.Choice("loop", "session-rhythm"), choose(s("timing", "interval", "local_files", true)), "{\"timing\": \"interval\", \"local_files\": true}");
        assertEquals(new ExtensionChoice.Choice("routine", "runs-unattended"), choose(s("timing", "interval", "presence", "away", "lasts_days", 30)), "{\"timing\": \"interval\", \"presence\": \"away\", \"lasts_days\": 30}");
        assertEquals(new ExtensionChoice.Choice("routine", "runs-unattended"), choose(s("timing", "interval", "presence", "away")), "{\"timing\": \"interval\", \"presence\": \"away\"}");
        assertEquals(new ExtensionChoice.Choice("loop", "session-rhythm"), choose(s("timing", "interval", "presence", "session")), "{\"timing\": \"interval\", \"presence\": \"session\"}");
    }

    @Test
    void e9_aPersonalPreferenceGoesToAStyleAStatusLineOrAKeyBindingAndKnowledgeKeepsItsOwnRules() {
        assertEquals(new ExtensionChoice.Choice("output-style", "response-voice"), choose(s("personal", "voice")), "{\"personal\": \"voice\"}");
        assertEquals(new ExtensionChoice.Choice("status-line", "personal-display"), choose(s("personal", "display")), "{\"personal\": \"display\"}");
        assertEquals(new ExtensionChoice.Choice("keybinding", "personal-keys"), choose(s("personal", "keys")), "{\"personal\": \"keys\"}");
        assertEquals(new ExtensionChoice.Choice("output-style", "response-voice"), choose(s("personal", "voice", "knowledge", "convention")), "{\"personal\": \"voice\", \"knowledge\": \"convention\"}");
        assertEquals(new ExtensionChoice.Choice("keybinding", "personal-keys"), choose(s("personal", "keys", "knowledge", "reference", "repos", 4)), "{\"personal\": \"keys\", \"knowledge\": \"reference\", \"repos\": 4}");
        assertEquals(new ExtensionChoice.Choice("status-line", "personal-display"), choose(s("personal", "display", "repos", 3)), "{\"personal\": \"display\", \"repos\": 3}");
        assertEquals(new ExtensionChoice.Choice("claude-md", "always-known"), choose(s("personal", "none", "knowledge", "convention")), "{\"personal\": \"none\", \"knowledge\": \"convention\"}");
        assertEquals(new ExtensionChoice.Choice("loop", "session-rhythm"), choose(s("personal", "voice", "timing", "interval")), "{\"personal\": \"voice\", \"timing\": \"interval\"}");
        assertEquals(new ExtensionChoice.Choice("api-tool", "own-schema-and-code"), choose(s("personal", "voice", "surface", "api")), "{\"personal\": \"voice\", \"surface\": \"api\"}");
    }
}
