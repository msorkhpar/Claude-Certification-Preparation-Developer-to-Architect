import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PolicyResolverTest {
    @Test
    fun aHigherLevelWinsAndManagedIsTheHighest() {
        assertEquals(7, effective(linkedMapOf("managed" to mapOf("cleanupPeriodDays" to 7), "command line" to mapOf("cleanupPeriodDays" to 14), "project" to mapOf("cleanupPeriodDays" to 30), "user" to mapOf("cleanupPeriodDays" to 60))).settings["cleanupPeriodDays"])
        assertEquals(20, effective(linkedMapOf("project" to mapOf("cleanupPeriodDays" to 30), "user" to mapOf("cleanupPeriodDays" to 60), "local" to mapOf("cleanupPeriodDays" to 20))).settings["cleanupPeriodDays"])
    }

    @Test
    fun listsMergeAcrossLevelsWithoutDuplicatesUntilALockStopsIt() {
        val project = mapOf<String, Any>("permissions.allow" to listOf("a", "b"))
        val user = mapOf<String, Any>("permissions.allow" to listOf("b", "c"))
        assertEquals(listOf("a", "b", "c"), effective(linkedMapOf("project" to project, "user" to user)).settings["permissions.allow"])
        val locked = effective(linkedMapOf("managed" to mapOf("allowManagedPermissionRulesOnly" to true, "permissions.allow" to listOf("m")), "project" to project, "user" to user))
        assertEquals(listOf("m"), locked.settings["permissions.allow"])
        assertEquals(listOf("ignored permissions.allow from project: managed settings are the only source of permission rules", "ignored permissions.allow from user: managed settings are the only source of permission rules"), locked.notes)
    }

    @Test
    fun aKeyOnlyAnOrganisationCanSetIsIgnoredAnywhereElse() {
        val result = effective(linkedMapOf("user" to mapOf("strictKnownMarketplaces" to listOf<String>())))
        assertFalse(result.settings.containsKey("strictKnownMarketplaces"))
        assertEquals(listOf("ignored strictKnownMarketplaces from user: a managed-only key"), result.notes)
        assertEquals(listOf("one"), effective(linkedMapOf("managed" to mapOf("strictKnownMarketplaces" to listOf("one")), "user" to mapOf("strictKnownMarketplaces" to listOf("two")))).settings["strictKnownMarketplaces"])
    }

    @Test
    fun theLowestEffortCapWinsAndAConnectorBanFromAnyLevelStands() {
        assertEquals("low", effective(linkedMapOf("managed" to mapOf("maxEffortLevel" to "high"), "user" to mapOf("maxEffortLevel" to "low"), "project" to mapOf("maxEffortLevel" to "max"))).settings["maxEffortLevel"])
        assertEquals(true, effective(linkedMapOf("managed" to mapOf("disableClaudeAiConnectors" to false), "project" to mapOf("disableClaudeAiConnectors" to true))).settings["disableClaudeAiConnectors"])
    }

    @Test
    fun aManagedModelListRefusesEveryOtherChoiceExactly() {
        val settings = effective(linkedMapOf("managed" to mapOf("availableModels" to listOf("sonnet", "haiku")), "user" to mapOf("availableModels" to listOf("opus")))).settings
        assertEquals(listOf("sonnet", "haiku"), settings["availableModels"])
        assertEquals("allowed", allowedModel("haiku", settings))
        assertEquals("refused (not in availableModels)", allowedModel("opus", settings))
        assertEquals("allowed", allowedModel("anything", mapOf()))
    }
}
