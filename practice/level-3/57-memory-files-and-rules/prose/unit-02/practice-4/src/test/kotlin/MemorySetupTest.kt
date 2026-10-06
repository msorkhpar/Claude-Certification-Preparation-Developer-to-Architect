import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class MemorySetupTest {
    // config.dir is the project folder that holds CLAUDE.md, .claude/rules/ and docs/: starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))

    private val t = listOf(
        "Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test.",
        "Every test builds its data from a fixture in `tests/fixtures/`; no ad-hoc data inline.",
        "Mock the network with `msw`; a test never calls a live service.",
        "Each test checks one behaviour and its name states that behaviour.",
    )
    private val a = listOf(
        "Every handler under `src/api/` validates its input with the shared `parse()` helper before it uses it.",
        "Return errors as `{ code, message }` with the HTTP status from `src/api/status.ts`.",
        "Handlers are `async` and never swallow a rejected promise.",
        "Document each endpoint with an OpenAPI comment above its handler.",
    )
    private val r = listOf(
        "Every resource carries the `owner` and `cost-centre` tags.",
        "Run `terraform fmt` and `terraform validate` before proposing a change.",
        "Pin provider versions with `~>` constraints.",
    )
    private val u = listOf(
        "Commit messages use the imperative mood and stay under 72 characters in the first line.",
        "Run `npm test` before saying a task is done.",
        "Do not add dependencies without asking.",
    )
    private val groups = linkedMapOf("U" to u, "T" to t, "A" to a, "R" to r)
    private val p1 = "I prefer short answers with no preamble."
    private val p2 = "My sandbox API is at http://localhost:4010 with the dev token from my shell profile."
    private val files = listOf(
        "src/api/users.ts", "src/api/orders.ts", "src/api/status.ts", "src/ui/Button.tsx", "src/ui/Button.test.tsx", "src/api/users.test.ts",
        "tools/cli/run.test.ts", "terraform/prod/main.tf", "terraform/modules/net/vpc.tf", "db/migrations/0001_init.sql", "README.md", "package.json",
    )
    private val tests = listOf("src/ui/Button.test.tsx", "src/api/users.test.ts", "tools/cli/run.test.ts")

    /** A rule file's `paths` (null when it has none) and its body. */
    data class Rule(val paths: List<String>?, val body: String)

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun rel(file: File) = file.relativeTo(root.toFile()).path.replace('\\', '/')

    private fun ruleFiles(): List<String> {
        val folder = root.resolve(".claude").resolve("rules").toFile()
        return if (folder.isDirectory) folder.walkTopDown().filter { it.isFile && it.name.endsWith(".md") }.map { rel(it) }.sorted().toList() else emptyList()
    }

    private fun bodyOf(text: String) = Regex("""^---\n(.*?)\n---\n?(.*)$""", RegexOption.DOT_MATCHES_ALL).matchEntire(text)?.groupValues?.get(2) ?: text

    /** The `paths` of a rule as Claude Code reads it: a YAML list or a comma-separated string; null when there are none. */
    private fun pathsOf(text: String): List<String>? = when (val value = frontmatter(text)["paths"]) {
        null -> null
        is String -> value.split(",").map { it.trim() }
        else -> (value as List<*>).map { it.toString() }
    }

    private fun ruleBodies(): Map<String, Rule> = ruleFiles().associateWith { Rule(pathsOf(read(it)), bodyOf(read(it))) }

    /** Every Markdown file outside .claude/, by project-relative path: what an @import can reach. */
    private fun projectTexts(): Map<String, String> =
        root.toFile().walkTopDown().filter { it.isFile && it.name.endsWith(".md") && ".claude" !in rel(it).split("/") }.sortedBy { rel(it) }.associate { rel(it) to it.readText() }

    /** CLAUDE.md and everything it imports: all of it is in context from the first message. */
    private fun launchText(): String {
        val texts = projectTexts()
        return (listOf("CLAUDE.md") + importsOf("CLAUDE.md", texts)).joinToString("\n") { texts.getValue(it) }
    }

    /** The text in context at launch plus the rules that the touched files bring in. */
    private fun contextFor(touched: List<String>): String {
        val bodies = ruleBodies()
        val loaded = rulesLoaded(bodies.mapValues { it.value.paths }, touched)
        return (listOf(launchText()) + loaded.map { bodies.getValue(it).body }).joinToString("\n")
    }

    private fun groupsIn(text: String): Set<String> {
        val found = sortedSetOf<String>()
        for ((name, lines) in groups) {
            val have = lines.map { it in text }
            assertTrue(have.all { it } || !have.any { it }, "group $name is only partly there: move whole conventions, not some of them")
            if (have.all { it }) found += name
        }
        return found
    }

    @Test
    fun m1_theConventionsOfEachAreaLoadForExactlyTheFilesThatAreaGoverns() {
        val table = linkedMapOf(
            "src/api/users.ts" to setOf("U", "A"), "src/ui/Button.tsx" to setOf("U"), "src/ui/Button.test.tsx" to setOf("U", "T"), "src/api/users.test.ts" to setOf("U", "A", "T"),
            "terraform/prod/main.tf" to setOf("U", "R"), "README.md" to setOf("U"), "tools/cli/run.test.ts" to setOf("U", "T"),
        )
        val got = table.keys.associateWith { groupsIn(contextFor(listOf(it))) }
        val wrong = table.filter { (k, v) -> v != got[k] }.map { (k, v) -> "$k: got ${got[k]}, want $v" }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun e1_theRootFileIsShortAndHoldsOnlyWhatEveryTaskNeeds() {
        val rootText = read("CLAUDE.md")
        assertTrue(rootText.lines().size - (if (rootText.endsWith("\n")) 1 else 0) <= 50, "the root file is long: it is read in every session, so only what every task needs belongs in it")
        assertTrue(u.all { it in rootText }, "the three rules that every task needs stay in the root file")
        val launch = launchText()
        for (name in listOf("T", "A", "R")) assertFalse(groups.getValue(name).any { it in launch }, "the $name conventions are in the launch context: an import loads at launch too")
    }

    @Test
    fun e2_theTestingRuleFollowsTheFileTypeAndNotTheFolder() {
        val testing = ruleBodies().values.filter { t[0] in it.body }.map { it.paths }
        assertTrue(testing.size == 1 && !testing[0].isNullOrEmpty(), "put the testing conventions in one rule file with paths")
        for (file in tests) assertTrue(testing[0]!!.any { globMatch(it, file) }, "$file is a test file and the rule must cover it")
        for (file in files.filter { it !in tests }) assertFalse(testing[0]!!.any { globMatch(it, file) }, "$file is not a test file")
    }

    @Test
    fun e3_theImportNamesAFileThatExistsAndLoadsAtLaunch() {
        read("CLAUDE.md")
        val texts = projectTexts()
        assertEquals(emptyList<String>(), unresolvedImports("CLAUDE.md", texts), "an import that names no file imports nothing: check the spelling")
        assertEquals(listOf("docs/standards/architecture.md"), importsOf("CLAUDE.md", texts), "import the architecture notes with @docs/standards/architecture.md outside a code span")
    }

    @Test
    fun e4_personalLinesSitInPersonalFilesAndTheLocalFileIsIgnored() {
        val user = read("user-memory.example.md")
        val local = read("CLAUDE.local.example.md")
        assertTrue(p1 in user && p2 !in user && p2 in local && p1 !in local, "a preference of yours goes in the user file, a sandbox note in the local file")
        val shared = (listOf("CLAUDE.md", "docs/standards/architecture.md") + ruleFiles()).map { read(it) }
        assertFalse(shared.any { p1 in it || p2 in it }, "a teammate would load your personal lines from the shared files")
        assertTrue("CLAUDE.local.md" in read(".gitignore").lines().map { it.trim() }, "CLAUDE.local.md must be ignored by git")
    }

    @Test
    fun e5_aRuleThatMustAlwaysHoldIsAPermissionRuleAndNotASentence() {
        assertTrue(Files.isRegularFile(root.resolve(".claude").resolve("settings.json")), ".claude/settings.json is missing")
        val settings = json(read(".claude/settings.json"))
        assertEquals("deny", decide(settings, ruleTool("Edit"), "db/migrations/0001_init.sql"), "memory is context, not enforcement: deny the edit in settings")
        assertNotEquals("deny", decide(settings, ruleTool("Edit"), "src/api/users.ts"), "only the migrations are denied")
    }

    @Test
    fun e6_everyRuleScopesItselfWithPathsThatAreValidAndMatchSomething() {
        val bodies = ruleBodies()
        assertTrue(bodies.size >= 3, "write the three rule files: testing, API and Terraform")
        for ((rel, rule) in bodies) {
            val paths = rule.paths
            assertTrue(!paths.isNullOrEmpty(), "$rel has no usable paths: without them (or with frontmatter that does not parse) the rule loads in every session")
            for (pattern in paths!!) {
                assertTrue(pattern.count { it == '{' } == pattern.count { it == '}' } && pattern.count { it == '[' } == pattern.count { it == ']' }, "$rel: the pattern '$pattern' is not balanced")
                assertTrue(files.any { globMatch(pattern, it) }, "$rel: the pattern '$pattern' matches no file of the project: a bare folder name is not a glob")
            }
        }
    }

    @Test
    fun e7_noFileHoldsAPersonalPathAnAddressOrAKey() {
        val patterns = listOf("home path" to Regex("""(/home/\w+|/Users/\w+|C:\\Users)"""), "email address" to Regex("""[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"""), "key" to Regex("""sk-ant-[\w-]{6,}"""))
        val hits = root.toFile().walkTopDown().filter(File::isFile).sortedBy { it.path }.flatMap { file ->
            val text = file.readText(Charsets.UTF_8)
            patterns.filter { (_, rx) -> rx.containsMatchIn(text) }.map { (label, _) -> "${file.relativeTo(root.toFile())}: $label" }
        }.toList()
        assertEquals(emptyList<String>(), hits)
    }
}
