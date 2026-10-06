import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorkflowLintTest {
    private fun rules(text: String) = lint(text).map { it.rule }.sorted()

    @Suppress("UNCHECKED_CAST")
    private fun at(node: Any?, vararg path: Any): Any? = path.fold(node) { n, key -> if (key is Int) (n as List<Any?>)[key] else (n as Map<String, Any?>)[key] }

    @Test
    fun theYamlReaderHandlesTheWorkflowShapes() {
        val wf = parseYaml(GOOD)
        assertEquals(listOf("opened", "synchronize"), at(wf, "on", "pull_request", "types"))
        assertEquals("--max-turns 8", at(wf, "jobs", "review", "steps", 1, "with", "claude_args"))
        assertEquals(15, at(wf, "jobs", "review", "timeout-minutes"))
        assertEquals(true, at(wf, "concurrency", "cancel-in-progress"))
    }

    @Test
    fun theGoodWorkflowHasNoFindings() {
        assertEquals(emptyList<Finding>(), lint(GOOD))
    }

    @Test
    fun theBadWorkflowIsFlaggedForEveryDocumentedRisk() {
        assertEquals(listOf("action-version", "literal-key", "literal-key", "no-checkout", "no-concurrency", "no-max-turns", "no-timeout", "review-writes", "unguarded-trigger"), rules(BAD))
    }

    @Test
    fun eachRuleIsIndependent() {
        assertEquals(listOf("no-checkout"), rules(GOOD.replace("      - uses: actions/checkout@v6\n        with:\n          fetch-depth: 1\n", "")))
        assertEquals(listOf("no-timeout"), rules(GOOD.replace("    timeout-minutes: 15\n", "")))
        assertEquals(listOf("no-max-turns"), rules(GOOD.replace("          claude_args: --max-turns 8\n", "")))
        assertEquals(listOf("no-permissions"), rules(GOOD.replace("    permissions:\n      contents: read\n      pull-requests: write\n      id-token: write\n", "")))
        assertEquals(listOf("review-writes"), rules(GOOD.replace("contents: read", "contents: write")))
        assertEquals(listOf("literal-key"), rules(GOOD.replace("\${{ secrets.ANTHROPIC_API_KEY }}", "abc")))
    }
}
