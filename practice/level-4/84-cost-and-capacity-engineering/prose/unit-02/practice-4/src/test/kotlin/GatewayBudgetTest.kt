import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GatewayBudgetTest {
    private val policy: Map<String, Any?> = mapOf(
        "allowed" to listOf("haiku", "sonnet"), "routes" to mapOf("classify" to "haiku", "draft" to "sonnet", "review" to "opus"), "default" to "sonnet",
        "cheaper" to mapOf("opus" to "sonnet", "sonnet" to "haiku"),
    )
    private val prices: Map<String, Any?> = mapOf(
        "haiku" to mapOf("input" to 100, "cache_read" to 10, "output" to 500), "sonnet" to mapOf("input" to 200, "cache_read" to 20, "output" to 1000),
    )

    private fun routed(request: Map<String, Any?>, status: String = "allow"): String? = route(request, policy, status)

    private fun admitted(spend: Long, budget: Long, estimate: Long): String {
        val result = admit(spend, budget, estimate)
        assertNotNull(result, "admit returned nothing")
        return result!!
    }

    private fun shown(rows: List<Map<String, Any?>>): List<Map<String, Any?>> {
        val result = showback(rows, prices)
        assertNotNull(result, "showback returned nothing")
        return result!!
    }

    private fun usage(team: String, model: String, input: Long, cacheRead: Long, output: Long): Map<String, Any?> =
        mapOf("team" to team, "model" to model, "input" to input, "cache_read" to cacheRead, "output" to output)

    private fun cost(team: String, cents: Long): Map<String, Any?> = mapOf("team" to team, "cents" to cents)

    @Test
    fun m1_aRequestFollowsTheRouteTableOfTheGateway() {
        assertEquals("haiku", routed(mapOf("task" to "classify")))
        assertEquals("sonnet", routed(mapOf("task" to "draft")))
        assertEquals("opus", routed(mapOf("task" to "review")))
        assertEquals("sonnet", routed(mapOf("task" to "translate")))
    }

    @Test
    fun e1_aTeamNearItsBudgetIsMovedToACheaperModelAndATeamOverItIsRefused() {
        assertEquals("sonnet", routed(mapOf("task" to "review"), "warn"))
        assertEquals("haiku", routed(mapOf("task" to "draft"), "warn"))
        assertEquals("haiku", routed(mapOf("task" to "classify"), "warn"))
        assertNull(routed(mapOf("task" to "review"), "block"))
    }

    @Test
    fun e2_aRequestIsAdmittedWarnedOrBlockedAgainstTheBudget() {
        assertEquals("allow", admitted(0, 1000, 100))
        assertEquals("allow", admitted(700, 1000, 99))
        assertEquals("warn", admitted(700, 1000, 100))
        assertEquals("warn", admitted(900, 1000, 100))
        assertEquals("block", admitted(900, 1000, 101))
        assertEquals("block", admitted(0, 0, 0))
        assertEquals("block", admitted(0, -5, 0))
    }

    @Test
    fun e3_showbackAddsEachTeamsTokensAtThePriceOfTheModelAndRefusesAnUnknownModel() {
        val rows = listOf(usage("a", "sonnet", 1_000_000, 5_000_000, 100_000), usage("b", "haiku", 2_000_000, 0, 1_000_000), usage("a", "haiku", 500_000, 0, 0))
        assertEquals(listOf(cost("b", 700), cost("a", 450)), shown(rows))
        assertEquals("a", shown(listOf(usage("b", "sonnet", 1, 0, 0), usage("a", "sonnet", 1, 0, 0)))[0]["team"])
        val error = assertThrows(IllegalArgumentException::class.java) { showback(listOf(usage("a", "other", 1, 0, 0)), prices) }
        assertTrue(error.message!!.contains("unknown model: other"))
        assertEquals(emptyList<Map<String, Any?>>(), shown(emptyList()))
    }

    @Test
    fun e4_showbackRoundsEachTeamsTotalToACentOnce() {
        assertEquals(
            listOf(cost("x", 1), cost("y", 1), cost("z", 0)),
            shown(listOf(usage("x", "sonnet", 2000, 0, 0), usage("x", "sonnet", 2000, 0, 0), usage("y", "sonnet", 2500, 0, 0), usage("z", "sonnet", 2499, 0, 0))),
        )
    }

    private fun pick(p95: Long, timeout: Long, margin: Long): String {
        val result = delivery(p95, timeout, margin)
        assertNotNull(result, "delivery returned nothing")
        return result!!
    }

    @Test
    fun e5_aCallerWithAHardLatencyLimitGetsAcceptAndPollWhenTheSlowCaseDoesNotFit() {
        assertEquals("sync", pick(8, 10, 25))
        assertEquals("accept-and-poll", pick(8, 10, 26))
        assertEquals("accept-and-poll", pick(30, 10, 0))
        assertEquals("sync", pick(10, 10, 0))
        assertEquals("accept-and-poll", pick(10, 10, 1))
    }

    @Test
    fun e6_aModelPinnedByATeamIsHonouredOnlyWhenThePolicyAllowsIt() {
        assertEquals("sonnet", routed(mapOf("task" to "classify", "model" to "sonnet")))
        assertEquals("haiku", routed(mapOf("task" to "classify", "model" to "opus")))
        assertEquals("haiku", routed(mapOf("task" to "review", "model" to "haiku")))
        assertEquals("haiku", routed(mapOf("task" to "draft", "model" to "sonnet"), "warn"))
    }
}
