import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class CiReviewTest {
    // config.dir is the project folder that holds the workflow, CLAUDE.md, the schema and the gate: starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))
    private val mapper = ObjectMapper()
    private val schemaOfTheModel: JsonNode = REVIEW_SCHEMA

    private fun policy(failOn: List<String>): Map<String, Any?> = linkedMapOf("min_severity" to "medium", "disabled_categories" to listOf("style"), "fail_on" to failOn)

    private val defaultPolicy = policy(listOf("high"))
    private val vague = listOf("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment")

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun finding(vararg overrides: Pair<String, Any?>): MutableMap<String, Any?> {
        val f = linkedMapOf<String, Any?>(
            "file" to "api.py", "line" to 12, "category" to "bug", "severity" to "medium", "issue" to "Unchecked None.", "suggested_fix" to "Return early.", "detected_pattern" to "missing-none-check",
        )
        f.putAll(overrides)
        return f
    }

    private fun run(findings: List<Map<String, Any?>>, code: Int = 0, policy: Map<String, Any?> = defaultPolicy, over: Map<String, Any?> = emptyMap()): Map<String, Any?> {
        val result = ReviewGate.gate(envelope(mapOf("structured_output" to mapOf("findings" to findings)) + over), code, schemaOfTheModel, policy)
        assertNotNull(result, "gate returned nothing")
        return result!!
    }

    private fun decision(exit: Int, comments: List<Map<String, Any?>>, problems: List<String>): Map<String, Any?> = mapOf("exit" to exit, "problems" to problems, "comments" to comments)

    private fun comment(file: String, line: Int, severity: String, body: String): Map<String, Any?> = mapOf("file" to file, "line" to line, "severity" to severity, "body" to body)

    @Suppress("UNCHECKED_CAST")
    private fun comments(result: Map<String, Any?>) = result["comments"] as List<Map<String, Any?>>

    @Suppress("UNCHECKED_CAST")
    private fun problems(result: Map<String, Any?>) = result["problems"] as List<String>

    private fun has(regex: String, text: String) = Regex(regex).containsMatchIn(text)

    @Test
    fun m1_aValidRunPostsTheFindingsAboveTheFloorAndOutsideTheDisabledCategories() {
        val result = run(
            listOf(
                finding(), finding("category" to "style", "severity" to "medium"), finding("line" to 3, "severity" to "low"),
                finding("file" to "ui.py", "line" to 7, "category" to "security", "severity" to "medium", "issue" to "Unescaped input.", "suggested_fix" to "Escape it."),
            ),
        )
        assertEquals(decision(0, listOf(comment("api.py", 12, "medium", "Unchecked None. Suggested fix: Return early."), comment("ui.py", 7, "medium", "Unescaped input. Suggested fix: Escape it.")), emptyList()), result)
        assertEquals(decision(0, emptyList(), emptyList()), run(emptyList()))
    }

    @Test
    fun e1_aFailedRunFailsTheJobInsteadOfPassingItSilently() {
        val cases = listOf(
            Triple(envelope(mapOf("subtype" to "error_max_turns", "is_error" to true)), 1, "error_max_turns"),
            Triple(envelope(mapOf("subtype" to "error_max_structured_output_retries", "is_error" to true)), 1, "retries"),
            Triple(envelope(), 0, "structured_output"),
            Triple(envelope(mapOf("is_error" to true, "structured_output" to mapOf("findings" to emptyList<Any>()))), 0, "ended"),
            Triple("Error: no key", 1, "not a JSON"), Triple("[1]", 0, "not a JSON"), Triple("", 0, "not a JSON"),
        )
        for ((out, code, word) in cases) {
            val result = ReviewGate.gate(out, code, schemaOfTheModel, defaultPolicy)
            assertNotNull(result)
            assertTrue(result!!["exit"] == 1 && comments(result).isEmpty() && problems(result).any { word in it }, "$out -> $result")
        }
        val failed = run(emptyList(), code = 2)
        assertTrue(failed["exit"] == 1 && problems(failed).any { "exited with status 2" in it }, failed.toString())
    }

    @Test
    fun e2_anAnswerThatBreaksTheSchemaFailsTheJobAndNamesThePathOfTheProblem() {
        val cases = listOf(
            listOf(finding("line" to "12")) to "$.findings[0].line", listOf(finding("severity" to "critical")) to "$.findings[0].severity",
            listOf(finding().also { it.remove("file") }) to "$.findings[0].file", listOf(finding("confidence" to 0.9)) to "$.findings[0].confidence",
        )
        for ((bad, path) in cases) {
            val result = run(bad)
            assertTrue(result["exit"] == 1 && comments(result).isEmpty() && problems(result).any { it.startsWith("schema $path") }, "$bad -> $result")
        }
    }

    @Test
    fun e3_aFindingAtTheFailingSeverityBlocksTheMergeAndLowerOnesOnlyComment() {
        val blocked = run(listOf(finding("severity" to "high"), finding("line" to 30)))
        assertEquals(1, blocked["exit"])
        assertEquals(listOf("high", "medium"), comments(blocked).map { it["severity"] })
        assertEquals(emptyList<String>(), problems(blocked))
        assertEquals(0, run(listOf(finding("severity" to "medium")))["exit"])
        assertEquals(1, run(listOf(finding("severity" to "medium")), policy = policy(listOf("medium", "high")))["exit"])
        assertEquals(0, run(listOf(finding("severity" to "high", "category" to "style")))["exit"], "a disabled category cannot block")
    }

    @Test
    fun e4_thePromptListsEarlierFindingsAndExistingTestsAndAsksForNewOrUnaddressedIssuesOnly() {
        val prior = listOf(mapOf("file" to "api.py", "line" to 12, "category" to "bug", "issue" to "Unchecked None."), mapOf("file" to "ui.py", "line" to 7, "category" to "security", "issue" to "Unescaped input."))
        val full = ReviewGate.reviewPrompt("+ x = 1", prior, listOf("test_empty_cart", "test_two_items"))
        assertEquals(
            listOf(
                "<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed.",
                "Do not repeat a finding listed in <already_reported>.", "Do not suggest a test for a behaviour that an existing test in <existing_tests> already covers.", "</instructions>",
                "<already_reported>", "- api.py:12 [bug] Unchecked None.", "- ui.py:7 [security] Unescaped input.", "</already_reported>", "<existing_tests>", "- test_empty_cart", "- test_two_items",
                "</existing_tests>", "<diff>", "+ x = 1", "</diff>",
            ).joinToString("\n"),
            full,
        )
        val first = ReviewGate.reviewPrompt("+ x = 1", emptyList(), emptyList())
        assertEquals(
            listOf("<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed.", "</instructions>", "<diff>", "+ x = 1", "</diff>").joinToString("\n"),
            first,
        )
    }

    private fun keywords(node: JsonNode): Set<String> {
        val found = mutableSetOf<String>()
        if (node.isObject) {
            for ((k, v) in node.fields()) {
                if (k == "properties" && v.isObject) v.forEach { found += keywords(it) } else {
                    found += k
                    found += keywords(v)
                }
            }
        } else if (node.isArray) node.forEach { found += keywords(it) }
        return found
    }

    private fun texts(array: JsonNode?): Set<String> = array?.map { it.asText() }?.toSet() ?: emptySet()

    private fun tree(o: Any?): JsonNode = mapper.valueToTree(o)

    @Test
    fun e5_theSchemaFileIsValidDraft07AndRequiresEveryFieldOfAFinding() {
        val schema = try { mapper.readTree(read("review-schema.json")) } catch (error: IOException) { throw AssertionError("review-schema.json is not valid JSON: ${error.message}") }
        assertEquals("http://json-schema.org/draft-07/schema#", schema.path("\$schema").asText("http://json-schema.org/draft-07/schema#"), "the SDK validates draft-07 and rejects a newer \$schema")
        val banned = setOf("minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf", "minLength", "maxLength")
        assertTrue(keywords(schema).none { it in banned }, "structured outputs do not support numeric or string constraints")
        val items = schema.path("properties").path("findings").path("items")
        assertEquals(setOf("file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"), texts(items.get("required")), "every field of a finding is required")
        assertTrue(
            items.path("additionalProperties").let { it.isBoolean && !it.asBoolean() } && schema.path("additionalProperties").let { it.isBoolean && !it.asBoolean() } && tree(listOf("findings")) == schema.get("required"),
        )
        val props = items.path("properties")
        assertEquals(setOf("low", "medium", "high"), texts(props.path("severity").get("enum")))
        assertTrue(texts(props.path("category").get("enum")).containsAll(setOf("bug", "security", "style", "other")))
        assertEquals("integer", props.path("line").path("type").asText(null))
        assertEquals(emptyList<String>(), SchemaCheck.schemaCheck(tree(mapOf("findings" to listOf(finding()))), schema))
        assertEquals(emptyList<String>(), SchemaCheck.schemaCheck(tree(mapOf("findings" to emptyList<Any>())), schema))
        for (bad in listOf(finding().also { it.remove("detected_pattern") }, finding("severity" to "critical"), finding("line" to "12"), finding("confidence" to 0.9))) {
            assertTrue(SchemaCheck.schemaCheck(tree(mapOf("findings" to listOf(bad))), schema).isNotEmpty(), "the schema accepted $bad")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun asMap(o: Any?): Map<String, Any?> = o as? Map<String, Any?> ?: emptyMap()

    @Test
    fun e6_theWorkflowRunsClaudeHeadlessWithJsonOutputASchemaATurnLimitReadOnlyToolsAndTheContextFile() {
        val wf = parseYaml(read(".github/workflows/claude-review.yml"))
        val on = wf["on"]
        assertTrue(if (on is Map<*, *>) "pull_request" in on else on == "pull_request", "run the review on pull requests")
        val job = asMap(asMap(wf["jobs"])["review"])
        val timeout = job["timeout-minutes"]
        assertTrue(timeout is Int && timeout <= 30, "a stuck run must not hold the runner")
        assertEquals("read", asMap(job["permissions"])["contents"], "a review reads the repository")
        val step = (job["steps"] as List<*>? ?: emptyList<Any?>()).map { asMap(it) }.firstOrNull { (it["run"]?.toString() ?: "").contains("claude") }
        assertNotNull(step, "a step runs claude")
        assertTrue((asMap(step!!["env"])["ANTHROPIC_API_KEY"]?.toString() ?: "").startsWith("\${{ secrets."), "the key comes from the secrets context")
        val command = step["run"].toString()
        assertEquals(emptyList<String>(), lintCommand(command))
        val turns = Regex("""--max-turns[ =](\d+)""").find(command)
        assertTrue(turns != null && turns.groupValues[1].toInt() <= 10)
        val allowed = Regex("""--allowed[Tt]ools[ =]("[^"]*"|\S+)""").find(command)
        assertNotNull(allowed)
        val tools = Regex("""[^\s,(]+(?:\([^)]*\))?""").findAll(allowed!!.groupValues[1].trim('"')).map { it.value }.toList()
        assertTrue(tools.isNotEmpty() && tools.all { it in listOf("Read", "Grep", "Glob") || Regex("""Bash\(git (diff|log|show|status)( \*)?\)""").matches(it) }, "a review needs read-only tools: $tools")
        assertTrue(has("""--json-schema\s+"\$\(cat review-schema\.json\)"""", command), "pass the schema file to --json-schema")
        assertTrue(has("""--append-system-prompt-file\s+CLAUDE\.md""", command), "--bare skips CLAUDE.md, so pass it by hand")
    }

    private fun section(text: String, title: String): List<String> {
        val m = Regex("""(?ims)^## ${Regex.escape(title)}\s*\n(.*?)(?=^## |\z)""").find(text)
        assertNotNull(m, "CLAUDE.md has no section '## $title'")
        return m!!.groupValues[1].lines().filter { it.startsWith("- ") }.map { it.substring(2) }
    }

    @Test
    fun e7_theProjectFileStatesWhatToReportAndWhatToSkipWithASeverityExampleForEachLevel() {
        val text = read("CLAUDE.md")
        assertTrue(vague.none { it in text.lowercase() }, "a general instruction like be conservative does not improve precision: name the patterns")
        val report = section(text, "Report")
        val skip = section(text, "Skip")
        assertTrue(report.size >= 3 && report.any { "bug" in it.lowercase() } && report.any { "security" in it.lowercase() }, "list at least three categories to report, among them bugs and security")
        assertTrue(skip.size >= 2 && skip.any { "style" in it.lowercase() }, "list what to skip, minor style among it")
        val severity = section(text, "Severity")
        for (level in listOf("high", "medium", "low")) {
            val line = severity.firstOrNull { it.lowercase().startsWith("$level:") }
            assertTrue(line != null && has("`[^`]+`", line), "severity $level needs a concrete example in code")
        }
        assertTrue(section(text, "Testing standards").any { "tests/fixtures/" in it }, "name the fixtures folder in the testing standards")
    }

    @Test
    fun e8_noFileHoldsAPersonalPathAnAddressOrAKey() {
        val patterns = listOf("home path" to Regex("""(/home/\w+|/Users/\w+|C:\\Users)"""), "email address" to Regex("""[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"""), "key" to Regex("""sk-ant-[\w-]{6,}"""))
        val hits = root.toFile().walkTopDown().filter(File::isFile).sortedBy { it.path }.flatMap { file ->
            val text = file.readText(Charsets.UTF_8)
            patterns.filter { (_, rx) -> rx.containsMatchIn(text) }.map { (label, _) -> "${file.relativeTo(root.toFile())}: $label" }
        }.toList()
        assertEquals(emptyList<String>(), hits)
    }
}
