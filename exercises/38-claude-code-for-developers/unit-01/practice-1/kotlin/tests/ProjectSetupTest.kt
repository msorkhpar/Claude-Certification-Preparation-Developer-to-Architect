import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class ProjectSetupTest {
    // config.dir is the project folder that holds CLAUDE.md and .claude/: starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))
    private val mapper = ObjectMapper()

    private val managed = json("""{"permissions": {"deny": ["Bash(sudo *)"]}}""") // a fictional organisation policy
    private val user = json("""{"model": "haiku", "permissions": {"allow": ["Bash(ls *)"]}}""")

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readJson(rel: String): Map<String, Any?> {
        try {
            val data = mapper.readValue(read(rel), Any::class.java)
            assertTrue(data is Map<*, *>, "$rel must hold a JSON object")
            return data as Map<String, Any?>
        } catch (error: IOException) {
            throw AssertionError("$rel is not valid JSON: ${error.message}")
        }
    }

    private fun layers(withLocal: Boolean): Map<String, Map<String, Any?>> {
        val out = linkedMapOf("managed" to managed, "user" to user, "project" to readJson(".claude/settings.json"))
        if (withLocal && Files.isRegularFile(root.resolve(".claude/settings.local.json"))) out["local"] = readJson(".claude/settings.local.json")
        return out
    }

    private fun permissions(settings: Map<String, Any?>): Map<*, *> = settings["permissions"] as Map<*, *>? ?: emptyMap<String, Any?>()

    private fun strings(value: Any?): List<String> = (value as List<*>? ?: emptyList<Any?>()).map { it.toString() }

    private fun flat(script: String) = script.replace("\\\n", " ")

    @Test
    fun m1_theMemoryFileIsShortConcreteAndPullsInTheArchitectureNotes() {
        val text = read("CLAUDE.md")
        assertTrue(text.lines().size - (if (text.endsWith("\n")) 1 else 0) <= 200, "keep CLAUDE.md under 200 lines")
        assertTrue("`make test`" in text && "`make lint`" in text, "name the test and lint commands in backticks")
        assertTrue(Regex("""(?m)^[^`\n]*@docs/architecture\.md""").containsMatchIn(text), "import docs/architecture.md with an @ line")
        assertTrue(text.lines().count { Regex("""\b(IMPORTANT|MUST|NEVER|ALWAYS)\b""").containsMatchIn(it) } <= 2, "emphasis on many lines makes none of them stand out")
        val files = mapOf("/p/CLAUDE.md" to text, "/p/docs/architecture.md" to read("docs/architecture.md"))
        assertEquals(listOf("/p/CLAUDE.md", "/p/docs/architecture.md"), loadMemory(files, "/p"), "the import must resolve to the architecture file")
    }

    @Test
    fun e1_thePermissionRulesAllowTheDailyCommandsAskBeforeCommitsAndDenySecretsAndPushes() {
        val s = effectiveSettings(layers(true))
        val mode = permissions(s)["defaultMode"]?.toString() ?: "default"
        val table = listOf(
            Triple("Bash", "make test", "allow"), Triple("Bash", "make lint", "allow"), Triple("Bash", "make deploy", "ask"), Triple("Bash", "git status", "allow"),
            Triple("Bash", "git diff HEAD~1", "allow"), Triple("Bash", "git commit -m 'AB-1 fix'", "ask"), Triple("Bash", "git push origin main", "deny"),
            Triple("Bash", "make test && git push origin main", "deny"), Triple("Bash", "curl https://example.com", "deny"), Triple("Bash", "sudo make test", "deny"),
            Triple("Bash", "ls -la", "allow"), Triple("Read", "./.env", "deny"), Triple("Read", "secrets/key.pem", "deny"), Triple("Edit", ".env", "deny"),
            Triple("Read", "src/api/app.py", "allow"), Triple("Edit", "src/api/app.py", "allow"),
        )
        val wrong = table.mapNotNull { (tool, arg, want) -> decide(s, tool, arg, mode).takeIf { it != want }?.let { "$tool $arg: got $it, want $want" } }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun e2_theSharedFileSetsAModeItMaySetAndUsesOnlyRulesThatAreConsulted() {
        val shared = readJson(".claude/settings.json")
        val perms = permissions(shared)
        assertEquals("acceptEdits", perms["defaultMode"], "set defaultMode to acceptEdits; auto and bypassPermissions are ignored in a repository file")
        assertEquals("acceptEdits", permissions(effectiveSettings(mapOf("project" to shared)))["defaultMode"])
        val rules = listOf("allow", "ask", "deny").flatMap { strings(perms[it]) }
        assertEquals(emptyList<String>(), rules.filter { Regex("""(Write|NotebookEdit|MultiEdit)\(.*""").matches(it) }, "path rules for Write are never consulted: use Edit or Read")
        assertEquals(emptyList<String>(), strings(perms["allow"]).filter { it == "Bash" || it == "Bash(*)" }, "a bare Bash allow rule approves every command")
        assertTrue(setOf("defaultMode", "allow", "ask", "deny").containsAll(perms.keys))
    }

    @Test
    fun e3_personalSettingsStayLocalAndTheLocalFileWins() {
        val ignore = read(".gitignore").lines().map { it.trim() }
        assertTrue(".claude/settings.local.json" in ignore && "CLAUDE.local.md" in ignore, "git must ignore both personal files")
        assertEquals("opus", readJson(".claude/settings.json")["model"], "the team default model is opus")
        assertEquals("sonnet", effectiveSettings(layers(true))["model"], "the local file overrides the team model")
        assertEquals("opus", effectiveSettings(layers(false))["model"])
        val local = readJson(".claude/settings.local.json")
        assertTrue("permissions" !in local && "env" !in local, "keep the local file to the model override")
    }

    @Test
    fun e4_theCustomCommandIsASkillThatOnlyAPersonCanStart() {
        val text = read(".claude/skills/fix-issue/SKILL.md")
        val fm = frontmatter(text)
        val body = text.substring(text.indexOf("\n---", 3) + 4)
        assertTrue(fm["name"] == "fix-issue" && !fm["description"]?.toString().isNullOrBlank(), "name and description are required")
        assertEquals(true, fm["disable-model-invocation"], "a command that edits code and calls gh is started by a person")
        assertTrue(!fm["argument-hint"]?.toString().isNullOrEmpty(), "show the argument in the menu")
        assertTrue("\$ARGUMENTS" in body && "`make test`" in body)
    }

    @Test
    fun e5_theHeadlessScriptIsBoundedAndDoesNotSkipPermissions() {
        val flat = flat(read("scripts/ci-review.sh"))
        assertTrue(Regex("""\bclaude -p\b""").containsMatchIn(flat) && "--output-format json" in flat && "--bare" in flat)
        val turns = Regex("""--max-turns (\d+)""").find(flat)
        assertTrue(turns != null && turns.groupValues[1].toInt() in 1..10, "cap the turns at 10 or fewer")
        assertTrue(Regex("""--max-budget-usd \d""").containsMatchIn(flat), "cap the spend")
        assertTrue(Regex("""--permission-mode (dontAsk|plan)\b""").containsMatchIn(flat), "start from a mode that never prompts and never auto-approves")
        val allowed = Regex("""--allowedTools "([^"]*)"""").find(flat)
        assertNotNull(allowed, "list the pre-approved tools")
        val tools = allowed!!.groupValues[1].split(Regex(""",(?![^()]*\))""")).map { it.trim() }
        assertTrue(tools.isNotEmpty() && "Bash" !in tools && "Edit" !in tools && "Write" !in tools, "pre-approve patterns, not whole tools that change things")
        assertTrue("dangerously-skip-permissions" !in flat && "bypassPermissions" !in flat)
    }

    @Test
    fun e6_noFileHoldsAPersonalPathAnAddressOrAKey() {
        val patterns = listOf("home path" to Regex("""(/home/\w+|/Users/\w+|C:\\Users)"""), "email address" to Regex("""[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"""), "key" to Regex("""sk-ant-[\w-]{6,}"""))
        val hits = root.toFile().walkTopDown().filter(File::isFile).sortedBy { it.path }.flatMap { file ->
            val text = file.readText(Charsets.UTF_8)
            patterns.filter { (_, rx) -> rx.containsMatchIn(text) }.map { (label, _) -> "${file.relativeTo(root.toFile())}: $label" }
        }.toList()
        assertEquals(emptyList<String>(), hits)
    }
}
