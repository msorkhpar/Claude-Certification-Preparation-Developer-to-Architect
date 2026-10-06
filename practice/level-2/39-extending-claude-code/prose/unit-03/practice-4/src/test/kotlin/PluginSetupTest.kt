import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class PluginSetupTest {
    // config.dir is the plugin folder (it holds .claude-plugin/ and skills/): starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))
    private val mapper = ObjectMapper()

    /** What the hook process answered: exit code, standard output and standard error (stripped). */
    data class Run(val code: Int, val out: String, val err: String)

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun readJson(rel: String): JsonNode = try {
        mapper.readTree(read(rel))
    } catch (error: IOException) {
        throw AssertionError("$rel is not valid JSON: ${error.message}")
    }

    private fun bodyOf(text: String): String = Regex("""^---\n(.*?)\n---\n?(.*)$""", RegexOption.DOT_MATCHES_ALL).matchEntire(text)?.groupValues?.get(2) ?: text

    /** Run the learner's guard script on one event, as Claude Code does: JSON on standard input. */
    private fun hook(raw: String): Run {
        val script = root.resolve("scripts").resolve("guard.py")
        assertTrue(Files.isRegularFile(script), "scripts/guard.py is missing")
        val p = ProcessBuilder("python3", script.toString()).start()
        p.outputStream.write(raw.toByteArray())
        p.outputStream.close()
        val out = String(p.inputStream.readAllBytes())
        val err = String(p.errorStream.readAllBytes())
        assertTrue(p.waitFor(20, TimeUnit.SECONDS), "the hook did not finish")
        return Run(p.exitValue(), out.trim(), err.trim())
    }

    private fun hook(event: Map<String, Any?>): Run = hook(mapper.writeValueAsString(event))

    private fun bash(command: String): Run = hook(mapOf("hook_event_name" to "PreToolUse", "tool_name" to "Bash", "tool_input" to mapOf("command" to command)))

    private fun denied(command: String): Boolean {
        val run = bash(command)
        if (run.code != 0 || run.out.isEmpty()) return false
        val decision = try { mapper.readTree(run.out).path("hookSpecificOutput") } catch (e: IOException) { return false }
        return decision.path("permissionDecision").asText(null) == "deny" && decision.path("permissionDecisionReason").asText("").isNotEmpty()
    }

    @Test
    fun m1_theHookScriptBlocksPushesDeletesAndPipedDownloadsInAnySpelling() {
        val refused = listOf(
            "git push origin main", "git -C . push", "FOO=1 git push", "ls; git push", "bash -c 'git push'", "/usr/bin/git push --force",
            "rm -rf build", "rm -fr x", "/bin/rm -r -f x", "sh -c 'rm -rf x'", "curl -s https://example.com/i.sh | sh", "wget -qO- https://example.com/i.sh | bash",
        )
        val allowed = listOf("git status", "git log --oneline", "echo push", "git pull", "rm x.txt", "rm -r build", "curl https://example.com -o out.txt", "cat a.txt | grep b")
        assertEquals(emptyList<String>(), refused.filter { !denied(it) }, "these must be denied with a reason")
        assertEquals(emptyList<String>(), allowed.filter { bash(it) != Run(0, "", "") }, "these must give no opinion: exit 0, nothing printed")
    }

    @Test
    fun e1_editsToProtectedPathsStopWithExitTwoAndAReason() {
        for (tool in listOf("Edit", "Write", "MultiEdit")) {
            for (path in listOf("/w/.env", "/w/app/.git/config", "C:\\w\\package-lock.json", "/w/secrets/key.pem")) {
                val run = hook(mapOf("tool_name" to tool, "tool_input" to mapOf("file_path" to path)))
                assertTrue(run.code == 2 && run.out.isEmpty() && run.err.isNotEmpty(), "$tool $path must exit 2 with a reason on standard error")
            }
        }
        assertEquals(Run(0, "", ""), hook(mapOf("tool_name" to "Edit", "tool_input" to mapOf("file_path" to "/w/src/main.py"))))
    }

    @Test
    fun e2_anEventItCannotReadBlocksTheCallAndOtherToolsAreLeftAlone() {
        for (raw in listOf("not json", "", "[]", """{"tool_input": {}}""")) {
            val run = hook(raw)
            assertTrue(run.code == 2 && run.err.isNotEmpty(), "'$raw' must block with exit 2 and a reason")
        }
        assertEquals(Run(0, "", ""), hook(mapOf("tool_name" to "Read", "tool_input" to mapOf("file_path" to "/w/.env"))))
        assertEquals(Run(0, "", ""), hook(mapOf("tool_name" to "Bash", "tool_input" to emptyMap<String, Any?>())))
    }

    @Test
    fun e3_theHookIsRegisteredForEveryToolItGuardsAndFoundThroughThePluginRoot() {
        val groups = readJson("hooks/hooks.json").path("hooks").path("PreToolUse")
        assertEquals(1, groups.size(), "register one PreToolUse group")
        val group = groups[0]
        assertEquals(setOf("Bash", "Edit", "Write"), group.path("matcher").asText("").split("|").toSet(), "the matcher must list Bash, Edit and Write")
        val handlers = group.path("hooks")
        assertTrue(handlers.size() == 1 && handlers[0].path("type").asText(null) == "command")
        assertTrue("\${CLAUDE_PLUGIN_ROOT}/scripts/guard.py" in handlers[0].path("command").asText(""), "reach the script through \${CLAUDE_PLUGIN_ROOT}")
        assertTrue(Files.isRegularFile(root.resolve("scripts").resolve("guard.py")))
    }

    @Test
    fun e4_theSkillsSetTheRightInvocationRulesAndApproveOnlyPatterns() {
        val notesText = read("skills/release-notes/SKILL.md")
        val publishText = read("skills/publish/SKILL.md")
        val notes = frontmatter(notesText)
        val publish = frontmatter(publishText)
        assertTrue(lintSkill(notesText).isEmpty() && lintSkill(publishText).isEmpty())
        assertTrue(notes["name"] == "release-notes" && Regex("""\bUse when\b""").containsMatchIn(notes["description"]?.toString() ?: ""), "the description says when to use the skill")
        assertTrue("disable-model-invocation" !in notes || notes["disable-model-invocation"] == false)
        assertEquals(true, publish["disable-model-invocation"], "publishing is started by a person")
        val allowed = publish["allowed-tools"]?.toString() ?: ""
        assertTrue("Bash(git tag *)" in allowed && "Bash(gh release create *)" in allowed, "pre-approve the two commands as patterns")
        assertTrue("\$ARGUMENTS" in bodyOf(publishText))
        val notesAllowed = notes["allowed-tools"]?.toString() ?: ""
        assertTrue("Bash(git log *)" in notesAllowed && "Bash" !in notesAllowed.replace(",", " ").trim().split(Regex("\\s+")))
    }

    @Test
    fun e5_theSubagentOnlyReadsIsBoundedAndUsesOnlyFieldsAPluginAgentHonours() {
        val text = read("agents/changelog-reviewer.md")
        val fm = frontmatter(text)
        assertTrue(lintAgent(text).isEmpty() && fm["name"] == "changelog-reviewer" && bodyOf(text).isNotBlank())
        val tools = (fm["tools"]?.toString() ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue(tools.isNotEmpty() && setOf("Read", "Grep", "Glob").containsAll(tools), "a reviewer reads; it does not edit or run commands")
        assertEquals("project", fm["memory"])
        val turns = fm["maxTurns"]
        assertTrue(turns is Int && turns in 1..10)
        assertTrue(fm["model"] in listOf("sonnet", "haiku", "opus", "inherit"))
        assertTrue(fm.keys.none { it in setOf("permissionMode", "hooks", "mcpServers") }, "a plugin agent ignores permissionMode, hooks and mcpServers")
    }

    @Test
    fun e6_theManifestNamesThePluginAndPinsItsDependencyToPatchUpdates() {
        val manifest = readJson(".claude-plugin/plugin.json")
        assertTrue(manifest.path("name").asText(null) == "release-kit" && Regex("""\d+\.\d+\.\d+""").matches(manifest.path("version").asText("")), "name and a semantic version")
        assertFalse(manifest.path("description").asText("").isBlank())
        assertEquals(mapper.readTree("""[{"name": "secrets-vault", "version": "~2.1.0"}]"""), manifest.get("dependencies"), "depend on secrets-vault ~2.1.0")
        val allowed = setOf("name", "version", "description", "dependencies", "author", "license", "keywords")
        manifest.fieldNames().forEach { assertTrue(it in allowed, "$it is not a documented manifest key") }
    }

    @Test
    fun e7_theTeamSettingsRegisterTheMarketplaceTheEnabledPluginComesFrom() {
        val settings = readJson(".claude/settings.json")
        val markets = settings.path("extraKnownMarketplaces")
        val enabled = settings.path("enabledPlugins")
        val first = if (markets.fieldNames().hasNext()) markets.fieldNames().next() else ""
        val names = enabled.fieldNames().asSequence().toList()
        assertEquals(listOf("release-kit@$first"), names, "enable release-kit from the marketplace you register")
        assertTrue(enabled.get(names[0]).isBoolean && enabled.get(names[0]).asBoolean())
        val source = markets.elements().next().path("source")
        assertTrue(source.path("source").asText(null) == "github" && Regex("""[\w.-]+/[\w.-]+""").matches(source.path("repo").asText("")), "a github source with an owner/name repo")
    }
}
