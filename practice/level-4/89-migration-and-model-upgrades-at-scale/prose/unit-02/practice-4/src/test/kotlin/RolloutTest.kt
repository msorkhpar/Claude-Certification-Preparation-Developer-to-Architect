import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RolloutTest {
    private fun mk(id: String, segment: String, mustPass: Boolean, oldOk: Boolean, newOk: Boolean, oldCost: Int, newCost: Int, newMs: Int) = Case(id, segment, mustPass, oldOk, newOk, oldCost, newCost, newMs)

    /** Ten cases: billing a1, a2 (must pass), refund b1 (must pass) b2 b3, faq c1 to c5. By default b3 is lost and c4 is gained; the ids given are set to fail or to pass in the new model. */
    private fun base(vararg flips: String): List<Case> {
        val rows = mutableListOf(mk("a1", "billing", true, true, true, 4, 5, 900), mk("a2", "billing", true, true, true, 4, 5, 1000), mk("b1", "refund", true, true, true, 6, 7, 1500),
            mk("b2", "refund", false, true, true, 6, 7, 1600), mk("b3", "refund", false, true, false, 6, 7, 1700))
        for (i in 1..5) rows.add(mk("c$i", "faq", false, i != 4 && i != 5, i != 5, 2, 2, 550 + 50 * i))
        return rows.map { if (it.id in flips) it.copy(newOk = !it.newOk) else it }
    }

    private fun fixed() = base("b3")

    private fun verdict(cases: List<Case>, protectedSegments: Set<String>, maxCostUp: Int = 20, maxP95: Int = 2000): Verdict {
        val result = gate(cases, protectedSegments, maxCostUp, maxP95)
        assertNotNull(result, "gate returned nothing")
        return result ?: Verdict("", listOf())
    }

    private fun lines(result: List<String>?): List<String> {
        assertNotNull(result, "a list was expected")
        return result ?: listOf()
    }

    @Test
    fun m1_aChangeThatRegressesNothingAndStaysInsideItsLimitsGetsAGoWithNoReasons() {
        assertEquals(Verdict("go", listOf()), verdict(fixed(), setOf("refund")))
    }

    @Test
    fun e1_aMustPassCaseThatFailsBlocksTheChangeAndTheIdsAreListedInOrder() {
        assertEquals(listOf("must-pass failed: a1"), verdict(base("b3", "a1"), setOf()).reasons)
        assertEquals("must-pass failed: a2, b1", verdict(base("b3", "a2", "b1"), setOf()).reasons[0])
    }

    @Test
    fun e2_aProtectedSegmentThatLostAnswersBlocksTheChangeEvenWhenGainsElsewhereMatchTheLosses() {
        assertEquals(listOf("protected segment lost answers: refund"), verdict(base(), setOf("refund")).reasons)
        assertEquals(Verdict("go", listOf()), verdict(base(), setOf()))
    }

    @Test
    fun e3_moreLossesThanGainsBlocksTheChangeAndBothCountsAreNamed() {
        assertEquals(Verdict("no-go", listOf("net loss: lost 2, gained 1")), verdict(base("b3", "c1", "c2"), setOf()))
    }

    @Test
    fun e4_aCostRiseOverTheLimitBlocksTheChangeAndARiseExactlyAtTheLimitDoesNot() {
        assertEquals("go", verdict(fixed(), setOf(), 13).decision)
        assertEquals(listOf("cost up 13% over the 12% limit"), verdict(fixed(), setOf(), 12).reasons)
        val cheaper = fixed().map { it.copy(newCost = 1) }
        assertEquals("go", verdict(cheaper, setOf(), 0).decision)
    }

    @Test
    fun e5_theTailIsTheNearestRank95thPercentileAndASingleSlowCaseDoesNotBlock() {
        val rows = (1..40).map { mk("c$it", "faq", false, true, true, 1, 1, if (it == 40) 9000 else 1000) }
        assertEquals(Verdict("go", listOf()), verdict(rows, setOf(), 20, 2000))
        val slow = rows.mapIndexed { i, c -> if (i < 3) c.copy(newMs = 3000) else c }
        assertEquals(listOf("p95 latency 3000 ms over the 2000 ms limit"), verdict(slow, setOf(), 20, 2000).reasons)
    }

    @Test
    fun e6_aRollOutAdvancesWhenHealthyHoldsWithTooFewRequestsAndRollsBackToZeroWhenErrorsPassTheLimit() {
        assertEquals("advance to 5", rolloutStep(1, 2000, 6, 1000, 5))
        assertEquals("hold at 5", rolloutStep(5, 300, 0, 1000, 5))
        assertEquals("hold at 5", rolloutStep(5, 300, 300, 1000, 5))
        assertEquals("rollback to 0", rolloutStep(25, 50000, 400, 1000, 5))
        assertEquals("advance to 100", rolloutStep(25, 1000, 5, 1000, 5))
        assertEquals("complete", rolloutStep(100, 50000, 10, 1000, 5))
    }

    @Test
    fun e7_theRetirementCalendarCountsDaysRanksTheNearestFirstAndNamesTheLevel() {
        val models = listOf(Model("b", "2026-11-30", false), Model("a", "2026-10-18", true), Model("c", "2026-08-05", false), Model("d", "2027-01-01", false), Model("e", "2026-10-19", false))
        assertEquals(listOf("c: -60 days, retired", "a: 14 days, urgent (tentative)", "e: 15 days, migrate now", "b: 57 days, migrate now", "d: 89 days, watch"), lines(retirementStatus(models, "2026-10-04")))
        assertEquals(listOf("z: 60 days, migrate now"), lines(retirementStatus(listOf(Model("z", "2026-12-03", false)), "2026-10-04")))
        assertEquals(listOf("z: 61 days, watch"), lines(retirementStatus(listOf(Model("z", "2026-12-04", false)), "2026-10-04")))
    }

    @Test
    fun e8_migrationRemovesTheSettingsTheNewModelRefusesAndNamesEachChange() {
        val old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, 40.0, "disabled", "any", false, true)
        val result = migrateRequest(old)
        assertNotNull(result, "migrateRequest returned nothing")
        assertEquals(Request(TARGET, null, null, null, "between_tools", "auto", true, false), result?.request)
        assertEquals(listOf("model set to claude-sonnet-5-5", "removed temperature", "removed top_p", "removed top_k", "thinking disabled replaced by between_tools",
            "forced tool choice replaced by auto with strict tools", "assistant prefill removed; state the format in the instructions"), result?.changes)
        val clean = Request(TARGET, null, null, null, "adaptive", "auto", false, false)
        val unchanged = migrateRequest(clean)
        assertNotNull(unchanged, "migrateRequest returned nothing")
        assertEquals(Migration(clean, listOf()), unchanged)
        val budget = migrateRequest(Request(TARGET, null, null, null, "budget", "tool", false, false))
        assertNotNull(budget, "migrateRequest returned nothing")
        assertEquals("adaptive", budget?.request?.thinking)
    }
}
