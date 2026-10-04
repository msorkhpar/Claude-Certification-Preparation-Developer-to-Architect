import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class CiSetupTest {
    // the folder that holds ci/: the starter, the reference or a planted wrong solution
    private val root: Path = Path.of(System.getProperty("solution.dir", "starter"))

    private val vague = listOf("be conservative", "be careful", "only report important", "high confidence", "use good judgement", "use your judgment")
    private val readOnly = setOf("Read", "Grep", "Glob")

    private fun words(command: String): List<String> {
        val out = mutableListOf<String>()
        val word = StringBuilder()
        var quoted = false
        var started = false
        var i = 0
        while (i < command.length) {
            val c = command[i]
            when {
                c == '\\' && i + 1 < command.length -> { i++; word.append(command[i]); started = true }
                c == '"' -> { quoted = !quoted; started = true }
                c == ' ' && !quoted -> { if (started) out += word.toString(); word.setLength(0); started = false }
                else -> { word.append(c); started = true }
            }
            i++
        }
        if (started) out += word.toString()
        return out
    }

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun loadJson(rel: String): JsonNode = try {
        ObjectMapper().readTree(read(rel))
    } catch (e: java.io.IOException) {
        throw AssertionError("$rel is not valid JSON: ${e.message}")
    }

    private fun jobs(): Map<String, JsonNode> = loadJson("ci/pipeline.json").path("jobs").associateBy { it.get("name").asText() }

    private fun job(name: String): JsonNode {
        val found = jobs()[name]
        assertNotNull(found, "ci/pipeline.json has no job named $name")
        return found!!
    }

    private fun strings(node: JsonNode): List<String> = node.map { it.asText() }

    private fun section(text: String, title: String): List<String> {
        val match = Regex("^## $title\\s*\\n(.*?)(?=^## |\\z)", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)).find(text) ?: return listOf()
        return match.groupValues[1].split("\n").filter { it.startsWith("- ") }.map { it.substring(2).trim() }
    }

    @Test
    fun m1_whoWaitsDecidesBetweenRealTimeAndBatch() {
        assertEquals("realtime", job("pre-merge-review").get("api").asText(), "a developer waits for the merge check, so it runs in real time")
        assertEquals("batch", job("debt-report").get("api").asText(), "nobody waits for the overnight report, so it runs as a batch")
        for (j in jobs().values) assertEquals(if (j.get("audience").asText() == "waiting") "realtime" else "batch", j.get("api").asText(), "${j.get("name").asText()}: the audience decides the api")
    }

    @Test
    fun e1_aReviewHasAPassForEachFileAndThenAnIntegrationPass() {
        assertEquals(listOf("per-file", "integration"), strings(job("pre-merge-review").get("passes")), "one pass per file for local issues, then one pass across files, in that order")
    }

    @Test
    fun e2_aReviewRunsInAFreshSessionAndIsGivenTheEarlierFindings() {
        val review = job("pre-merge-review")
        assertEquals("fresh", review.get("session").asText(), "the session that wrote the code is biased toward it")
        assertTrue("prior_findings" in strings(review.get("context")), "a re-run needs the earlier findings so that it reports only what is new")
    }

    @Test
    fun e3_everyClaudeCommandIsHeadlessJsonAndBounded() {
        for (j in jobs().values) {
            val tokens = words(j.get("command").asText())
            val name = j.get("name").asText()
            if (tokens[0] != "claude") continue
            assertTrue("-p" in tokens || "--print" in tokens, "$name: without -p the run waits for input")
            val fmt = tokens.indexOf("--output-format")
            assertTrue(fmt >= 0 && tokens[fmt + 1] == "json", "$name: ask for json output")
            val turns = tokens.indexOf("--max-turns")
            assertTrue(turns >= 0 && tokens[turns + 1].matches(Regex("\\d+")), "$name: bound the run with --max-turns")
        }
        val tokens = words(job("pre-merge-review").get("command").asText())
        assertTrue("--json-schema" in tokens, "the review answers in a schema")
        val item = loadJson(tokens[tokens.indexOf("--json-schema") + 1]).path("properties").path("findings").path("items")
        assertTrue(item.path("properties").path("severity").has("enum"), "severity is a closed list in the schema")
        val required = strings(item.path("required"))
        for (field in listOf("file", "line", "severity", "issue", "suggestion")) assertTrue(field in required, "a finding is required to say where, how bad, what and what to do")
    }

    @Test
    fun e4_theReviewCanOnlyRead() {
        val review = job("pre-merge-review")
        val tools = strings(review.get("tools"))
        assertTrue(tools.isNotEmpty() && readOnly.containsAll(tools), "read-only tools only, found $tools")
        val tokens = words(review.get("command").asText())
        val allowed = tokens.indexOf("--allowedTools")
        assertTrue(allowed >= 0, "list the allowed tools in the command")
        assertTrue(readOnly.containsAll(tokens[allowed + 1].split(",")), "the command approves read tools only")
    }

    @Test
    fun e5_theCriteriaNameWhatToReportWhatToSkipAndAnExampleForEachSeverity() {
        val text = read("ci/review-criteria.md")
        assertTrue(section(text, "Report").size >= 2, "list at least two kinds of issue to report")
        assertTrue(section(text, "Skip").size >= 2, "list at least two kinds of issue to skip")
        val levels = section(text, "Severity").associateBy { it.split(":")[0] }
        for (level in listOf("high", "medium", "low")) assertTrue(level in levels && "Example:" in levels.getValue(level), "$level needs a description and an Example:")
        assertEquals(listOf<String>(), vague.filter { it in text.lowercase() }, "a vague instruction does not make a review more precise: name the cases")
    }

    @Test
    fun e6_theTestPromptPassesTheExistingTestsAndSaysWhatAUsefulTestIs() {
        val text = read("ci/testgen-prompt.md")
        assertTrue("{{existing_tests}}" in text && "{{changed_files}}" in text, "the prompt carries the changed files and the existing tests")
        assertTrue(section(text, "A useful test").size >= 3, "say what a useful test is, in at least three points")
        assertTrue(section(text, "Do not write").size >= 2, "say what not to write, in at least two points")
    }

    @Test
    fun e7_noFileHoldsAPersonalPathAnAddressOrAKey() {
        val hits = mutableListOf<String>()
        val patterns = listOf("home path" to Regex("(/home/\\w+|/Users/\\w+|C:\\\\Users)"), "email address" to Regex("[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+"), "key" to Regex("sk-ant-[\\w-]{6,}"))
        Files.walk(root).use { s ->
            for (p in s.filter { Files.isRegularFile(it) }.sorted().toList()) {
                val text = Files.readString(p)
                for ((label, pattern) in patterns) if (pattern.containsMatchIn(text)) hits += "${root.relativize(p)}: $label"
            }
        }
        assertEquals(listOf<String>(), hits)
    }
}
