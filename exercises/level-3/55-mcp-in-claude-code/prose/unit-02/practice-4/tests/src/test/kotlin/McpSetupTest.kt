import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class McpSetupTest {
    // config.dir is the project folder that holds .mcp.json and .claude/: starter, reference or a planted wrong solution.
    private val root: Path = Path.of(System.getProperty("config.dir", "starter"))
    private val servers = listOf("core", "docs", "github", "schema")

    private fun read(rel: String): String {
        val path = root.resolve(rel)
        assertTrue(Files.isRegularFile(path), "$rel is missing")
        return Files.readString(path)
    }

    private fun readJson(rel: String): Map<String, Any?> = try {
        parse(read(rel))
    } catch (error: Exception) {
        throw AssertionError("$rel is not valid JSON: ${error.message}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun asMap(o: Any?): Map<String, Any?> = o as? Map<String, Any?> ?: emptyMap()

    private fun asList(o: Any?): List<Any?> = o as? List<*> ?: emptyList<Any?>()

    private fun shared(): Map<String, Any?> = asMap(readJson(".mcp.json")["mcpServers"])

    private fun server(name: String): Map<String, Any?> {
        val found = shared()[name]
        assertTrue(found is Map<*, *>, ".mcp.json has no server named $name")
        return asMap(found)
    }

    @Test
    fun m1_theSharedFileDeclaresTheFourTeamServersWithTheRightShape() {
        assertEquals(servers, shared().keys.sorted(), "declare exactly core, docs, github and schema")
        assertTrue(server("github")["type"] == "http" && server("core")["type"] == "http", "github and core are remote http servers")
        assertTrue((server("docs")["type"] ?: "stdio") == "stdio" && (server("schema")["type"] ?: "stdio") == "stdio", "docs and schema run as local processes")
        assertTrue(server("docs")["command"] == "python3" && server("schema")["command"] == "python3")
        assertEquals(emptyList<String>(), lint(mapOf("mcpServers" to shared())))
    }

    @Test
    fun e1_credentialsAreReadFromTheEnvironmentAndNeverWrittenInTheFile() {
        assertEquals("Bearer \${GITHUB_TOKEN}", asMap(server("github")["headers"])["Authorization"], "send the token as Bearer \${GITHUB_TOKEN}")
        assertEquals("\${DOCS_API_KEY}", asMap(server("docs")["env"])["DOCS_API_KEY"], "pass DOCS_API_KEY through from the environment")
        val names = Regex("""\$\{([A-Za-z_][A-Za-z0-9_]*)(?::-[^}]*)?\}""").findAll(show(server("github")["headers"] ?: emptyMap<String, Any?>())).map { it.groupValues[1] }.toSet()
        assertTrue((names intersect COVERED).isEmpty(), "a credential name that Claude Code reads as empty toward a remote server cannot carry the token")
        for (entry in shared().values) {
            for (field in listOf("headers", "env")) {
                for ((key, value) in asMap(asMap(entry)[field])) {
                    assertTrue(Regex("""\$\{[A-Za-z_]\w*\}""").containsMatchIn(value as String) && ":-" !in value, "$key: no default value for a credential, it would be committed")
                }
            }
        }
        val out = expandServer(server("github"), mapOf("GITHUB_TOKEN" to "t"))
        assertEquals("Bearer t", asMap(out.entry["headers"])["Authorization"])
        assertEquals(emptyList<String>(), out.warnings)
    }

    @Test
    fun e2_endpointsAndPathsThatAreNotSecretHaveADefaultSoTheFileWorksUnset() {
        for (name in listOf("github", "core")) {
            assertTrue(Regex("""\$\{[A-Z_]+:-https://[^}]+\}""").matches(server(name)["url"]?.toString() ?: ""), "$name: url needs a \${VAR:-default} with an https default")
        }
        for (name in listOf("docs", "schema")) {
            val args = asList(server(name)["args"])
            assertTrue(args.isNotEmpty() && (args[0] as String).startsWith("\${CLAUDE_PROJECT_DIR:-.}/"), "$name: CLAUDE_PROJECT_DIR is set for the server, not for the command, so it needs a default")
            val out = expandServer(server(name), emptyMap())
            assertTrue(out.warnings.none { "CLAUDE_PROJECT_DIR" in it } && (asList(out.entry["args"])[0] as String).startsWith("./tools/"))
        }
        val url = expandServer(server("github"), emptyMap()).entry["url"] as String
        assertTrue(url.startsWith("https://") && "\${" !in url)
    }

    @Test
    fun e3_onlyTheSmallCoreServerIsLoadedAtTheStart() {
        assertEquals(true, server("core")["alwaysLoad"], "core is used on every turn: alwaysLoad true")
        val others = servers.filter { it != "core" && asMap(shared()[it])["alwaysLoad"] == true }
        assertEquals(emptyList<String>(), others, "tool search should find the others on demand")
    }

    @Test
    fun e4_sharedServersStayInTheProjectFileAndPersonalOnesInTheUserScopeFile() {
        val user = asMap(readJson("user-scope.example.json")["mcpServers"])
        assertEquals(listOf("scratch"), user.keys.toList(), "the user-scope example holds one personal server, scratch")
        assertFalse("scratch" in shared(), "a personal server does not belong in the committed file")
        assertTrue(user.keys.none { it in shared() }, "the same name in two scopes: the whole entry of the higher scope wins and a warning is shown")
        val resolved = resolveServers(mapOf("project" to shared().mapValues { asMap(it.value) }, "user" to user.mapValues { asMap(it.value) }))
        assertEquals((servers + "scratch").sorted(), resolved.servers.keys.sorted())
        assertEquals(emptyList<String>(), resolved.warnings)
        assertTrue(user.values.none { Regex("/home/|/Users/").containsMatchIn(show(it)) }, "no absolute home path: use \${HOME}")
    }

    @Test
    fun e5_permissionsAllowTheReadOnlyServersByNameAndDenyTheDestructiveTool() {
        val settings = readJson(".claude/settings.json")
        val table = linkedMapOf(
            "mcp__docs__search_docs" to "allow", "mcp__schema__read_schema" to "allow", "mcp__github__list_pull_requests" to "ask", "mcp__github__create_issue" to "ask",
            "mcp__github__delete_repository" to "deny", "mcp__core__ping" to "ask", "mcp__scratch__anything" to "ask",
        )
        val wrong = table.mapNotNull { (tool, want) -> mcpDecision(settings, tool).takeIf { it != want }?.let { "$tool: got $it, want $want" } }
        assertEquals(emptyList<String>(), wrong)
        val rules = asList(asMap(settings["permissions"])["allow"]).map { it.toString() }
        assertTrue(rules.none { it == "*" || it == "mcp__*" || it.startsWith("mcp__*") }, "an allow rule must name its server; mcp__* is ignored")
    }

    @Test
    fun e6_theToolDescriptionSaysWhenToUseItInsteadOfGrepAndFitsTheLimit() {
        val tools = asList(readJson("docs/tool-descriptions.json")["tools"])
        val found = tools.map { asMap(it) }.filter { it["name"] == "search_docs" }
        assertTrue(found.isNotEmpty(), "describe the search_docs tool")
        val text = found[0]["description"]?.toString() ?: ""
        assertTrue(text.length <= 2048 && truncate(text) == text, "Claude Code cuts a description at 2,048 characters")
        assertTrue(Regex("instead of Grep").containsMatchIn(text.take(300)), "put the boundary against Grep in the first 300 characters")
        assertTrue("Returns" in text || "returns" in text, "say what comes back")
        assertTrue(Regex("""\b(does not|doesn't|not search)\b""").containsMatchIn(text), "say what it does not do")
        val props = asMap(asMap(found[0]["inputSchema"])["properties"])
        assertTrue(props.isNotEmpty() && props.values.all { !asMap(it)["description"]?.toString().isNullOrEmpty() }, "every parameter needs a description")
        assertEquals(listOf("query"), asMap(found[0]["inputSchema"])["required"])
    }

    @Test
    fun e7_theNotesListEveryServerWithItsScopeAndReadTheCatalogAsAResource() {
        val notes = read("docs/mcp-servers.md")
        for (name in servers) {
            val row = Regex("""(?m)^\|\s*`$name`\s*\|\s*(\w+)\s*\|(.*)$""").find(notes)
            assertNotNull(row, "add a table row for $name")
            assertEquals("project", row!!.groupValues[1], "$name is shared, so its scope is project")
        }
        val refs = Regex("""@([\w-]+):(\w+://[\w./-]+)""").findAll(notes).map { it.groupValues[1] to it.groupValues[2] }.toList()
        assertTrue(refs.isNotEmpty() && refs.all { it.first in servers }, "@server:protocol://path must name a configured server")
        assertTrue(("schema" to "schema://orders") in refs, "show how to read the orders schema")
        val row = Regex("""(?m)^\|\s*`schema`.*$""").find(notes)
        assertTrue(row != null && "resource" in row.value.lowercase(), "the schema server exposes resources")
    }

    @Test
    fun e8_noFileHoldsAPersonalPathAnAddressOrAKey() {
        val patterns = listOf(
            "home path" to Regex("""(/home/\w+|/Users/\w+|C:\\Users)"""), "email address" to Regex("""[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"""),
            "key" to Regex("""(sk-ant-[\w-]{6,}|ghp_\w{6,}|Bearer [A-Za-z0-9]{12,})"""),
        )
        val hits = root.toFile().walkTopDown().filter(File::isFile).sortedBy { it.path }.flatMap { file ->
            val text = file.readText(Charsets.UTF_8)
            patterns.filter { (_, rx) -> rx.containsMatchIn(text) }.map { (label, _) -> "${file.relativeTo(root.toFile())}: $label" }
        }.toList()
        assertEquals(emptyList<String>(), hits)
    }
}
