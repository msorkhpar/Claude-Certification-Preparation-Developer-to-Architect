import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class McpConfigTest {
    @Test
    fun aVariableExpandsAndADefaultFillsAnUnsetOne() {
        assertEquals(Expanded("x-fallback-", emptyList()), expand("\${A}-\${B:-fallback}-\${C:-}", mapOf("A" to "x")))
    }

    @Test
    fun anUnsetVariableWithoutADefaultKeepsItsTextAndIsReported() {
        assertEquals(Expanded("Bearer \${TOKEN}", listOf("TOKEN")), expand("Bearer \${TOKEN}", emptyMap()))
    }

    @Test
    fun credentialVariablesReadEmptyTowardARemoteServerButNotForALocalOne() {
        val env = mapOf("NPM_TOKEN" to "t", "MY_TOKEN" to "m")
        assertEquals(Expanded("Bearer ", emptyList()), expand("Bearer \${NPM_TOKEN}", env, remote = true))
        assertEquals(Expanded("Bearer ", emptyList()), expand("Bearer \${NPM_TOKEN:-d}", emptyMap(), remote = true))
        assertEquals(Expanded("Bearer m", emptyList()), expand("Bearer \${MY_TOKEN}", env, remote = true))
        assertEquals(Expanded("t", emptyList()), expand("\${NPM_TOKEN}", env, remote = false))
    }

    @Test
    fun expansionCoversCommandArgsEnvUrlAndHeadersOnly() {
        val entry = parse("""{"type": "stdio", "command": "${'$'}{BIN:-run}", "args": ["${'$'}{A:-1}"], "env": {"K": "${'$'}{V:-v}"}, "note": "${'$'}{A}"}""")
        val out = expandServer(entry, emptyMap())
        assertEquals(parse("""{"type": "stdio", "command": "run", "args": ["1"], "env": {"K": "v"}, "note": "${'$'}{A}"}"""), out.entry)
        assertEquals(emptyList<String>(), out.warnings)
    }

    @Test
    fun theHighestScopeWinsTheWholeEntryAndAConflictIsReported() {
        val scopes = mapOf(
            "user" to mapOf("s" to parse("""{"url": "u", "extra": 1}""")),
            "project" to mapOf("s" to parse("""{"url": "p"}""")),
            "local" to emptyMap(),
        )
        val resolved = resolveServers(scopes)
        assertEquals(Source("project", parse("""{"url": "p"}""")), resolved.servers["s"])
        assertEquals(1, resolved.warnings.size)
        assertEquals("local", SCOPES[0])
    }

    @Test
    fun lintFindsLiteralSecretsAndMissingFields() {
        val findings = lint(parse("""{"mcpServers": {"a": {"type": "http", "headers": {"Authorization": "x"}}, "b": {}}}"""))
        assertEquals(listOf("a: a http server needs a url", "a: headers.Authorization holds a literal value, reference an environment variable", "b: a stdio server needs a command"), findings)
        assertEquals(emptyList<String>(), lint(parse("""{"mcpServers": {"a": {"type": "http", "url": "u", "headers": {"Authorization": "Bearer ${'$'}{T}"}}}}""")))
    }

    @Test
    fun aDescriptionIsCutAtTheLimit() {
        assertEquals(2048, truncate("x".repeat(5000)).length)
    }

    @Test
    fun onlyAnAllowRuleThatNamesItsServerIsHonouredAndDenyWins() {
        val s = parse("""{"permissions": {"allow": ["mcp__docs__*", "mcp__*", "mcp__github__get_*"], "deny": ["mcp__github__get_secret"]}}""")
        val got = listOf("mcp__docs__a", "mcp__other__a", "mcp__github__get_pr", "mcp__github__get_secret", "mcp__github__push").map { mcpDecision(s, it) }
        assertEquals(listOf("allow", "ask", "allow", "deny", "ask"), got)
    }
}
