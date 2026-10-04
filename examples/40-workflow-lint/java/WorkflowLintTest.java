import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowLintTest {
    private static List<String> rules(String text) throws IOException {
        return WorkflowLint.lint(text).stream().map(WorkflowLint.Finding::rule).sorted().toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> T at(Object node, Object... path) {
        for (Object key : path) node = key instanceof Integer i ? ((List<Object>) node).get(i) : ((Map<String, Object>) node).get(key);
        return (T) node;
    }

    @Test
    void theYamlReaderHandlesTheWorkflowShapes() throws IOException {
        Map<String, Object> wf = WorkflowLint.parseYaml(WorkflowLint.GOOD);
        assertEquals(List.of("opened", "synchronize"), at(wf, "on", "pull_request", "types"));
        assertEquals("--max-turns 8", at(wf, "jobs", "review", "steps", 1, "with", "claude_args"));
        assertEquals(15, (Integer) at(wf, "jobs", "review", "timeout-minutes"));
        assertEquals(true, at(wf, "concurrency", "cancel-in-progress"));
    }

    @Test
    void theGoodWorkflowHasNoFindings() throws IOException {
        assertEquals(List.of(), WorkflowLint.lint(WorkflowLint.GOOD));
    }

    @Test
    void theBadWorkflowIsFlaggedForEveryDocumentedRisk() throws IOException {
        assertEquals(List.of("action-version", "literal-key", "literal-key", "no-checkout", "no-concurrency", "no-max-turns", "no-timeout", "review-writes", "unguarded-trigger"),
            rules(WorkflowLint.BAD));
    }

    @Test
    void eachRuleIsIndependent() throws IOException {
        String good = WorkflowLint.GOOD;
        assertEquals(List.of("no-checkout"), rules(good.replace("      - uses: actions/checkout@v6\n        with:\n          fetch-depth: 1\n", "")));
        assertEquals(List.of("no-timeout"), rules(good.replace("    timeout-minutes: 15\n", "")));
        assertEquals(List.of("no-max-turns"), rules(good.replace("          claude_args: --max-turns 8\n", "")));
        assertEquals(List.of("no-permissions"), rules(good.replace("    permissions:\n      contents: read\n      pull-requests: write\n      id-token: write\n", "")));
        assertEquals(List.of("review-writes"), rules(good.replace("contents: read", "contents: write")));
        assertEquals(List.of("literal-key"), rules(good.replace("${{ secrets.ANTHROPIC_API_KEY }}", "abc")));
    }
}
