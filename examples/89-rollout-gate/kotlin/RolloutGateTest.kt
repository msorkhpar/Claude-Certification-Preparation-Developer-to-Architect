import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RolloutGateTest {
    private fun broken(vararg ids: String): List<Case> = suite().map { if (it.id in ids) it.copy(newOk = false) else it }

    @Test
    fun retirementStatusCountsDaysAndRanksByUrgency() {
        val models = listOf(Model("b", "2026-11-30", false), Model("a", "2026-10-15", true), Model("c", "2026-08-05", false))
        assertEquals(listOf("c: -60 days, retired", "a: 11 days, urgent (tentative)", "b: 57 days, migrate now"), retirementStatus(models, "2026-10-04"))
        assertEquals(listOf("x: 89 days, watch"), retirementStatus(listOf(Model("x", "2027-01-01", false)), "2026-10-04"))
    }

    @Test
    fun migrationRemovesWhatTheNewModelRefusesAndKeepsTheRest() {
        val old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, null, "budget", "tool", false, true)
        val (next, changes) = migrateRequest(old)
        assertEquals(Request(TARGET, null, null, null, "adaptive", "auto", true, false), next)
        assertEquals(6, changes.size)
        assertEquals("model set to claude-sonnet-5-5", changes[0])
        val clean = Request(TARGET, null, null, null, "adaptive", "auto", true, false)
        assertEquals(Pair(clean, listOf<String>()), migrateRequest(clean))
    }

    @Test
    fun theGateIsGoOnlyWhenNoCheckFails() {
        assertEquals(Verdict("go", listOf()), gate(suite(), setOf(), 40, 2000))
        assertEquals(listOf("protected segment lost answers: refund", "cost up 35% over the 25% limit"), gate(suite(), setOf("refund"), 25, 2000).reasons)
    }

    @Test
    fun aCostRiseExactlyAtTheLimitPasses() {
        assertEquals("go", gate(suite(), setOf(), 35, 2000).decision)
        assertEquals(listOf("cost up 35% over the 34% limit"), gate(suite(), setOf(), 34, 2000).reasons)
    }

    @Test
    fun theTailIsTheNearestRank95thPercentile() {
        assertEquals("go", gate(suite(), setOf(), 40, 1800).decision)
        assertEquals(listOf("p95 latency 1800 ms over the 1799 ms limit"), gate(suite(), setOf(), 40, 1799).reasons)
    }

    @Test
    fun aMustPassFailureAndANetLossAreNamed() {
        val reasons = gate(broken("b1", "r1"), setOf(), 40, 2000).reasons
        assertEquals("must-pass failed: b1, r1", reasons[0])
        assertEquals("net loss: lost 3, gained 2", reasons[1])
    }

    @Test
    fun aRolloutAdvancesHoldsOrRollsBack() {
        assertEquals("advance to 5", rolloutStep(1, 2000, 6, 1000, 5))
        assertEquals("hold at 5", rolloutStep(5, 300, 0, 1000, 5))
        assertEquals("rollback to 0", rolloutStep(25, 50000, 400, 1000, 5))
        assertEquals("complete", rolloutStep(100, 50000, 10, 1000, 5))
        assertEquals("advance to 100", rolloutStep(25, 1000, 5, 1000, 5))
    }
}
