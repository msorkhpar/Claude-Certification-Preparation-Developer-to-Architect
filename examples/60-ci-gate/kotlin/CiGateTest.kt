import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CiGateTest {
    private val mapper = ObjectMapper()
    private val policy = Policy("medium", listOf("style"), listOf("high"))

    private fun run(findings: List<Map<String, Any?>>, code: Int = 0) = gate(envelope(mapOf("structured_output" to mapOf("findings" to findings))), code, REVIEW_SCHEMA, policy)

    private fun check(findings: List<Map<String, Any?>>) = schemaCheck(mapper.valueToTree(mapOf("findings" to findings)), REVIEW_SCHEMA)

    @Test
    fun theCommandIsHeadlessJsonSchemaCheckedBoundedAndReadOnly() {
        val argv = buildCommand("Review.", json("""{"type": "object"}"""))
        assertEquals(listOf("claude", "--bare", "-p"), argv.take(3))
        assertEquals("json", argv[argv.indexOf("--output-format") + 1])
        assertEquals(json("""{"type": "object"}"""), json(argv[argv.indexOf("--json-schema") + 1]))
        assertEquals("8", argv[argv.indexOf("--max-turns") + 1])
        assertEquals("Read,Grep,Glob,Bash(git diff *)", argv[argv.indexOf("--allowedTools") + 1])
        assertEquals(emptyList<String>(), lintCommand(join(argv)))
    }

    @Test
    fun lintNamesWhatACarelessStepLacks() {
        assertEquals(listOf("no-print", "no-json", "no-schema", "no-turn-limit", "no-tool-list", "no-bare"), lintCommand("claude 'Review it'"))
        assertTrue("wide-tools" in lintCommand("claude -p x --allowedTools Bash,Read"))
        assertTrue("wide-tools" in lintCommand("claude -p x --allowedTools 'Read,Edit'"))
        assertFalse("wide-tools" in lintCommand("claude -p x --allowedTools 'Bash(git diff *)'"))
        assertTrue("bare-without-context" in lintCommand("claude --bare -p x --output-format json --json-schema '{}' --max-turns 5 --allowedTools Read"))
        assertTrue("no-turn-limit" in lintCommand("claude -p x --max-turns 50"))
        assertEquals(listOf("no-claude-command"), lintCommand("no tool here"))
    }

    @Test
    fun theSchemaCheckReadsTypesEnumsRequiredAndExtraKeys() {
        assertEquals(emptyList<String>(), check(emptyList()))
        assertEquals(listOf("$.findings[0].line: expected integer"), check(listOf(finding().also { it["line"] = "12" })))
        assertTrue(check(listOf(finding(severity = "critical")))[0].startsWith("$.findings[0].severity: 'critical' is not one of"))
        assertEquals(listOf("$.findings[0].confidence: is not allowed"), check(listOf(finding().also { it["confidence"] = 0.9 })))
    }

    @Test
    fun aGoodRunPostsTheFindingsAboveTheFloorAndOutsideTheDisabledCategories() {
        val d = run(listOf(finding(), finding(category = "style"), finding(line = 3, severity = "low")))
        assertEquals(Decision(0, listOf(Comment("api.py", 12, "medium", "Unchecked None. Suggested fix: Return early.")), emptyList()), d)
    }

    @Test
    fun aHighFindingFailsTheJobAndNoFindingsPassIt() {
        assertEquals(1, run(listOf(finding(severity = "high"))).exit)
        assertEquals("high", run(listOf(finding(severity = "high"))).comments[0].severity)
        assertEquals(0, run(emptyList()).exit)
    }

    @Test
    fun aFailedRunFailsTheJobInsteadOfPassingItSilently() {
        val cases = listOf(
            Triple(envelope(mapOf("subtype" to "error_max_turns", "is_error" to true)), 1, "error_max_turns"),
            Triple(envelope(mapOf("subtype" to "error_max_structured_output_retries", "is_error" to true)), 1, "retries"),
            Triple(envelope(), 0, "structured_output"),
            Triple("Error: no key", 1, "not a JSON"),
            Triple("[1]", 0, "not a JSON"),
        )
        for ((out, code, word) in cases) {
            val d = gate(out, code, REVIEW_SCHEMA, policy)
            assertEquals(1, d.exit, out)
            assertEquals(emptyList<Comment>(), d.comments)
            assertTrue(d.problems.any { word in it }, d.problems.toString())
        }
        assertTrue("exited with status 2" in gate(envelope(mapOf("structured_output" to mapOf("findings" to emptyList<Any>()))), 2, REVIEW_SCHEMA, policy).problems[0])
    }
}
