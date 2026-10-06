import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CapabilityTest {
    private static final Map<String, Capability.Tool> CATALOG = new LinkedHashMap<>();
    private static final Capability.Policy POLICY = new Capability.Policy(Map.of("k1", "support", "k2", "research"), Map.of("support", Set.of("standard"), "research", Set.of("standard", "deep")),
        Map.of("support", Set.of("read_ticket", "draft_reply"), "research", Set.of("query")), Map.of("support", 2, "research", 5), Map.of("standard", "claude-sonnet-5-5", "deep", "claude-opus-5-5"));

    static {
        CATALOG.put("read_ticket", new Capability.Tool("read", 160));
        CATALOG.put("draft_reply", new Capability.Tool("draft", 220));
        CATALOG.put("issue_refund", new Capability.Tool("money", 240));
        CATALOG.put("delete_account", new Capability.Tool("destroy", 210));
        CATALOG.put("export_report", new Capability.Tool("read", 300));
    }

    private static Map<String, Integer> names(int count) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 1; i <= count; i++) m.put(String.format("t%02d", i), 100);
        return m;
    }

    private static Capability.Plan plan(Map<String, Integer> tools, Map<String, Integer> usage, int keep, int searchTokens) {
        var result = Capability.planLoading(tools, usage, keep, searchTokens);
        assertNotNull(result, "planLoading returned nothing");
        return result;
    }

    private static Capability.Outcome decide(String credential, String model, String tool, int recent) {
        var result = Capability.gateway(new Capability.Request(credential, model, tool, recent), POLICY);
        assertNotNull(result, "gateway returned nothing");
        return result;
    }

    @Test
    void m1_anAgentLosesTheToolsItsRoleDoesNotNeedAndTheRiskyOnesAmongThemAreNamed() {
        var agent = new Capability.Agent(List.copyOf(CATALOG.keySet()), List.of("read_ticket", "draft_reply"), Map.of("read_ticket", 12, "draft_reply", 9));
        assertEquals(new Capability.AuditResult(List.of("issue_refund", "delete_account", "export_report"), List.of("issue_refund", "delete_account"), List.of(), List.of()), Capability.audit(agent, CATALOG));
    }

    @Test
    void e1_aToolThatIsHeldAndNeededButNeverUsedIsReportedAsDormantAndNeverRemoved() {
        var agent = new Capability.Agent(List.of("read_ticket", "draft_reply"), List.of("read_ticket", "draft_reply", "issue_refund"), Map.of("read_ticket", 5, "draft_reply", 0));
        assertEquals(new Capability.AuditResult(List.of(), List.of(), List.of("issue_refund"), List.of("draft_reply")), Capability.audit(agent, CATALOG));
        var unrecorded = new Capability.Agent(agent.holds(), agent.needs(), Map.of("read_ticket", 5));
        assertEquals(List.of("draft_reply"), Capability.audit(unrecorded, CATALOG).dormant());
    }

    @Test
    void e2_aSmallSetLoadsWholeAndALargeOneDefersWhenItHasTenToolsOrOverTenThousandTokens() {
        assertEquals(new Capability.Plan(false, List.copyOf(names(9).keySet()), List.of(), 900), plan(names(9), Map.of(), 4, 350));
        var ten = plan(names(10), Map.of(), 4, 350);
        assertTrue(ten.search());
        assertEquals(List.of("t01", "t02", "t03", "t04"), ten.loadNow());
        assertEquals(6, ten.deferred().size());
        assertEquals(750, ten.tokens());
        assertFalse(plan(Map.of("a", 5000, "b", 5000), Map.of(), 4, 350).search());
        var heavy = plan(new LinkedHashMap<>(Map.of("a", 5000, "b", 5001)), Map.of(), 4, 350);
        assertTrue(heavy.search());
        assertEquals(10001 + 350, heavy.tokens());
        assertEquals(List.of(), heavy.deferred());
    }

    @Test
    void e3_theNumberOfToolsKeptLoadedStaysBetweenThreeAndFiveAndTiesAreBrokenByName() {
        assertEquals(3, plan(names(12), Map.of(), 1, 350).loadNow().size());
        assertEquals(5, plan(names(12), Map.of(), 8, 350).loadNow().size());
        Map<String, Integer> reversed = new LinkedHashMap<>();
        List<String> keys = new java.util.ArrayList<>(names(12).keySet());
        java.util.Collections.reverse(keys);
        keys.forEach(k -> reversed.put(k, 100));
        assertEquals(List.of("t02", "t05", "t01"), plan(reversed, Map.of("t05", 9, "t02", 9), 3, 350).loadNow());
        assertEquals(400, plan(names(12), Map.of(), 4, 0).tokens());
    }

    @Test
    void e4_theMechanismFollowsTheCounterpartThenThePathThenTheNumberOfClients() {
        assertEquals("agent-to-agent", Capability.chooseMechanism(1, "agent", "fixed"));
        assertEquals("agent-to-agent", Capability.chooseMechanism(4, "agent", "model-chosen"));
        assertEquals("direct call in code", Capability.chooseMechanism(4, "tool", "fixed"));
        assertEquals("direct call in code", Capability.chooseMechanism(1, "tool", "fixed"));
        assertEquals("MCP server", Capability.chooseMechanism(4, "tool", "model-chosen"));
        assertEquals("custom tool", Capability.chooseMechanism(1, "tool", "model-chosen"));
    }

    @Test
    void e5_aCallNeedsTheUsersScopeAndTheAgentsScopeAndAnUnknownToolIsRefused() {
        Map<String, String> required = Map.of("issue_refund", "refunds:write", "read_ticket", "tickets:read");
        assertEquals("deny: user lacks refunds:write", Capability.authorize("issue_refund", Set.of("tickets:read"), Set.of("refunds:write"), required));
        assertEquals("deny: agent lacks refunds:write", Capability.authorize("issue_refund", Set.of("refunds:write"), Set.of("tickets:read"), required));
        assertEquals("deny: user lacks refunds:write", Capability.authorize("issue_refund", Set.of(), Set.of(), required));
        assertEquals("allow", Capability.authorize("issue_refund", Set.of("refunds:write"), Set.of("refunds:write"), required));
        assertEquals("deny: unknown tool", Capability.authorize("wipe_disk", Set.of("x"), Set.of("x"), required));
    }

    @Test
    void e6_theGatewayChecksTheCredentialThenTheModelThenTheToolThenTheRate() {
        assertEquals("unauthenticated", decide(null, "deep", "wipe", 99).reason());
        assertEquals("unauthenticated", decide("zzz", "deep", "wipe", 99).reason());
        assertEquals("model not allowed", decide("k1", "deep", "wipe", 99).reason());
        assertEquals("tool not allowed", decide("k1", "standard", "issue_refund", 99).reason());
        assertEquals("rate limited", decide("k1", "standard", "read_ticket", 2).reason());
        assertEquals("routed to claude-sonnet-5-5", decide("k1", "standard", "read_ticket", 1).reason());
        assertEquals("allow", decide("k2", "deep", null, 0).decision());
        assertEquals("routed to claude-opus-5-5", decide("k2", "deep", null, 0).reason());
        assertEquals("deny", decide("k1", "standard", "read_ticket", 2).decision());
    }

    @Test
    void e7_theGatewayKeepsARecordOfEveryDecisionWithTheTeamOrUnknownAndNoContent() {
        assertEquals(Map.of("team", "unknown", "model", "deep", "tool", "none", "decision", "deny"), decide(null, "deep", null, 0).audit());
        assertEquals(Map.of("team", "support", "model", "standard", "tool", "issue_refund", "decision", "deny"), decide("k1", "standard", "issue_refund", 0).audit());
        assertEquals(Map.of("team", "research", "model", "deep", "tool", "query", "decision", "allow"), decide("k2", "deep", "query", 0).audit());
    }
}
