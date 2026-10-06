import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ExplorerSetupTest {
    // config.dir is the project folder that holds .claude/, agent-options.json and docs/: starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun readJson(rel: String): Map<String, Any?> = try {
        json(read(rel))
    } catch (error: Exception) {
        throw AssertionError("$rel is not valid JSON: ${error.message}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun asMap(o: Any?): Map<String, Any?> = o as? Map<String, Any?> ?: emptyMap()

    private fun strings(o: Any?): List<String> = (o as? List<*> ?: emptyList<Any?>()).map { it.toString() }

    private fun settings() = readJson(".claude/settings.json")

    private fun check(table: List<Triple<String, String, String>>) {
        val s = settings()
        val wrong = table.mapNotNull { (tool, arg, want) -> decide(s, ruleTool(tool), arg).takeIf { it != want }?.let { "$tool $arg: got $it, want $want" } }
        assertEquals(emptyList<String>(), wrong)
    }

    /** The text before the fallback heading, and the numbered steps before and after it. */
    data class Sections(val head: String, val steps: List<String>, val fallback: List<String>)

    private fun steps(block: String) = Regex("""(?m)^\d+\. (.*)$""").findAll(block).map { it.groupValues[1] }.toList()

    private fun sections(text: String): Sections {
        val sep = "## When an edit does not apply"
        val head = text.substringBefore(sep)
        val tail = if (sep in text) text.substringAfter(sep) else ""
        return Sections(head, steps(head), steps(tail))
    }

    private fun has(regex: String, text: String) = Regex(regex).containsMatchIn(text)

    @Test
    fun m1_thePermissionRulesLetAnExplorerReadSearchAndTakeNotesButNotChangeTheSource() {
        check(
            listOf(
                Triple("Read", "src/inventory/stock.py", "allow"), Triple("Grep", "src/inventory", "allow"), Triple("Glob", "src/**/handlers/*.py", "allow"),
                Triple("Edit", "src/inventory/stock.py", "deny"), Triple("Write", "src/inventory/new.py", "deny"), Triple("Edit", "notes/findings.md", "allow"),
                Triple("Write", "notes/findings.md", "allow"), Triple("Edit", "docs/readme.md", "ask"), Triple("Bash", "git log --oneline", "allow"),
                Triple("Bash", "git diff HEAD~1", "allow"), Triple("Bash", "git status", "allow"), Triple("Bash", "rm -rf build", "ask"), Triple("Bash", "git push origin main", "ask"),
            ),
        )
    }

    @Test
    fun e1_aReadRuleProtectsSecretsFromReadingSearchingAndWriting() {
        check(
            listOf(
                Triple("Read", "./.env", "deny"), Triple("Read", "secrets/prod.key", "deny"), Triple("Grep", "secrets/prod.key", "deny"), Triple("Glob", "secrets/prod.key", "deny"),
                Triple("Edit", ".env", "deny"), Triple("Write", "secrets/new.key", "deny"), Triple("Edit", "secrets/prod.key", "deny"), Triple("Read", "vendor/secrets/a.txt", "deny"),
            ),
        )
    }

    @Test
    fun e2_onlyRuleFormsThatAreConsultedAreUsedAndNoWholeToolThatChangesThingsIsAllowed() {
        val perms = asMap(settings()["permissions"])
        val rules = listOf("allow", "ask", "deny").flatMap { strings(perms[it]) }
        assertTrue(rules.isNotEmpty(), "write the permission rules")
        assertEquals(emptyList<String>(), rules.filter { Regex("""(Write|NotebookEdit|MultiEdit)\(.*""").matches(it) }, "path rules for Write are never matched: write them as Edit(...)")
        assertEquals(emptyList<String>(), rules.filter { Regex("""(Grep|Glob)\(.*""").matches(it) }, "path rules for the search tools are written as Read(...)")
        assertEquals(emptyList<String>(), strings(perms["allow"]).filter { it in listOf("Bash", "Bash(*)", "Edit", "Write") }, "a bare allow rule approves every call of that tool")
        assertTrue(setOf("allow", "ask", "deny").containsAll(perms.keys))
    }

    @Test
    fun e3_theExplorerAgentReadsAndSearchesAndSaysWhenToUseIt() {
        val text = read(".claude/agents/explorer.md")
        val fm = frontmatter(text)
        val body = Regex("""^---\n(.*?)\n---\n?(.*)$""", RegexOption.DOT_MATCHES_ALL).matchEntire(text)?.groupValues?.get(2) ?: text
        assertEquals("explorer", fm["name"])
        val tools = (fm["tools"]?.toString() ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        assertEquals(listOf("Glob", "Grep", "Read"), tools.sorted(), "an explorer lists Read, Grep and Glob and nothing that edits or runs commands")
        assertTrue(has("""(?:^|\. )Use when\b""", fm["description"]?.toString() ?: ""), "the description starts a sentence with Use when")
        val turns = fm["maxTurns"]
        assertTrue(turns is Int && turns in 1..15 && !fm["model"]?.toString().isNullOrEmpty(), "bound the turns and choose a model")
        assertTrue(body.isNotBlank(), "the body holds the agent's instructions")
    }

    @Test
    fun e4_theSdkOptionsMakeTheSearchToolsAvailableAndRemoveTheToolsThatChangeThings() {
        val o = readJson("agent-options.json")
        val tools = o["tools"]?.let { strings(it) }
        val allowed = o["allowedTools"]?.let { strings(it) }
        val disallowed = o["disallowedTools"]?.let { strings(it) }
        assertEquals(listOf("Glob", "Grep", "Read"), (tools ?: emptyList()).sorted(), "tools lists the three the agent may have; naming Grep and Glob puts them back on macOS, Linux and WSL")
        assertEquals(listOf("Read", "Grep", "Glob"), toolSet("linux", tools, allowed ?: emptyList(), disallowed ?: emptyList()))
        assertTrue(allowed != null && tools!!.containsAll(allowed), "allowedTools pre-approves only listed tools")
        assertTrue("Bash" !in allowed!! && "Edit" !in allowed && "Write" !in allowed)
        assertTrue((disallowed ?: emptyList()).containsAll(listOf("Bash", "Edit", "Write")), "a bare name in disallowedTools removes the tool from the model's context")
    }

    @Test
    fun e5_theExplorationPlanStartsWithASearchThenReadsAndNeverReadsEverythingFirst() {
        val s = sections(read("docs/exploration-plan.md"))
        assertTrue(has("(?i)do not read every file", s.head), "say that the whole repository is not read first")
        assertTrue(s.steps.size >= 4, "write the steps as a numbered list")
        val grep = s.steps.indexOfFirst { has("""\bGrep\b""", it) }
        val read = s.steps.indexOfFirst { has("""\bRead\b""", it) }
        assertTrue(grep >= 0 && read >= 0 && grep < read, "search for entry points before reading anything")
        assertTrue(s.steps.any { has("""\bGlob\b""", it) && has("""`[^`]*(\*\*/|\*\.)[^`]*`""", it) }, "use Glob with a name pattern")
        assertTrue(s.steps.withIndex().any { (i, t) -> i > read && has("export", t) && has("""\beach\b""", t) && has("""\bGrep\b""", t) }, "to trace usage through wrappers, list the exported names and search for each")
        assertTrue(s.steps.any { "notes/" in it }, "findings go to notes/, the only place the agent may write")
    }

    @Test
    fun e6_theEditFallbackWidensTheAnchorThenReplacesAllThenRewritesTheFile() {
        val fallback = sections(read("docs/exploration-plan.md")).fallback
        assertTrue(fallback.size >= 3, "write the three remedies as a numbered list under 'When an edit does not apply'")
        assertTrue(has("surrounding|more context|longer", fallback[0]) && "unique" in fallback[0])
        assertTrue("replace_all" in fallback[1])
        assertTrue(has("""\bRead\b""", fallback[2]) && has("""\bWrite\b""", fallback[2]) && has("(?i)whole file", fallback[2]))
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
