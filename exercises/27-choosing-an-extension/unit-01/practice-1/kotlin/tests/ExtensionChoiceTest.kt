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
        Row("s18", mapOf("surface" to "api", "external_system" to true, "remote_server" to true), "mcp", "remote-server"),
        Row("s19", mapOf("timing" to "interval"), "loop", "session-rhythm"),
        Row("s20", mapOf("timing" to "interval", "presence" to "away"), "routine", "runs-unattended"),
        Row("s21", mapOf("timing" to "event"), "monitor", "push-not-poll"),
        Row("s22", mapOf("timing" to "background"), "background-task", "work-while-it-runs"),
        Row("s23", mapOf("presence" to "pipeline"), "headless-ci", "no-person-present"),
        Row("s24", mapOf("timing" to "condition"), "goal", "until-condition-holds"),
        Row("s25", mapOf("personal" to "voice"), "output-style", "response-voice"),
        Row("s26", mapOf("personal" to "display"), "status-line", "personal-display"),
        Row("s27", mapOf("timing" to "interval", "lasts_days" to 30, "local_files" to true), "desktop-task", "durable-and-local"),
        Row("s28", mapOf("personal" to "keys"), "keybinding", "personal-keys")
    )

    @Test
    fun m1_everySituationOfTheBankGetsItsMechanismAndItsReason() {
        val wrong = BANK.filter { decide(it.situation) != Choice(it.mechanism, it.reason) }.map { it.id }
        assertEquals(emptyList<String>(), wrong, "these situations got the wrong mechanism or reason")
    }

    @Test
    fun e1_aRuleThatMustHoldGoesToAHookWhateverElseIsTrue() {
        for (s in sweep(mapOf("guarantee" to true), "knowledge" to listOf("none", "convention", "reference", "procedure"), "external_system" to listOf(false, true), "noisy" to listOf(false, true), "path_scoped" to listOf(false, true), "timing" to listOf("none", "interval", "event", "condition", "background"), "presence" to listOf("session", "pipeline", "away"), "personal" to listOf("none", "voice")))
            assertEquals(Choice("hook", "must-hold-every-time"), decide(s), s.toString())
    }

    @Test
    fun e2_anOutsideSystemNeedsAServerAndNoisyWorkAloneNeedsASubagent() {
        for (s in sweep(mapOf("external_system" to true), "knowledge" to listOf("none", "convention", "reference", "procedure"), "noisy" to listOf(false, true), "path_scoped" to listOf(false, true), "timing" to listOf("none", "interval", "event", "condition", "background"), "presence" to listOf("session", "pipeline", "away")))
            assertEquals(Choice("mcp", "external-system"), decide(s), s.toString())
        for (s in sweep(mapOf("noisy" to true), "knowledge" to listOf("none", "convention", "reference", "procedure"), "path_scoped" to listOf(false, true), "timing" to listOf("none", "interval", "event", "condition", "background"), "presence" to listOf("session", "pipeline", "away")))
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
        assertTrue(refused(mapOf("timing" to "weekly")), "{\"timing\": \"weekly\"} must be an error")
        assertTrue(refused(mapOf("presence" to "cloud")), "{\"presence\": \"cloud\"} must be an error")
        assertTrue(refused(mapOf("personal" to "theme")), "{\"personal\": \"theme\"} must be an error")
        assertTrue(refused(mapOf("lasts_days" to 0)), "{\"lasts_days\": 0} must be an error")
        assertTrue(refused(mapOf("presence" to "away", "local_files" to true, "timing" to "interval")), "{\"presence\": \"away\", \"local_files\": true, \"timing\": \"interval\"} must be an error")
        assertTrue(refused(mapOf("surface" to "api", "timing" to "weekly")), "{\"surface\": \"api\", \"timing\": \"weekly\"} must be an error")
    }

    @Test
    fun e7_aPipelineAConditionALongCommandAndAnEventAreNotIntervals() {
        assertEquals(Choice("headless-ci", "no-person-present"), decide(mapOf("presence" to "pipeline")), "{\"presence\": \"pipeline\"}")
        assertEquals(Choice("headless-ci", "no-person-present"), decide(mapOf("presence" to "pipeline", "timing" to "interval", "lasts_days" to 30)), "{\"presence\": \"pipeline\", \"timing\": \"interval\", \"lasts_days\": 30}")
        assertEquals(Choice("goal", "until-condition-holds"), decide(mapOf("timing" to "condition", "lasts_days" to 30)), "{\"timing\": \"condition\", \"lasts_days\": 30}")
        assertEquals(Choice("goal", "until-condition-holds"), decide(mapOf("timing" to "condition", "presence" to "away")), "{\"timing\": \"condition\", \"presence\": \"away\"}")
        assertEquals(Choice("background-task", "work-while-it-runs"), decide(mapOf("timing" to "background", "presence" to "away")), "{\"timing\": \"background\", \"presence\": \"away\"}")
        assertEquals(Choice("monitor", "push-not-poll"), decide(mapOf("timing" to "event")), "{\"timing\": \"event\"}")
        assertEquals(Choice("routine", "runs-unattended"), decide(mapOf("timing" to "event", "presence" to "away")), "{\"timing\": \"event\", \"presence\": \"away\"}")
    }

    @Test
    fun e8_anIntervalIsALoopInTheSessionAndARoutineOrDesktopTaskWhenItMustOutliveIt() {
        assertEquals(Choice("loop", "session-rhythm"), decide(mapOf("timing" to "interval", "lasts_days" to 7)), "{\"timing\": \"interval\", \"lasts_days\": 7}")
        assertEquals(Choice("routine", "runs-unattended"), decide(mapOf("timing" to "interval", "lasts_days" to 8)), "{\"timing\": \"interval\", \"lasts_days\": 8}")
        assertEquals(Choice("desktop-task", "durable-and-local"), decide(mapOf("timing" to "interval", "lasts_days" to 8, "local_files" to true)), "{\"timing\": \"interval\", \"lasts_days\": 8, \"local_files\": true}")
        assertEquals(Choice("loop", "session-rhythm"), decide(mapOf("timing" to "interval", "local_files" to true)), "{\"timing\": \"interval\", \"local_files\": true}")
        assertEquals(Choice("routine", "runs-unattended"), decide(mapOf("timing" to "interval", "presence" to "away", "lasts_days" to 30)), "{\"timing\": \"interval\", \"presence\": \"away\", \"lasts_days\": 30}")
        assertEquals(Choice("routine", "runs-unattended"), decide(mapOf("timing" to "interval", "presence" to "away")), "{\"timing\": \"interval\", \"presence\": \"away\"}")
        assertEquals(Choice("loop", "session-rhythm"), decide(mapOf("timing" to "interval", "presence" to "session")), "{\"timing\": \"interval\", \"presence\": \"session\"}")
    }

    @Test
    fun e9_aPersonalPreferenceGoesToAStyleAStatusLineOrAKeyBindingAndKnowledgeKeepsItsOwnRules() {
        assertEquals(Choice("output-style", "response-voice"), decide(mapOf("personal" to "voice")), "{\"personal\": \"voice\"}")
        assertEquals(Choice("status-line", "personal-display"), decide(mapOf("personal" to "display")), "{\"personal\": \"display\"}")
        assertEquals(Choice("keybinding", "personal-keys"), decide(mapOf("personal" to "keys")), "{\"personal\": \"keys\"}")
        assertEquals(Choice("output-style", "response-voice"), decide(mapOf("personal" to "voice", "knowledge" to "convention")), "{\"personal\": \"voice\", \"knowledge\": \"convention\"}")
        assertEquals(Choice("keybinding", "personal-keys"), decide(mapOf("personal" to "keys", "knowledge" to "reference", "repos" to 4)), "{\"personal\": \"keys\", \"knowledge\": \"reference\", \"repos\": 4}")
        assertEquals(Choice("status-line", "personal-display"), decide(mapOf("personal" to "display", "repos" to 3)), "{\"personal\": \"display\", \"repos\": 3}")
        assertEquals(Choice("claude-md", "always-known"), decide(mapOf("personal" to "none", "knowledge" to "convention")), "{\"personal\": \"none\", \"knowledge\": \"convention\"}")
        assertEquals(Choice("loop", "session-rhythm"), decide(mapOf("personal" to "voice", "timing" to "interval")), "{\"personal\": \"voice\", \"timing\": \"interval\"}")
        assertEquals(Choice("api-tool", "own-schema-and-code"), decide(mapOf("personal" to "voice", "surface" to "api")), "{\"personal\": \"voice\", \"surface\": \"api\"}")
    }
}
