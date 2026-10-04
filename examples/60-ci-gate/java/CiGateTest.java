import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CiGateTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final CiGate.Policy POLICY = new CiGate.Policy("medium", List.of("style"), List.of("high"));

    private static Map<String, Object> finding(String key, Object value) {
        Map<String, Object> f = CiGate.finding("api.py", 12, "bug", "medium", "Unchecked None.", "Return early.", "missing-none-check");
        if (key != null) f.put(key, value);
        return f;
    }

    private static Map<String, Object> finding() {
        return finding(null, null);
    }

    private static CiGate.Decision run(List<Map<String, Object>> findings, int code) {
        return CiGate.gate(CiGate.envelope(Map.of("structured_output", Map.of("findings", findings))), code, CiGate.REVIEW_SCHEMA, POLICY);
    }

    private static JsonNode tree(Object o) {
        return JSON.valueToTree(o);
    }

    private static List<String> check(Object findings) {
        return CiGate.schemaCheck(tree(Map.of("findings", findings)), CiGate.REVIEW_SCHEMA);
    }

    @Test
    void theCommandIsHeadlessJsonSchemaCheckedBoundedAndReadOnly() throws Exception {
        List<String> argv = CiGate.buildCommand("Review.", CiGate.json("{\"type\": \"object\"}"));
        assertEquals(List.of("claude", "--bare", "-p"), argv.subList(0, 3));
        assertEquals("json", argv.get(argv.indexOf("--output-format") + 1));
        assertEquals(CiGate.json("{\"type\": \"object\"}"), CiGate.json(argv.get(argv.indexOf("--json-schema") + 1)));
        assertEquals("8", argv.get(argv.indexOf("--max-turns") + 1));
        assertEquals("Read,Grep,Glob,Bash(git diff *)", argv.get(argv.indexOf("--allowedTools") + 1));
        assertEquals(List.of(), CiGate.lintCommand(CiGate.join(argv)));
    }

    @Test
    void lintNamesWhatACarelessStepLacks() {
        assertEquals(List.of("no-print", "no-json", "no-schema", "no-turn-limit", "no-tool-list", "no-bare"), CiGate.lintCommand("claude 'Review it'"));
        assertTrue(CiGate.lintCommand("claude -p x --allowedTools Bash,Read").contains("wide-tools"));
        assertTrue(CiGate.lintCommand("claude -p x --allowedTools 'Read,Edit'").contains("wide-tools"));
        assertFalse(CiGate.lintCommand("claude -p x --allowedTools 'Bash(git diff *)'").contains("wide-tools"));
        assertTrue(CiGate.lintCommand("claude --bare -p x --output-format json --json-schema '{}' --max-turns 5 --allowedTools Read").contains("bare-without-context"));
        assertTrue(CiGate.lintCommand("claude -p x --max-turns 50").contains("no-turn-limit"));
        assertEquals(List.of("no-claude-command"), CiGate.lintCommand("no tool here"));
    }

    @Test
    void theSchemaCheckReadsTypesEnumsRequiredAndExtraKeys() {
        assertEquals(List.of(), check(List.of()));
        assertEquals(List.of("$.findings[0].line: expected integer"), check(List.of(finding("line", "12"))));
        assertTrue(check(List.of(finding("severity", "critical"))).get(0).startsWith("$.findings[0].severity: 'critical' is not one of"));
        assertEquals(List.of("$.findings[0].confidence: is not allowed"), check(List.of(finding("confidence", 0.9))));
    }

    @Test
    void aGoodRunPostsTheFindingsAboveTheFloorAndOutsideTheDisabledCategories() {
        Map<String, Object> style = finding("category", "style");
        Map<String, Object> low = finding("line", 3);
        low.put("severity", "low");
        CiGate.Decision d = run(List.of(finding(), style, low), 0);
        assertEquals(new CiGate.Decision(0, List.of(new CiGate.Comment("api.py", 12, "medium", "Unchecked None. Suggested fix: Return early.")), List.of()), d);
    }

    @Test
    void aHighFindingFailsTheJobAndNoFindingsPassIt() {
        Map<String, Object> high = finding("severity", "high");
        assertEquals(1, run(List.of(high), 0).exit());
        assertEquals("high", run(List.of(high), 0).comments().get(0).severity());
        assertEquals(0, run(List.of(), 0).exit());
    }

    @Test
    void aFailedRunFailsTheJobInsteadOfPassingItSilently() {
        Object[][] cases = {
            {CiGate.envelope(Map.of("subtype", "error_max_turns", "is_error", true)), 1, "error_max_turns"},
            {CiGate.envelope(Map.of("subtype", "error_max_structured_output_retries", "is_error", true)), 1, "retries"},
            {CiGate.envelope(), 0, "structured_output"},
            {"Error: no key", 1, "not a JSON"},
            {"[1]", 0, "not a JSON"}};
        for (Object[] c : cases) {
            CiGate.Decision d = CiGate.gate((String) c[0], (Integer) c[1], CiGate.REVIEW_SCHEMA, POLICY);
            assertEquals(1, d.exit(), (String) c[0]);
            assertEquals(List.of(), d.comments());
            assertTrue(d.problems().stream().anyMatch(p -> p.contains((String) c[2])), d.problems().toString());
        }
        assertTrue(CiGate.gate(CiGate.envelope(Map.of("structured_output", Map.of("findings", List.of()))), 2, CiGate.REVIEW_SCHEMA, POLICY).problems().get(0).contains("exited with status 2"));
    }
}
