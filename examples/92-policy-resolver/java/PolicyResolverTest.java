import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PolicyResolverTest {
    private static Map<String, Map<String, Object>> layers(Object... pairs) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            @SuppressWarnings("unchecked")
            Map<String, Object> level = (Map<String, Object>) pairs[i + 1];
            out.put((String) pairs[i], level);
        }
        return out;
    }

    @Test
    void aHigherLevelWinsAndManagedIsTheHighest() {
        assertEquals(7, PolicyResolver.effective(layers("managed", Map.of("cleanupPeriodDays", 7), "command line", Map.of("cleanupPeriodDays", 14), "project", Map.of("cleanupPeriodDays", 30), "user", Map.of("cleanupPeriodDays", 60))).settings().get("cleanupPeriodDays"));
        assertEquals(20, PolicyResolver.effective(layers("project", Map.of("cleanupPeriodDays", 30), "user", Map.of("cleanupPeriodDays", 60), "local", Map.of("cleanupPeriodDays", 20))).settings().get("cleanupPeriodDays"));
    }

    @Test
    void listsMergeAcrossLevelsWithoutDuplicatesUntilALockStopsIt() {
        Map<String, Object> project = Map.of("permissions.allow", List.of("a", "b"));
        Map<String, Object> user = Map.of("permissions.allow", List.of("b", "c"));
        assertEquals(List.of("a", "b", "c"), PolicyResolver.effective(layers("project", project, "user", user)).settings().get("permissions.allow"));
        PolicyResolver.Result locked = PolicyResolver.effective(layers("managed", Map.of("allowManagedPermissionRulesOnly", true, "permissions.allow", List.of("m")), "project", project, "user", user));
        assertEquals(List.of("m"), locked.settings().get("permissions.allow"));
        assertEquals(List.of("ignored permissions.allow from project: managed settings are the only source of permission rules", "ignored permissions.allow from user: managed settings are the only source of permission rules"), locked.notes());
    }

    @Test
    void aKeyOnlyAnOrganisationCanSetIsIgnoredAnywhereElse() {
        PolicyResolver.Result result = PolicyResolver.effective(layers("user", Map.of("strictKnownMarketplaces", List.of())));
        assertFalse(result.settings().containsKey("strictKnownMarketplaces"));
        assertEquals(List.of("ignored strictKnownMarketplaces from user: a managed-only key"), result.notes());
        assertEquals(List.of("one"), PolicyResolver.effective(layers("managed", Map.of("strictKnownMarketplaces", List.of("one")), "user", Map.of("strictKnownMarketplaces", List.of("two")))).settings().get("strictKnownMarketplaces"));
    }

    @Test
    void theLowestEffortCapWinsAndAConnectorBanFromAnyLevelStands() {
        assertEquals("low", PolicyResolver.effective(layers("managed", Map.of("maxEffortLevel", "high"), "user", Map.of("maxEffortLevel", "low"), "project", Map.of("maxEffortLevel", "max"))).settings().get("maxEffortLevel"));
        assertEquals(true, PolicyResolver.effective(layers("managed", Map.of("disableClaudeAiConnectors", false), "project", Map.of("disableClaudeAiConnectors", true))).settings().get("disableClaudeAiConnectors"));
    }

    @Test
    void aManagedModelListRefusesEveryOtherChoiceExactly() {
        Map<String, Object> settings = PolicyResolver.effective(layers("managed", Map.of("availableModels", List.of("sonnet", "haiku")), "user", Map.of("availableModels", List.of("opus")))).settings();
        assertEquals(List.of("sonnet", "haiku"), settings.get("availableModels"));
        assertEquals("allowed", PolicyResolver.allowedModel("haiku", settings));
        assertEquals("refused (not in availableModels)", PolicyResolver.allowedModel("opus", settings));
        assertEquals("allowed", PolicyResolver.allowedModel("anything", Map.of()));
    }
}
