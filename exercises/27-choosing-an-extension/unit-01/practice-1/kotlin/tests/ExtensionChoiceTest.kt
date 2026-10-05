import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtensionChoiceTest {
    private fun decide(situation: Map<String, Any>): Choice {
        val value: Choice? = choose(situation.toMap(LinkedHashMap()))
        assertNotNull(value, "choose returned nothing")
        return value!!
    }

    private fun refused(situation: Map<String, Any>): Boolean = try {
        decide(situation)
        false
    } catch (error: IllegalArgumentException) {
        true
    }

    /** Every combination of the axes, each added to the base situation. */
    private fun sweep(base: Map<String, Any>, vararg axes: Pair<String, List<Any>>): List<Map<String, Any>> {
        var combos: List<Map<String, Any>> = listOf(base)
        for ((key, values) in axes) combos = combos.flatMap { c -> values.map { v -> c + (key to v) } }
        return combos
    }

    // The scenario bank of the page: id, situation, expected mechanism and reason code.
    private data class Row(val id: String, val situation: Map<String, Any>, val mechanism: String, val reason: String)

    private val BANK = listOf(
        Row("s01", mapOf("knowledge" to "convention"), "claude-md", "always-known"),
        Row("s02", mapOf("guarantee" to true, "knowledge" to "convention"), "hook", "must-hold-every-time"),
        Row("s03", mapOf("guarantee" to true), "hook", "must-hold-every-time"),
        Row("s04", mapOf("knowledge" to "convention", "path_scoped" to true), "path-rule", "scoped-convention"),
        Row("s05", mapOf("knowledge" to "reference"), "skill", "on-demand-reference"),
        Row("s06", mapOf("knowledge" to "procedure"), "skill", "repeatable-procedure"),
        Row("s07", mapOf("external_system" to true), "mcp", "external-system"),
        Row("s08", mapOf("noisy" to true), "subagent", "isolate-context"),
        Row("s09", emptyMap(), "builtin-tool", "built-in-covers"),
        Row("s10", mapOf("external_system" to true, "knowledge" to "procedure", "repos" to 6), "plugin", "shared-setup"),
        Row("s11", mapOf("knowledge" to "procedure", "repos" to 2), "plugin", "shared-setup"),
        Row("s12", mapOf("guarantee" to true, "external_system" to true), "hook", "must-hold-every-time"),
        Row("s13", mapOf("external_system" to true, "noisy" to true), "mcp", "external-system"),
        Row("s14", mapOf("knowledge" to "convention", "repos" to 6), "claude-md", "always-known"),
        Row("s15", mapOf("noisy" to true, "repos" to 4), "plugin", "shared-setup"),
        Row("s16", mapOf("surface" to "api"), "api-tool", "own-schema-and-code"),
        Row("s17", mapOf("surface" to "api", "builtin_covers" to true), "builtin-tool", "provided-schema"),
        Row("s18", mapOf("surface" to "api", "external_system" to true, "remote_server" to true), "mcp", "remote-server")
    )

    @Test
    fun m1_everySituationOfTheBankGetsItsMechanismAndItsReason() {
        val wrong = BANK.filter { decide(it.situation) != Choice(it.mechanism, it.reason) }.map { it.id }
        assertEquals(emptyList<String>(), wrong, "these situations got the wrong mechanism or reason")
    }

    @Test
    fun e1_aRuleThatMustHoldGoesToAHookWhateverElseIsTrue() {
        for (s in sweep(mapOf("guarantee" to true), "knowledge" to listOf("none", "convention", "reference", "procedure"), "external_system" to listOf(false, true), "noisy" to listOf(false, true), "path_scoped" to listOf(false, true)))
            assertEquals(Choice("hook", "must-hold-every-time"), decide(s), s.toString())
    }

    @Test
    fun e2_anOutsideSystemNeedsAServerAndNoisyWorkAloneNeedsASubagent() {
        for (s in sweep(mapOf("external_system" to true), "knowledge" to listOf("none", "convention", "reference", "procedure"), "noisy" to listOf(false, true), "path_scoped" to listOf(false, true)))
            assertEquals(Choice("mcp", "external-system"), decide(s), s.toString())
        for (s in sweep(mapOf("noisy" to true), "knowledge" to listOf("none", "convention", "reference", "procedure"), "path_scoped" to listOf(false, true)))
            assertEquals(Choice("subagent", "isolate-context"), decide(s), s.toString())
    }

    @Test
    fun e3_knowledgeGoesToTheFileOrSkillThatLoadsItAtTheRightTime() {
        assertEquals(Choice("claude-md", "always-known"), decide(mapOf("knowledge" to "convention")), "{\"knowledge\": \"convention\"}")
        assertEquals(Choice("path-rule", "scoped-convention"), decide(mapOf("knowledge" to "convention", "path_scoped" to true)), "{\"knowledge\": \"convention\", \"path_scoped\": true}")
        assertEquals(Choice("skill", "on-demand-reference"), decide(mapOf("knowledge" to "reference")), "{\"knowledge\": \"reference\"}")
        assertEquals(Choice("skill", "repeatable-procedure"), decide(mapOf("knowledge" to "procedure")), "{\"knowledge\": \"procedure\"}")
        assertEquals(Choice("skill", "on-demand-reference"), decide(mapOf("knowledge" to "reference", "path_scoped" to true)), "{\"knowledge\": \"reference\", \"path_scoped\": true}")
        assertEquals(Choice("builtin-tool", "built-in-covers"), decide(mapOf("path_scoped" to true)), "{\"path_scoped\": true}")
        assertEquals(Choice("skill", "repeatable-procedure"), decide(mapOf("builtin_covers" to true, "knowledge" to "procedure")), "{\"builtin_covers\": true, \"knowledge\": \"procedure\"}")
    }

    @Test
    fun e4_aPluginCarriesASkillHookSubagentOrServerToASecondRepositoryAndNothingElse() {
        assertEquals(Choice("skill", "repeatable-procedure"), decide(mapOf("knowledge" to "procedure", "repos" to 1)), "{\"knowledge\": \"procedure\", \"repos\": 1}")
        assertEquals(Choice("hook", "must-hold-every-time"), decide(mapOf("guarantee" to true, "repos" to 1)), "{\"guarantee\": true, \"repos\": 1}")
        assertEquals(Choice("mcp", "external-system"), decide(mapOf("external_system" to true, "repos" to 1)), "{\"external_system\": true, \"repos\": 1}")
        assertEquals(Choice("subagent", "isolate-context"), decide(mapOf("noisy" to true, "repos" to 1)), "{\"noisy\": true, \"repos\": 1}")
        assertEquals(Choice("plugin", "shared-setup"), decide(mapOf("knowledge" to "procedure", "repos" to 2)), "{\"knowledge\": \"procedure\", \"repos\": 2}")
        assertEquals(Choice("plugin", "shared-setup"), decide(mapOf("guarantee" to true, "repos" to 2)), "{\"guarantee\": true, \"repos\": 2}")
        assertEquals(Choice("plugin", "shared-setup"), decide(mapOf("external_system" to true, "repos" to 3)), "{\"external_system\": true, \"repos\": 3}")
        assertEquals(Choice("plugin", "shared-setup"), decide(mapOf("noisy" to true, "repos" to 2)), "{\"noisy\": true, \"repos\": 2}")
        assertEquals(Choice("claude-md", "always-known"), decide(mapOf("knowledge" to "convention", "repos" to 2)), "{\"knowledge\": \"convention\", \"repos\": 2}")
        assertEquals(Choice("path-rule", "scoped-convention"), decide(mapOf("knowledge" to "convention", "path_scoped" to true, "repos" to 5)), "{\"knowledge\": \"convention\", \"path_scoped\": true, \"repos\": 5}")
        assertEquals(Choice("builtin-tool", "built-in-covers"), decide(mapOf("repos" to 9)), "{\"repos\": 9}")
    }

    @Test
    fun e5_inAnApplicationThePlatformMaySupplyTheSchemaAndOnlyARemoteServerReplacesYourOwnTool() {
        assertEquals(Choice("api-tool", "own-schema-and-code"), decide(mapOf("surface" to "api")), "{\"surface\": \"api\"}")
        assertEquals(Choice("builtin-tool", "provided-schema"), decide(mapOf("surface" to "api", "builtin_covers" to true, "external_system" to true, "remote_server" to true)), "{\"surface\": \"api\", \"builtin_covers\": true, \"external_system\": true, \"remote_server\": true}")
        assertEquals(Choice("mcp", "remote-server"), decide(mapOf("surface" to "api", "external_system" to true, "remote_server" to true)), "{\"surface\": \"api\", \"external_system\": true, \"remote_server\": true}")
        assertEquals(Choice("api-tool", "own-schema-and-code"), decide(mapOf("surface" to "api", "external_system" to true)), "{\"surface\": \"api\", \"external_system\": true}")
        assertEquals(Choice("api-tool", "own-schema-and-code"), decide(mapOf("surface" to "api", "remote_server" to true)), "{\"surface\": \"api\", \"remote_server\": true}")
        assertEquals(Choice("api-tool", "own-schema-and-code"), decide(mapOf("surface" to "api", "guarantee" to true, "knowledge" to "convention", "noisy" to true, "repos" to 4)), "{\"surface\": \"api\", \"guarantee\": true, \"knowledge\": \"convention\", \"noisy\": true, \"repos\": 4}")
    }

    @Test
    fun e6_anUnknownValueIsAnErrorAndAMissingKeyTakesItsDefault() {
        assertEquals(Choice("builtin-tool", "built-in-covers"), decide(emptyMap()), "{}")
        assertTrue(refused(mapOf("surface" to "cli")), "{\"surface\": \"cli\"} must be an error")
        assertTrue(refused(mapOf("knowledge" to "tips")), "{\"knowledge\": \"tips\"} must be an error")
        assertTrue(refused(mapOf("repos" to 0)), "{\"repos\": 0} must be an error")
        assertTrue(refused(mapOf("repos" to -1)), "{\"repos\": -1} must be an error")
        assertTrue(refused(mapOf("surface" to "api", "knowledge" to "tips")), "{\"surface\": \"api\", \"knowledge\": \"tips\"} must be an error")
    }
}
