import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class PipelineSetupTest {
    // config.dir is the repository folder: starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    /** A workflow's text and its parsed map; it needs a jobs map. */
    inner class Workflow(val text: String, val map: Map<String, Any?>) {
        val jobs: Map<String, Any?> get() = asMap(map["jobs"])
    }

    @Suppress("UNCHECKED_CAST")
    private fun asMap(o: Any?): Map<String, Any?> = o as? Map<String, Any?> ?: emptyMap()

    private fun asList(o: Any?): List<Any?> = o as? List<*> ?: emptyList<Any?>()

    private fun workflow(rel: String): Workflow {
        val text = read(rel)
        val wf = try { parseYaml(text) } catch (error: Exception) { throw AssertionError("$rel is not readable YAML: ${error.message}") }
        assertTrue(wf["jobs"] is Map<*, *>, "$rel needs a jobs map")
        return Workflow(text, wf)
    }

    private fun claudeStep(job: Map<String, Any?>): Map<String, Any?> {
        val steps = asList(job["steps"]).map { asMap(it) }.filter { (it["uses"]?.toString() ?: "").startsWith("anthropics/claude-code-action") }
        assertEquals(1, steps.size, "one claude-code-action step")
        return steps[0]
    }

    private fun str(o: Any?) = o?.toString() ?: "None"

    @Test
    fun m1_theMentionWorkflowAnswersOnlyClaudeCommentsAndHoldsNoKey() {
        val w = workflow(".github/workflows/claude.yml")
        val on = asMap(w.map["on"])
        assertEquals(setOf("issue_comment", "pull_request_review_comment"), on.keys, "trigger on issue comments and review comments")
        for (v in on.values) assertEquals(listOf("created"), asMap(v)["types"], "only new comments")
        val job = asMap(w.jobs["claude"])
        assertTrue(job.isNotEmpty(), "the job is called claude")
        assertTrue("@claude" in str(job["if"]) && "github.event.comment.body" in str(job["if"]), "start the runner only for comments that mention @claude")
        assertTrue(str(asMap(asList(job["steps"]).first())["uses"]).startsWith("actions/checkout@v"), "check the repository out first")
        val step = claudeStep(job)
        assertEquals("anthropics/claude-code-action@v1", step["uses"])
        assertEquals("\${{ secrets.ANTHROPIC_API_KEY }}", asMap(step["with"])["anthropic_api_key"])
        val perms = asMap(job["permissions"])
        assertEquals(listOf("write", "write", "write", "write"), listOf("contents", "pull-requests", "issues", "id-token").map { perms[it] })
        assertEquals(emptyList<Finding>(), lint(w.text))
    }

    @Test
    fun e1_theReviewWorkflowReadsTheCodeAndPostsTheReview() {
        val w = workflow(".github/workflows/review.yml")
        val on = asMap(w.map["on"])
        assertEquals(listOf("pull_request"), on.keys.toList())
        assertEquals(listOf("opened", "synchronize", "ready_for_review", "reopened"), asMap(on["pull_request"])["types"])
        val job = asMap(w.jobs["review"])
        assertTrue(job.isNotEmpty(), "the job is called review")
        val perms = asMap(job["permissions"])
        assertTrue(perms["contents"] == "read" && perms["id-token"] == "write", "a review reads the code")
        val uses = asList(job["steps"]).map { asMap(it)["uses"]?.toString() ?: "" }
        assertTrue(uses[0].startsWith("actions/checkout@v") && uses.getOrNull(1) == "anthropics/claude-code-action@v1")
        val with = asMap(claudeStep(job)["with"])
        assertTrue("/code-review" in str(with["prompt"]) && "--comment" in str(with["prompt"]), "run the review skill and post its findings")
        assertTrue("mcp__github_inline_comment__create_inline_comment" in str(with["claude_args"]), "name the inline comment tool in claude_args")
        assertTrue(Regex("""code-review@[\w-]+""").matches(str(with["plugins"])) && with["plugin_marketplaces"] != null && str(with["plugin_marketplaces"]).isNotEmpty(), "install the review plugin from a marketplace")
        assertEquals(emptyList<Finding>(), lint(w.text))
    }

    @Test
    fun e2_eachPromptFileCarriesAVersionThatItsChangelogExplains() {
        val dir = root.resolve("prompts")
        val prompts = if (Files.isDirectory(dir)) dir.toFile().listFiles { f -> f.name.endsWith(".md") && f.name != "CHANGELOG.md" }!!.sortedBy { it.name } else emptyList<File>()
        assertTrue(prompts.isNotEmpty(), "prompts/ holds at least one prompt")
        val headings = Regex("""(?m)^## (\S+)\s*$""").findAll(read("prompts/CHANGELOG.md")).map { it.groupValues[1] }.toList()
        assertTrue(headings.isNotEmpty(), "the changelog has one ## heading per version, newest first")
        for (prompt in prompts) {
            val text = prompt.readText()
            val m = Regex("""^---\n(.*?)\n---\n?(.*)$""", RegexOption.DOT_MATCHES_ALL).matchEntire(text)
            val fm = if (m != null) parseYaml(m.groupValues[1]) else emptyMap()
            val body = m?.groupValues?.get(2) ?: text
            val version = fm["version"]?.toString() ?: ""
            assertTrue(Regex("""\d+\.\d+\.\d+""").matches(version), "${prompt.name} needs a semantic version in its frontmatter")
            assertEquals(version, headings[0], "the newest changelog entry (${headings[0]}) must match ${prompt.name} ($version)")
            assertTrue(body.isNotBlank())
        }
    }

    @Test
    fun e3_theReviewGuidanceAndTheGitWorkflowAreFilesTheReviewerAndClaudeRead() {
        val review = read("REVIEW.md")
        for (heading in listOf("Always check", "Skip")) {
            val m = Regex("""(?ms)^## $heading\s*\n(.*?)(?=^## |\z)""").find(review)
            assertNotNull(m, "REVIEW.md needs a '## $heading' section")
            val section = m!!.groupValues[1]
            assertTrue(Regex("""(?m)^- .+$""").findAll(section).count() >= 2, "'$heading' needs at least two bullets")
            assertTrue(Regex("""`[\w./*-]+/`|`[\w./*-]+\.\w+`""").containsMatchIn(section), "'$heading' names a path in backticks")
        }
        val memory = read("CLAUDE.md")
        assertTrue("`feature/<ticket>`" in memory && Regex("(?i)commit message").containsMatchIn(memory) && Regex("(?i)pull request").containsMatchIn(memory), "CLAUDE.md states the branch name, the commit message and the pull request rule")
        assertTrue(Regex("(?i)never commit to `main`").containsMatchIn(memory))
    }

    @Test
    fun e4_everyRunIsBoundedByTurnsTimeAndConcurrency() {
        for ((rel, jobName) in listOf(".github/workflows/claude.yml" to "claude", ".github/workflows/review.yml" to "review")) {
            val w = workflow(rel)
            val job = asMap(w.jobs[jobName])
            val timeout = job["timeout-minutes"]
            assertTrue(timeout is Int && timeout in 1..30, "$rel: a timeout of 30 minutes or less")
            val turns = Regex("""--max-turns (\d+)""").find(str(asMap(claudeStep(job)["with"])["claude_args"]))
            assertTrue(turns != null && turns.groupValues[1].toInt() in 1..10, "$rel: --max-turns of 10 or fewer")
            val group = asMap(w.map["concurrency"])["group"]
            assertTrue(group != null && group.toString().isNotEmpty(), "$rel: a concurrency group")
        }
        assertEquals(true, asMap(workflow(".github/workflows/review.yml").map["concurrency"])["cancel-in-progress"], "a new push replaces a running review")
    }

    @Test
    fun e5_noFileHoldsAKeyAPersonalPathOrAnAddress() {
        val patterns = listOf("home path" to Regex("""(/home/\w+|/Users/\w+|C:\\Users)"""), "email address" to Regex("""[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"""), "key" to Regex("""sk-ant-[\w-]{6,}"""))
        val hits = root.toFile().walkTopDown().filter(File::isFile).sortedBy { it.path }.flatMap { file ->
            val text = file.readText(Charsets.UTF_8)
            patterns.filter { (_, rx) -> rx.containsMatchIn(text) }.map { (label, _) -> "${file.relativeTo(root.toFile())}: $label" }
        }.toList()
        assertEquals(emptyList<String>(), hits)
    }
}
