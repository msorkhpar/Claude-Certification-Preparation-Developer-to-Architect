import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CapabilityAuditTest {
    @Test
    void aRoleLosesTheToolsItDoesNotNeedAndTheRiskyOnesAreNamed() {
        var result = CapabilityAudit.audit(List.copyOf(CapabilityAudit.CATALOG.keySet()), List.of("read_ticket", "draft_reply"), CapabilityAudit.CATALOG);
        assertEquals(new CapabilityAudit.Audit(List.of("issue_refund", "delete_account"), List.of("issue_refund", "delete_account"), List.of()), result);
        assertEquals(new CapabilityAudit.Audit(List.of(), List.of(), List.of("draft_reply")), CapabilityAudit.audit(List.of("read_ticket"), List.of("read_ticket", "draft_reply"), CapabilityAudit.CATALOG));
    }

    @Test
    void aLargeToolSetDefersAllButTheMostUsedAndASmallOneLoadsWhole() {
        var plan = CapabilityAudit.planLoading(CapabilityAudit.TOOLS, CapabilityAudit.USAGE);
        assertTrue(plan.search());
        assertEquals(List.of("github_create_issue", "github_search_code", "slack_post_message", "github_get_pr"), plan.loadNow());
        assertEquals(20, plan.deferred().size());
        assertEquals(2320, plan.tokens());
        Map<String, Integer> small = CapabilityAudit.TOOLS.keySet().stream().limit(9).collect(Collectors.toMap(n -> n, n -> 100, (a, b) -> a, java.util.LinkedHashMap::new));
        assertEquals(new CapabilityAudit.Plan(false, List.copyOf(small.keySet()), List.of(), 900), CapabilityAudit.planLoading(small, CapabilityAudit.USAGE));
        assertTrue(CapabilityAudit.planLoading(Map.of("a", 6000, "b", 5000), Map.of(), 4, 350).search());
        assertEquals(5, CapabilityAudit.planLoading(CapabilityAudit.TOOLS, CapabilityAudit.USAGE, 9, 350).loadNow().size());
        assertEquals(3, CapabilityAudit.planLoading(CapabilityAudit.TOOLS, CapabilityAudit.USAGE, 1, 350).loadNow().size());
    }

    @Test
    void theMechanismFollowsTheCounterpartThenThePathThenTheNumberOfClients() {
        assertEquals("agent-to-agent", CapabilityAudit.chooseMechanism(1, "agent", "fixed"));
        assertEquals("direct call in code", CapabilityAudit.chooseMechanism(4, "tool", "fixed"));
        assertEquals("MCP server", CapabilityAudit.chooseMechanism(4, "tool", "model-chosen"));
        assertEquals("custom tool", CapabilityAudit.chooseMechanism(1, "tool", "model-chosen"));
    }

    @Test
    void theAgentRightsAreACeilingAndTheUserRightsDecide() {
        Map<String, String> required = Map.of("issue_refund", "refunds:write");
        assertEquals("deny: user lacks refunds:write", CapabilityAudit.authorize("issue_refund", Set.of("tickets:read"), Set.of("refunds:write"), required));
        assertEquals("deny: agent lacks refunds:write", CapabilityAudit.authorize("issue_refund", Set.of("refunds:write"), Set.of("tickets:read"), required));
        assertEquals("allow", CapabilityAudit.authorize("issue_refund", Set.of("refunds:write"), Set.of("refunds:write"), required));
        assertEquals("deny: unknown tool", CapabilityAudit.authorize("wipe", Set.of("x"), Set.of("x"), required));
    }

    @Test
    void theGatewayDecidesInOrderAndKeepsARecordOfEveryDecision() {
        assertEquals("unauthenticated", CapabilityAudit.gateway(null, "standard", 0, CapabilityAudit.POLICY).reason());
        assertEquals("model not allowed", CapabilityAudit.gateway("key-a", "deep", 0, CapabilityAudit.POLICY).reason());
        assertEquals("rate limited", CapabilityAudit.gateway("key-a", "standard", 30, CapabilityAudit.POLICY).reason());
        assertEquals(new CapabilityAudit.Outcome("allow", "routed to claude-opus-5-5", Map.of("team", "research", "model", "deep", "decision", "allow")), CapabilityAudit.gateway("key-b", "deep", 3, CapabilityAudit.POLICY));
        assertEquals(Map.of("team", "unknown", "model", "deep", "decision", "deny"), CapabilityAudit.gateway("nope", "deep", 0, CapabilityAudit.POLICY).audit());
    }
}
