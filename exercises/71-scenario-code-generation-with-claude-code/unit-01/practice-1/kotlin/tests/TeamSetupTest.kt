import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class TeamSetupTest {
    // the folder that holds CLAUDE.md, .claude/ and docs/: the starter, the reference or a planted wrong solution
    private val root: Path = Path.of(System.getProperty("solution.dir", "starter"))

    private val ui = "src/ui/Button.tsx"
    private val uiTest = "src/ui/Button.spec.tsx"
    private val handler = "server/handlers/orders.ts"
    private val handlerTest = "server/handlers/orders.spec.ts"
    private val db = "server/db/orderRepo.ts"
    private val dbTest = "server/db/orderRepo.spec.ts"
    private val doc = "docs/readme.md"
    private val samples = listOf(ui, uiTest, handler, handlerTest, db, dbTest, doc)

    /** The words a convention is written with, the files it must reach and the files it must not reach. */
    private class Area(val marker: String, val expected: List<String>, val forbidden: List<String>)

    private val areas = listOf(
        Area("hooks", listOf(ui), listOf(handler, db, doc)),
        Area("async/await", listOf(handler), listOf(ui, db, doc)),
        Area("repository", listOf(db), listOf(ui, handler, doc)),
        Area("describe", listOf(uiTest, handlerTest, dbTest), listOf(ui, handler, db, doc)),
    )

    private class Rule(val name: String, val text: String, val paths: List<String>?)

    private fun globRegex(glob: String): Regex {
        val out = StringBuilder()
        var i = 0
        while (i < glob.length) {
            when {
                glob.startsWith("**/", i) -> { out.append("(?:.*/)?"); i += 3 }
                glob.startsWith("**", i) -> { out.append(".*"); i += 2 }
                glob[i] == '*' -> { out.append("[^/]*"); i += 1 }
                glob[i] == '?' -> { out.append("[^/]"); i += 1 }
                else -> { out.append(Regex.escape(glob[i].toString())); i += 1 }
            }
        }
        return Regex(out.toString())
    }

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun frontMatter(text: String): Pair<String, String> {
        val head = Regex("^---\\n(.*?)\\n---\\n", RegexOption.DOT_MATCHES_ALL).find(text) ?: return "" to text
        return head.groupValues[1] to text.substring(head.range.last + 1)
    }

    private fun rulePaths(head: String): List<String>? {
        val listed = Regex("^paths:\\s*\\n((?:[ \\t]+-[ \\t]+.*\\n?)+)", RegexOption.MULTILINE).find(head + "\n") ?: return null
        return listed.groupValues[1].split("\n").filter { it.isNotBlank() }.map { it.replaceFirst(Regex("^\\s*-\\s+"), "").trim().replace(Regex("^[\"']|[\"']$"), "") }
    }

    private fun rules(): List<Rule> {
        val folder = root.resolve(".claude/rules")
        if (!Files.isDirectory(folder)) return listOf()
        return Files.list(folder).use { s -> s.filter { it.toString().endsWith(".md") }.sorted().toList() }.map {
            val text = Files.readString(it)
            Rule(it.fileName.toString(), text.lowercase(), rulePaths(frontMatter(text).first))
        }
    }

    private fun reaches(paths: List<String>?, file: String) = paths == null || paths.any { globRegex(it).matches(file) }

    @Test
    fun m1_eachConventionLoadsForExactlyTheFilesOfItsArea() {
        for (area in areas) {
            val holders = rules().filter { area.marker in it.text }
            assertFalse(holders.isEmpty(), "no rule file holds the '${area.marker}' convention")
            for (file in area.expected) assertTrue(holders.any { reaches(it.paths, file) }, "the '${area.marker}' convention does not load for $file")
            for (file in area.forbidden) assertFalse(holders.any { reaches(it.paths, file) }, "the '${area.marker}' convention loads for $file, which is not its area")
        }
    }

    @Test
    fun e1_theRootFileIsShortAndHoldsOnlyWhatEveryTaskNeeds() {
        val text = read("CLAUDE.md")
        assertTrue(text.lines().count { it.isNotBlank() } <= 25, "keep the root file to 25 lines that matter")
        assertEquals(listOf<String>(), areas.map { it.marker }.filter { it in text.lowercase() }, "area conventions belong in rule files, not in the root file")
    }

    @Test
    fun e2_theReviewCommandIsSharedReadOnlyAndSaysWhatItDoes() {
        val (head, body) = frontMatter(read(".claude/commands/review.md"))
        assertTrue(Regex("^description:\\s*\\S", RegexOption.MULTILINE).containsMatchIn(head), "the command needs a description")
        val allowed = Regex("^allowed-tools:\\s*(.*)$", RegexOption.MULTILINE).find(head)
        assertNotNull(allowed, "the command lists the tools it pre-approves")
        val tools = Regex("[^\\s,(]+(?:\\([^)]*\\))?").findAll(allowed!!.groupValues[1]).map { it.value }.toList()
        assertTrue("Read" in tools && tools.none { it in listOf("Bash", "Edit", "Write", "MultiEdit") }, "a review reads: no bare Bash, no edits")
        assertTrue("git diff" in body, "the body says how to get the changes")
    }

    @Test
    fun e3_theSettingsProtectTheEnvironmentFileAndApproveNoWholeTool() {
        val perms = try {
            ObjectMapper().readTree(read(".claude/settings.json")).path("permissions")
        } catch (e: java.io.IOException) {
            throw AssertionError(".claude/settings.json is not valid JSON: ${e.message}")
        }
        assertTrue(perms.path("deny").any { it.asText() == "Read(./.env)" }, "deny reading the environment file")
        for (n in perms.path("allow")) assertFalse(n.asText() in listOf("Bash", "Bash(*)", "Edit", "Write"), "a bare allow rule approves every call of that tool")
    }

    @Test
    fun e4_theModesTableSendsOpenDesignWorkToPlanModeAndClearSmallWorkToDirect() {
        val rows = linkedMapOf<String, String>()
        for (line in read("docs/working-modes.md").split("\n")) {
            val cells = line.trim().replace(Regex("^\\||\\|$"), "").split("|")
            if (line.startsWith("|") && cells.size >= 2 && cells[1].trim().lowercase() in listOf("plan", "direct")) rows[cells[0].trim().lowercase()] = cells[1].trim().lowercase()
        }
        for ((keyword, mode) in listOf("typo" to "direct", "monolith" to "plan", "validation" to "direct", "auth library" to "plan", "rename" to "direct", "unclear" to "plan")) {
            val task = rows.keys.firstOrNull { keyword in it }
            assertNotNull(task, "the table has no row for the '$keyword' task")
            assertEquals(mode, rows[task], "'$keyword' should be $mode")
        }
    }

    @Test
    fun e5_everyRuleScopesItselfWithAGlobThatMatchesAFile() {
        val found = rules()
        assertFalse(found.isEmpty(), "write the rule files")
        for (r in found) {
            assertTrue(!r.paths.isNullOrEmpty(), "${r.name} has no paths list, so it loads in every session")
            assertTrue(samples.any { reaches(r.paths, it) }, "${r.name} matches none of the sample files")
        }
    }

    @Test
    fun e6_noFileHoldsAPersonalPathAnAddressOrAKey() {
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
