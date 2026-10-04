import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsLayersTest {
    private static Map<String, Map<String, Object>> layers() {
        Map<String, Map<String, Object>> layers = new LinkedHashMap<>();
        layers.put("managed", SettingsLayers.json("{\"permissions\": {\"deny\": [\"Bash(curl *)\"]}}"));
        layers.put("user", SettingsLayers.json("{\"model\": \"sonnet\", \"permissions\": {\"allow\": [\"Bash(git status *)\"]}}"));
        layers.put("project", SettingsLayers.json("""
            {"model": "opus", "permissions": {"defaultMode": "bypassPermissions", "allow": ["Bash(npm run *)", "Bash(curl *)"], "ask": ["Bash(git push *)"], "deny": ["Read(./.env)"]}}"""));
        layers.put("local", SettingsLayers.json("{\"model\": \"haiku\", \"permissions\": {\"allow\": [\"Bash(git push *)\"]}}"));
        return layers;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> perms(Map<String, Object> settings) {
        return (Map<String, Object>) settings.get("permissions");
    }

    private static Map<String, Object> settings(String json) {
        return SettingsLayers.json(json);
    }

    private static List<String> decisions(Map<String, Object> s, String tool, String... args) {
        return java.util.Arrays.stream(args).map(a -> SettingsLayers.decide(s, tool, a)).toList();
    }

    @Test
    void aHigherLevelWinsAScalarKeyAndListsCombine() {
        Map<String, Object> s = SettingsLayers.effectiveSettings(layers(), true);
        assertEquals("haiku", s.get("model"));
        assertEquals(List.of("Bash(git status *)", "Bash(npm run *)", "Bash(curl *)", "Bash(git push *)"), perms(s).get("allow"));
    }

    @Test
    void aRepositoryFileCannotSetBypassPermissionsAndItsAllowRulesWaitForTrust() {
        assertFalse(perms(SettingsLayers.effectiveSettings(layers(), true)).containsKey("defaultMode"));
        assertEquals("ask", SettingsLayers.decide(SettingsLayers.effectiveSettings(layers(), false), "Bash", "npm run build"));
        assertEquals("allow", SettingsLayers.decide(SettingsLayers.effectiveSettings(layers(), true), "Bash", "npm run build"));
    }

    @Test
    void denyBeatsAskBeatsAllowWhateverTheLevel() {
        Map<String, Object> s = SettingsLayers.effectiveSettings(layers(), true);
        assertEquals("deny", SettingsLayers.decide(s, "Bash", "curl https://example.com"));
        assertEquals("ask", SettingsLayers.decide(s, "Bash", "git push origin main"));
        assertEquals("ask", SettingsLayers.decide(s, "Bash", "npm run build && git push origin main"));
        assertEquals("ask", SettingsLayers.decide(s, "Bash", "rm -rf build"));
    }

    @Test
    void wildcardsMatchTheBareCommandButNotALongerProgramName() {
        Map<String, Object> s = settings("{\"permissions\": {\"allow\": [\"Bash(ls *)\", \"Bash(npm run build)\"]}}");
        assertEquals(List.of("allow", "allow", "ask", "allow", "ask"), decisions(s, "Bash", "ls", "ls -la", "lsof", "npm run build", "npm run build --watch"));
    }

    @Test
    void pathRulesFollowTheRuleTypeAndAReadDenyAlsoBlocksEdits() {
        Map<String, Object> s = settings("{\"permissions\": {\"deny\": [\"Read(./.env)\", \"Read(secrets/**)\"], \"allow\": [\"Edit(src/**)\"]}}");
        assertEquals(List.of("deny", "deny", "deny", "deny", "allow"), decisions(s, "Read", "./.env", "sub/.env", "secrets/a.txt", "vendor/secrets/a.txt", "src/app.ts"));
        assertEquals(List.of("deny", "allow", "ask"), decisions(s, "Edit", ".env", "src/app.ts", "vendor/pkg/src/lib.js"));
    }

    @Test
    void memoryFilesLoadBroadToSpecificWithImportsAndWithoutBacktickedOnes() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("/m/CLAUDE.md", "m");
        files.put("/u/CLAUDE.md", "u");
        files.put("/r/CLAUDE.md", "See @docs/a.md and `@README`");
        files.put("/r/docs/a.md", "A, then @b.md");
        files.put("/r/docs/b.md", "B");
        files.put("/r/s/CLAUDE.md", "s");
        files.put("/r/s/CLAUDE.local.md", "l");
        files.put("/r/o/CLAUDE.md", "o");
        assertEquals(List.of("/m/CLAUDE.md", "/u/CLAUDE.md", "/r/CLAUDE.md", "/r/docs/a.md", "/r/docs/b.md", "/r/s/CLAUDE.md", "/r/s/CLAUDE.local.md"),
            SettingsLayers.loadMemory(files, "/r/s", "/m/CLAUDE.md", "/u/CLAUDE.md"));
    }

    @Test
    void acceptEditsModeAcceptsAnUndecidedEditButNotADenyOrACommand() {
        Map<String, Object> s = settings("{\"permissions\": {\"deny\": [\"Edit(.env)\"]}}");
        assertEquals("allow", SettingsLayers.decide(s, "Edit", "src/a.py", "acceptEdits"));
        assertEquals("deny", SettingsLayers.decide(s, "Edit", ".env", "acceptEdits"));
        assertEquals("ask", SettingsLayers.decide(s, "Bash", "make deploy", "acceptEdits"));
        assertEquals("ask", SettingsLayers.decide(s, "Edit", "src/a.py"));
    }
}
