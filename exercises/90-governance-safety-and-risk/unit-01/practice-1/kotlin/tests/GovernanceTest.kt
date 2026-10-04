import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class GovernanceTest {
    // the folder that holds governance/ and docs/: the starter, the reference or a planted wrong solution
    private val root: Path = Path.of(System.getProperty("solution.dir", "starter"))

    private val badOwners = setOf("", "tbd", "everyone", "team", "n/a", "none")

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

    private fun controls(): JsonNode = loadJson("governance/controls.json").path("controls")

    private fun registerRows(): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        for (line in read("docs/risk-register.md").split("\n")) {
            if (line.startsWith("|")) {
                val cells = line.trim().replace(Regex("^\\||\\|$"), "").split("|").map { it.trim() }
                if (!cells.all { it.matches(Regex("[-: ]*")) }) rows += cells
            }
        }
        return rows.drop(1)
    }

    @Test
    fun m1_noControlThatGuardsAnInputOrAnActionFailsOpen() {
        val found = controls()
        assertTrue(found.size() > 0, "list the controls")
        for (c in found) {
            val id = c.path("id").asText()
            val onFailure = c.path("on_failure").asText()
            assertTrue(onFailure in listOf("hold", "proceed-flagged"), "$id: on_failure is hold or proceed-flagged")
            if (c.path("tier").asText() == "high" || c.path("layer").asText() in listOf("input", "action")) {
                assertEquals("hold", onFailure, "$id guards an input or an action and must hold when it fails")
            }
        }
    }

    @Test
    fun e1_everyHighConsequenceActionHasAHumanStepThatExistsAndHolds() {
        val data = loadJson("governance/controls.json")
        val actions = data.path("high_consequence_actions")
        assertTrue(actions.size() > 0, "name the high consequence actions")
        for (a in actions) {
            val action = a.asText()
            val step = data.path("human_review").path(action).asText("")
            assertFalse(step.isEmpty(), "$action has no human review step")
            val control = data.path("controls").firstOrNull { it.path("id").asText() == step }
            assertNotNull(control, "$action names the control $step, which is not defined")
            assertTrue(control!!.path("tier").asText() == "high" && control.path("on_failure").asText() == "hold", "$step must be a high tier control that holds")
        }
    }

    @Test
    fun e2_theAutomaticThresholdIsAtLeast95AndAHighConsequenceActionIsNeverAutomatic() {
        val routing = loadJson("governance/routing.json")
        val threshold = routing.path("auto_confidence_min")
        assertTrue(threshold.isInt && threshold.asInt() in 95..100, "auto_confidence_min is a whole number from 95 to 100")
        assertTrue(routing.path("high_consequence_auto").isBoolean && !routing.path("high_consequence_auto").asBoolean(), "a high consequence action is never automatic")
        assertEquals("hold", routing.path("unsupported_answer").asText(), "an unsupported answer is held")
    }

    @Test
    fun e3_retentionKeepsAtLeast90DaysWithinTheCeilingAndStoresNoContent() {
        val audit = loadJson("governance/retention.json").path("audit")
        val floor = audit.path("floor_days").asInt(0)
        val retain = audit.path("retain_days").asInt(-1)
        val ceiling = audit.path("ceiling_days").asInt(-1)
        assertTrue(floor >= 90, "the audit floor is at least 90 days")
        assertTrue(floor <= retain && retain <= ceiling, "retain between the floor and the ceiling")
        assertTrue(audit.path("store_content").isBoolean && !audit.path("store_content").asBoolean(), "the audit log stores no content")
        assertTrue(audit.path("legal_hold_overrides_ceiling").asBoolean(false), "a legal hold outranks the ceiling")
    }

    @Test
    fun e4_theRiskRegisterNamesAControlAndAnOwnerForEachOfFourFailureModes() {
        val ids = controls().map { it.path("id").asText() }.toSet()
        val rows = registerRows()
        assertTrue(rows.size >= 4, "the register has a row for each of four failure modes")
        for (row in rows) {
            assertTrue(row.size >= 5, "$row is missing a column")
            assertTrue(row[2] in ids, "the control '${row[2]}' is not defined in controls.json")
            assertFalse(row[3].lowercase() in badOwners, "${row[1]}: name an owner")
        }
        val modes = rows.joinToString(" ") { it[1].lowercase() }
        for (word in listOf("hallucination", "prompt injection", "privacy", "unfair")) assertTrue(word in modes, "no row covers $word")
    }

    @Test
    fun e5_usersAreToldThatAiHelpedAndNoFileHoldsPersonalData() {
        assertTrue(Regex("told that ai (helped|assisted)", RegexOption.IGNORE_CASE).containsMatchIn(read("docs/risk-register.md")), "say that users are told that AI helped")
        val hits = mutableListOf<String>()
        val home = Regex("(/home/\\w+|/Users/\\w+|C:\\\\Users)")
        val email = Regex("[\\w.+-]+@(?!example\\.(com|invalid))[\\w-]+\\.[\\w.]+")
        Files.walk(root).use { s ->
            for (p in s.filter { Files.isRegularFile(it) }.sorted().toList()) {
                val text = Files.readString(p)
                val rel = root.relativize(p).toString()
                if (home.containsMatchIn(text)) hits += "$rel: home path"
                if (email.containsMatchIn(text)) hits += "$rel: email address"
            }
        }
        assertEquals(listOf<String>(), hits)
    }

    @Test
    fun e6_erasureRemovesTheVaultMappingWithin30Days() {
        val erasure = loadJson("governance/retention.json").path("erasure")
        assertTrue(erasure.path("remove_vault_mapping").asBoolean(false), "erasure removes the map from tokens to people")
        val days = erasure.path("max_days_to_complete").asInt(999)
        assertTrue(days in 1..30, "erasure completes within 30 days")
    }
}
