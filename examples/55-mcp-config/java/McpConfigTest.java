import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpConfigTest {
    private static Map<String, Object> json(String text) {
        return McpConfig.parse(text);
    }

    @Test
    void aVariableExpandsAndADefaultFillsAnUnsetOne() {
        assertEquals(new McpConfig.Expanded("x-fallback-", List.of()), McpConfig.expand("${A}-${B:-fallback}-${C:-}", Map.of("A", "x")));
    }

    @Test
    void anUnsetVariableWithoutADefaultKeepsItsTextAndIsReported() {
        assertEquals(new McpConfig.Expanded("Bearer ${TOKEN}", List.of("TOKEN")), McpConfig.expand("Bearer ${TOKEN}", Map.of()));
    }

    @Test
    void credentialVariablesReadEmptyTowardARemoteServerButNotForALocalOne() {
        Map<String, String> env = Map.of("NPM_TOKEN", "t", "MY_TOKEN", "m");
        assertEquals(new McpConfig.Expanded("Bearer ", List.of()), McpConfig.expand("Bearer ${NPM_TOKEN}", env, true));
        assertEquals(new McpConfig.Expanded("Bearer ", List.of()), McpConfig.expand("Bearer ${NPM_TOKEN:-d}", Map.of(), true));
        assertEquals(new McpConfig.Expanded("Bearer m", List.of()), McpConfig.expand("Bearer ${MY_TOKEN}", env, true));
        assertEquals(new McpConfig.Expanded("t", List.of()), McpConfig.expand("${NPM_TOKEN}", env, false));
    }

    @Test
    void expansionCoversCommandArgsEnvUrlAndHeadersOnly() {
        var entry = json("{\"type\": \"stdio\", \"command\": \"${BIN:-run}\", \"args\": [\"${A:-1}\"], \"env\": {\"K\": \"${V:-v}\"}, \"note\": \"${A}\"}");
        var out = McpConfig.expandServer(entry, Map.of());
        assertEquals(json("{\"type\": \"stdio\", \"command\": \"run\", \"args\": [\"1\"], \"env\": {\"K\": \"v\"}, \"note\": \"${A}\"}"), out.entry());
        assertEquals(List.of(), out.warnings());
    }

    @Test
    @SuppressWarnings("unchecked")
    void theHighestScopeWinsTheWholeEntryAndAConflictIsReported() {
        Map<String, Map<String, Map<String, Object>>> scopes = Map.of(
            "user", Map.of("s", json("{\"url\": \"u\", \"extra\": 1}")), "project", Map.of("s", json("{\"url\": \"p\"}")), "local", Map.of());
        var resolved = McpConfig.resolveServers(scopes);
        assertEquals(new McpConfig.Source("project", json("{\"url\": \"p\"}")), resolved.servers().get("s"));
        assertEquals(1, resolved.warnings().size());
        assertEquals("local", McpConfig.SCOPES.get(0));
    }

    @Test
    void lintFindsLiteralSecretsAndMissingFields() {
        var findings = McpConfig.lint(json("{\"mcpServers\": {\"a\": {\"type\": \"http\", \"headers\": {\"Authorization\": \"x\"}}, \"b\": {}}}"));
        assertEquals(List.of("a: a http server needs a url", "a: headers.Authorization holds a literal value, reference an environment variable", "b: a stdio server needs a command"), findings);
        assertEquals(List.of(), McpConfig.lint(json("{\"mcpServers\": {\"a\": {\"type\": \"http\", \"url\": \"u\", \"headers\": {\"Authorization\": \"Bearer ${T}\"}}}}")));
    }

    @Test
    void aDescriptionIsCutAtTheLimit() {
        assertEquals(2048, McpConfig.truncate("x".repeat(5000)).length());
    }

    @Test
    void onlyAnAllowRuleThatNamesItsServerIsHonouredAndDenyWins() {
        var s = json("{\"permissions\": {\"allow\": [\"mcp__docs__*\", \"mcp__*\", \"mcp__github__get_*\"], \"deny\": [\"mcp__github__get_secret\"]}}");
        List<String> got = List.of("mcp__docs__a", "mcp__other__a", "mcp__github__get_pr", "mcp__github__get_secret", "mcp__github__push").stream().map(t -> McpConfig.mcpDecision(s, t)).toList();
        assertEquals(List.of("allow", "ask", "allow", "deny", "ask"), got);
    }
}
