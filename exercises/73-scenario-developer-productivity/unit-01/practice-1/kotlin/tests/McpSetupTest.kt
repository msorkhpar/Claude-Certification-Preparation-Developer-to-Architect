import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class McpSetupTest {
    // the folder that holds .mcp.json, .claude/ and docs/: the starter, the reference or a planted wrong solution
    private val root: Path = Path.of(System.getProperty("solution.dir", "starter"))

    private val agentFiles = listOf(".claude/agents/explorer.md", ".claude/agents/scaffolder.md")
    private val secretKey = Regex("token|key|secret|authorization", RegexOption.IGNORE_CASE)

    private class Agent(val description: String, val tools: List<String>?)

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

    private fun frontMatter(text: String): Pair<String, String> {
        val head = Regex("^---\\n(.*?)\\n---\\n", RegexOption.DOT_MATCHES_ALL).find(text) ?: return "" to text
        return head.groupValues[1] to text.substring(head.range.last + 1)
    }

    private fun agent(rel: String): Agent {
        val head = frontMatter(read(rel)).first
        val description = Regex("^description:\\s*(.*)$", RegexOption.MULTILINE).find(head)
        val tools = Regex("^tools:\\s*(.*)$", RegexOption.MULTILINE).find(head)
        return Agent(description?.groupValues?.get(1)?.trim() ?: "", tools?.groupValues?.get(1)?.split(",")?.map { it.trim() })
    }

    private fun servers(): JsonNode = loadJson(".mcp.json").path("mcpServers")

    private fun rules(kind: String): List<String> = loadJson(".claude/settings.json").path("permissions").path(kind).map { it.asText() }

    @Test
    fun m1_everyMcpToolReferenceBelongsToAConfiguredServer() {
        val configured = servers()
        assertTrue(configured.size() > 0, "configure at least one server in .mcp.json")
        val refs = agentFiles.flatMap { agent(it).tools ?: listOf() }.filter { it.startsWith("mcp__") }.toMutableList()
        for (kind in listOf("allow", "deny")) refs += rules(kind).filter { it.startsWith("mcp__") }
        assertFalse(refs.isEmpty(), "the setup refers to at least one MCP tool")
        for (ref in refs) assertTrue(configured.has(ref.split("__")[1]), "$ref names a server that .mcp.json does not configure")
    }

    @Test
    fun e1_credentialsComeFromTheEnvironmentAndTheTokenHasNoDefault() {
        val configured = servers()
        for ((name, server) in configured.fields()) {
            for (field in listOf("headers", "env")) {
                for ((key, value) in server.path(field).fields()) assertTrue(!secretKey.containsMatchIn(key) || "\${" in value.asText(), "$name $field.$key holds a literal credential")
            }
        }
        val auth = configured.path("tickets").path("headers").path("Authorization").asText("")
        assertTrue(Regex("\\$\\{[A-Z_][A-Z0-9_]*\\}").containsMatchIn(auth), "the tickets token is a \${VAR} reference")
        assertFalse(":-" in auth, "a default for a token would be a credential in the file")
    }

    @Test
    fun e2_theExplorerIsReadOnlyAndSaysWhenToUseIt() {
        val explorer = agent(".claude/agents/explorer.md")
        assertTrue(explorer.description.startsWith("Use when"), "the description says when to delegate")
        val tools = explorer.tools
        assertNotNull(tools, "list the tools: without a tools line the subagent inherits every tool")
        assertTrue("Read" in tools!! && "Grep" in tools, "an explorer reads and searches")
        assertEquals(listOf<String>(), tools.filter { it !in listOf("Read", "Grep", "Glob", "mcp__docs__search") }, "read-only tools only")
    }

    @Test
    fun e3_theScaffolderWritesOnlyInTheGeneratedFolder() {
        val tools = agent(".claude/agents/scaffolder.md").tools
        assertNotNull(tools, "list the tools: without a tools line the subagent inherits every tool")
        assertTrue("Edit" in tools!! && "Bash" !in tools, "the scaffolder edits files and runs no commands")
        val writes = rules("allow").filter { Regex("^(Edit|Write|MultiEdit)\\b").containsMatchIn(it) }
        assertFalse(writes.isEmpty(), "allow the scaffolder to edit the generated folder")
        for (rule in writes) assertTrue(Regex("Edit\\(src/generated/[^)]*\\)").matches(rule), "$rule is not an Edit rule limited to the generated folder (a Write path rule is never consulted)")
    }

    @Test
    fun e4_theTicketsServerIsReadOnlyForAgents() {
        val deny = rules("deny")
        for (tool in listOf("mcp__tickets__create_ticket", "mcp__tickets__delete_ticket")) assertTrue(tool in deny, "deny $tool")
        for (rule in rules("allow")) {
            if (rule.startsWith("mcp__tickets")) assertTrue(Regex("mcp__tickets__(get|list|search)_\\w+").matches(rule), "$rule approves more than reading tickets")
        }
    }

    @Test
    fun e5_theEnvironmentFileIsDeniedAndNoWholeToolIsApproved() {
        assertTrue("Read(./.env)" in rules("deny"), "deny reading the environment file")
        assertEquals(listOf<String>(), rules("allow").filter { it in listOf("Bash", "Bash(*)", "Edit", "Write", "Read", "mcp__tickets") }, "a bare allow rule approves every call of that tool")
    }

    @Test
    fun e6_theTeamNoteListsEveryVariableAndSaysWhichSessionToStart() {
        val note = read("docs/team-setup.md")
        for (v in Regex("\\$\\{([A-Za-z_]\\w*)").findAll(read(".mcp.json")).map { it.groupValues[1] }.toSet()) assertTrue(Regex("\\b$v\\b").containsMatchIn(note), "the note does not list $v")
        val rows = linkedMapOf<String, String>()
        for (line in note.split("\n")) {
            val cells = line.trim().replace(Regex("^\\||\\|$"), "").split("|")
            if (line.startsWith("|") && cells.size >= 2 && cells[1].trim().lowercase() in listOf("resume", "fork", "fresh")) rows[cells[0].trim().lowercase()] = cells[1].trim().lowercase()
        }
        for ((keyword, mode) in listOf("yesterday" to "resume", "compare" to "fork", "rewritten" to "fresh")) {
            val situation = rows.keys.firstOrNull { keyword in it }
            assertNotNull(situation, "the table has no row for the '$keyword' situation")
            assertEquals(mode, rows[situation], "'$keyword' should be $mode")
        }
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
