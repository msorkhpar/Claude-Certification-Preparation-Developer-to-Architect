import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TradeoffBriefTest {
    @Test
    fun percentRoundsHalvesUpAndSurvivesNoCases() {
        assertEquals(listOf(50, 13, 33, 67), listOf(pct(1, 2), pct(1, 8), pct(1, 3), pct(2, 3)))
        assertEquals(0, pct(0, 0))
    }

    @Test
    fun breakEvenIsWhereTheExpectedErrorCostEqualsTheCheck() {
        assertEquals(98, breakEven(250, 5))
        assertEquals(91, breakEven(60, 5))
        assertEquals(58, breakEven(12, 5))
    }

    @Test
    fun aServiceLevelIsMetExactlyAtItsEdge() {
        val latency = Sla("p95 latency", 2000, "max", "ms")
        assertTrue(slaLine(latency, 2000).endsWith("met"))
        assertEquals("p95 latency: 2001 ms against a limit of 2000 ms: missed by 1 ms", slaLine(latency, 2001))
        val floor = Sla("availability", 995, "min", "per mille")
        assertTrue(slaLine(floor, 995).endsWith("met"))
        assertTrue(slaLine(floor, 994).endsWith("missed by 1 per mille"))
    }

    @Test
    fun theReportListsTheCostliestSegmentFirstAndChecksBelowTheBreakEven() {
        val lines = segmentReport(listOf(Segment("status", 98, 100, 12), Segment("credit", 63, 100, 250)), 5)
        assertTrue(lines[0].startsWith("credit: 63 percent right") && lines[0].endsWith("reviewed (break-even 98)"))
        assertEquals("status: 98 percent right, error cost 12, auto (break-even 58)", lines[1])
    }

    @Test
    fun theTwoAudiencesGetTheSameFactsInDifferentWords() {
        val weakest = Segment("credit", 63, 100, 250)
        val sponsor = brief("sponsor", "Routing", 80000, 315000, weakest, "approve the pilot")
        val engineer = brief("engineer", "Routing", 80000, 315000, weakest, "approve the pilot")
        assertTrue(sponsor.contains("a saving of 235,000") && sponsor.contains("Decision asked: approve the pilot."))
        assertTrue(engineer.contains("saving=235000") && !engineer.lowercase().contains("decision"))
    }
}
