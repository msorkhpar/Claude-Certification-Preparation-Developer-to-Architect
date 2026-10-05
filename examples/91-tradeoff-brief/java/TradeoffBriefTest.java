import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TradeoffBriefTest {
    @Test
    void percentRoundsHalvesUpAndSurvivesNoCases() {
        assertEquals(List.of(50, 13, 33, 67), List.of(TradeoffBrief.pct(1, 2), TradeoffBrief.pct(1, 8), TradeoffBrief.pct(1, 3), TradeoffBrief.pct(2, 3)));
        assertEquals(0, TradeoffBrief.pct(0, 0));
    }

    @Test
    void breakEvenIsWhereTheExpectedErrorCostEqualsTheCheck() {
        assertEquals(98, TradeoffBrief.breakEven(250, 5));
        assertEquals(91, TradeoffBrief.breakEven(60, 5));
        assertEquals(58, TradeoffBrief.breakEven(12, 5));
    }

    @Test
    void aServiceLevelIsMetExactlyAtItsEdge() {
        TradeoffBrief.Sla latency = new TradeoffBrief.Sla("p95 latency", 2000, "max", "ms");
        assertTrue(TradeoffBrief.slaLine(latency, 2000).endsWith("met"));
        assertEquals("p95 latency: 2001 ms against a limit of 2000 ms: missed by 1 ms", TradeoffBrief.slaLine(latency, 2001));
        TradeoffBrief.Sla floor = new TradeoffBrief.Sla("availability", 995, "min", "per mille");
        assertTrue(TradeoffBrief.slaLine(floor, 995).endsWith("met"));
        assertTrue(TradeoffBrief.slaLine(floor, 994).endsWith("missed by 1 per mille"));
    }

    @Test
    void theReportListsTheCostliestSegmentFirstAndChecksBelowTheBreakEven() {
        List<String> lines = TradeoffBrief.segmentReport(List.of(new TradeoffBrief.Segment("status", 98, 100, 12), new TradeoffBrief.Segment("credit", 63, 100, 250)), 5);
        assertTrue(lines.get(0).startsWith("credit: 63 percent right") && lines.get(0).endsWith("reviewed (break-even 98)"));
        assertEquals("status: 98 percent right, error cost 12, auto (break-even 58)", lines.get(1));
    }

    @Test
    void theTwoAudiencesGetTheSameFactsInDifferentWords() {
        TradeoffBrief.Segment weakest = new TradeoffBrief.Segment("credit", 63, 100, 250);
        String sponsor = TradeoffBrief.brief("sponsor", "Routing", 80000, 315000, weakest, "approve the pilot");
        String engineer = TradeoffBrief.brief("engineer", "Routing", 80000, 315000, weakest, "approve the pilot");
        assertTrue(sponsor.contains("a saving of 235,000") && sponsor.contains("Decision asked: approve the pilot."));
        assertTrue(engineer.contains("saving=235000") && !engineer.toLowerCase().contains("decision"));
    }
}
