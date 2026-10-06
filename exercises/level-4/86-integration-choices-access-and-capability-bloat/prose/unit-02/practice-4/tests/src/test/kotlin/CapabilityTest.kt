import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CapabilityTest {
    private val catalog = linkedMapOf(
        "read_ticket" to Tool("read", 160), "draft_reply" to Tool("draft", 220), "issue_refund" to Tool("money", 240),
        "delete_account" to Tool("destroy", 210), "export_report" to Tool("read", 300),
    )
    private val policy = Policy(
        mapOf("k1" to "support", "k2" to "research"), mapOf("support" to setOf("standard"), "research" to setOf("standard", "deep")),
        mapOf("support" to setOf("read_ticket", "draft_reply"), "research" to setOf("query")), mapOf("support" to 2, "research" to 5),
        mapOf("standard" to "claude-sonnet-5-5", "deep" to "claude-opus-5-5"),
    )

    private fun names(count: Int) = (1..count).associate { "t%02d".format(it) to 100 }

    private fun plan(tools: Map<String, Int>, usage: Map<String, Int>, keep: Int = 4, searchTokens: Int = 350): Plan {
        val result = planLoading(tools, usage, keep, searchTokens)
        assertNotNull(result, "planLoading returned nothing")
        return result!!
    }

    private fun decide(credential: String?, model: String, tool: String?, recent: Int): Outcome {
        val result = gateway(Request(credential, model, tool, recent), policy)
        assertNotNull(result, "gateway returned nothing")
        return result!!
    }

    @Test
    fun m1_anAgentLosesTheToolsItsRoleDoesNotNeedAndTheRiskyOnesAmongThemAreNamed() {
        val agent = Agent(catalog.keys.toList(), listOf("read_ticket", "draft_reply"), mapOf("read_ticket" to 12, "draft_reply" to 9))
        assertEquals(AuditResult(listOf("issue_refund", "delete_account", "export_report"), listOf("issue_refund", "delete_account"), emptyList(), emptyList()), audit(agent, catalog))
    }

    @Test
    fun e1_aToolThatIsHeldAndNeededButNeverUsedIsReportedAsDormantAndNeverRemoved() {
        val agent = Agent(listOf("read_ticket", "draft_reply"), listOf("read_ticket", "draft_reply", "issue_refund"), mapOf("read_ticket" to 5, "draft_reply" to 0))
        assertEquals(AuditResult(emptyList(), emptyList(), listOf("issue_refund"), listOf("draft_reply")), audit(agent, catalog))
        assertEquals(listOf("draft_reply"), audit(agent.copy(used = mapOf("read_ticket" to 5)), catalog)!!.dormant)
    }

    @Test
    fun e2_aSmallSetLoadsWholeAndALargeOneDefersWhenItHasTenToolsOrOverTenThousandTokens() {
        assertEquals(Plan(false, names(9).keys.toList(), emptyList(), 900), plan(names(9), emptyMap()))
        val ten = plan(names(10), emptyMap())
        assertTrue(ten.search)
        assertEquals(listOf("t01", "t02", "t03", "t04"), ten.loadNow)
        assertEquals(6, ten.deferred.size)
        assertEquals(750, ten.tokens)
        assertFalse(plan(mapOf("a" to 5000, "b" to 5000), emptyMap()).search)
        val heavy = plan(mapOf("a" to 5000, "b" to 5001), emptyMap())
        assertTrue(heavy.search)
        assertEquals(10001 + 350, heavy.tokens)
        assertEquals(emptyList<String>(), heavy.deferred)
    }

    @Test
    fun e3_theNumberOfToolsKeptLoadedStaysBetweenThreeAndFiveAndTiesAreBrokenByName() {
        assertEquals(3, plan(names(12), emptyMap(), keep = 1).loadNow.size)
        assertEquals(5, plan(names(12), emptyMap(), keep = 8).loadNow.size)
        assertEquals(listOf("t02", "t05", "t01"), plan(names(12).entries.reversed().associate { it.key to it.value }, mapOf("t05" to 9, "t02" to 9), keep = 3).loadNow)
        assertEquals(400, plan(names(12), emptyMap(), keep = 4, searchTokens = 0).tokens)
    }

    @Test
    fun e4_theMechanismFollowsTheCounterpartThenThePathThenTheNumberOfClients() {
        val rows = listOf(Triple(1, "agent", "fixed"), Triple(4, "agent", "model-chosen"), Triple(4, "tool", "fixed"), Triple(1, "tool", "fixed"), Triple(4, "tool", "model-chosen"), Triple(1, "tool", "model-chosen"))
        assertEquals(listOf("agent-to-agent", "agent-to-agent", "direct call in code", "direct call in code", "MCP server", "custom tool"), rows.map { (c, k, p) -> chooseMechanism(c, k, p) })
    }

    @Test
    fun e5_aCallNeedsTheUsersScopeAndTheAgentsScopeAndAnUnknownToolIsRefused() {
        val required = mapOf("issue_refund" to "refunds:write", "read_ticket" to "tickets:read")
        assertEquals("deny: user lacks refunds:write", authorize("issue_refund", setOf("tickets:read"), setOf("refunds:write"), required))
        assertEquals("deny: agent lacks refunds:write", authorize("issue_refund", setOf("refunds:write"), setOf("tickets:read"), required))
        assertEquals("deny: user lacks refunds:write", authorize("issue_refund", emptySet(), emptySet(), required))
        assertEquals("allow", authorize("issue_refund", setOf("refunds:write"), setOf("refunds:write"), required))
        assertEquals("deny: unknown tool", authorize("wipe_disk", setOf("x"), setOf("x"), required))
    }

    @Test
    fun e6_theGatewayChecksTheCredentialThenTheModelThenTheToolThenTheRate() {
        assertEquals("unauthenticated", decide(null, "deep", "wipe", 99).reason)
        assertEquals("unauthenticated", decide("zzz", "deep", "wipe", 99).reason)
        assertEquals("model not allowed", decide("k1", "deep", "wipe", 99).reason)
        assertEquals("tool not allowed", decide("k1", "standard", "issue_refund", 99).reason)
        assertEquals("rate limited", decide("k1", "standard", "read_ticket", 2).reason)
        assertEquals("routed to claude-sonnet-5-5", decide("k1", "standard", "read_ticket", 1).reason)
        assertEquals("allow", decide("k2", "deep", null, 0).decision)
        assertEquals("routed to claude-opus-5-5", decide("k2", "deep", null, 0).reason)
        assertEquals("deny", decide("k1", "standard", "read_ticket", 2).decision)
    }

    @Test
    fun e7_theGatewayKeepsARecordOfEveryDecisionWithTheTeamOrUnknownAndNoContent() {
        assertEquals(mapOf("team" to "unknown", "model" to "deep", "tool" to "none", "decision" to "deny"), decide(null, "deep", null, 0).audit)
        assertEquals(mapOf("team" to "support", "model" to "standard", "tool" to "issue_refund", "decision" to "deny"), decide("k1", "standard", "issue_refund", 0).audit)
        assertEquals(mapOf("team" to "research", "model" to "deep", "tool" to "query", "decision" to "allow"), decide("k2", "deep", "query", 0).audit)
    }
}
