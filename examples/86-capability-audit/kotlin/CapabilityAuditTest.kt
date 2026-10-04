import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CapabilityAuditTest {
    @Test
    fun aRoleLosesTheToolsItDoesNotNeedAndTheRiskyOnesAreNamed() {
        assertEquals(Audit(listOf("issue_refund", "delete_account"), listOf("issue_refund", "delete_account"), emptyList()), audit(CATALOG.keys.toList(), listOf("read_ticket", "draft_reply"), CATALOG))
        assertEquals(Audit(emptyList(), emptyList(), listOf("draft_reply")), audit(listOf("read_ticket"), listOf("read_ticket", "draft_reply"), CATALOG))
    }

    @Test
    fun aLargeToolSetDefersAllButTheMostUsedAndASmallOneLoadsWhole() {
        val plan = planLoading(TOOLS, USAGE)
        assertTrue(plan.search)
        assertEquals(listOf("github_create_issue", "github_search_code", "slack_post_message", "github_get_pr"), plan.loadNow)
        assertEquals(20, plan.deferred.size)
        assertEquals(2320, plan.tokens)
        val small = TOOLS.keys.take(9).associateWith { 100 }
        assertEquals(Plan(false, small.keys.toList(), emptyList(), 900), planLoading(small, USAGE))
        assertTrue(planLoading(mapOf("a" to 6000, "b" to 5000), emptyMap()).search)
        assertEquals(5, planLoading(TOOLS, USAGE, keep = 9).loadNow.size)
        assertEquals(3, planLoading(TOOLS, USAGE, keep = 1).loadNow.size)
    }

    @Test
    fun theMechanismFollowsTheCounterpartThenThePathThenTheNumberOfClients() {
        assertEquals(listOf("agent-to-agent", "direct call in code", "MCP server", "custom tool"),
            listOf(Triple(1, "agent", "fixed"), Triple(4, "tool", "fixed"), Triple(4, "tool", "model-chosen"), Triple(1, "tool", "model-chosen")).map { (c, k, p) -> chooseMechanism(c, k, p) })
    }

    @Test
    fun theAgentRightsAreACeilingAndTheUserRightsDecide() {
        val required = mapOf("issue_refund" to "refunds:write")
        assertEquals("deny: user lacks refunds:write", authorize("issue_refund", setOf("tickets:read"), setOf("refunds:write"), required))
        assertEquals("deny: agent lacks refunds:write", authorize("issue_refund", setOf("refunds:write"), setOf("tickets:read"), required))
        assertEquals("allow", authorize("issue_refund", setOf("refunds:write"), setOf("refunds:write"), required))
        assertEquals("deny: unknown tool", authorize("wipe", setOf("x"), setOf("x"), required))
    }

    @Test
    fun theGatewayDecidesInOrderAndKeepsARecordOfEveryDecision() {
        assertEquals("unauthenticated", gateway(null, "standard", 0, POLICY).reason)
        assertEquals("model not allowed", gateway("key-a", "deep", 0, POLICY).reason)
        assertEquals("rate limited", gateway("key-a", "standard", 30, POLICY).reason)
        assertEquals(Outcome("allow", "routed to claude-opus-5-5", mapOf("team" to "research", "model" to "deep", "decision" to "allow")), gateway("key-b", "deep", 3, POLICY))
        assertEquals(mapOf("team" to "unknown", "model" to "deep", "decision" to "deny"), gateway("nope", "deep", 0, POLICY).audit)
    }
}
