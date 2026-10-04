import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RolloutGateTest {
    private static List<RolloutGate.Case> broken(String... ids) {
        List<RolloutGate.Case> out = new ArrayList<>();
        for (RolloutGate.Case c : RolloutGate.suite()) {
            boolean hit = List.of(ids).contains(c.id());
            out.add(hit ? new RolloutGate.Case(c.id(), c.segment(), c.mustPass(), c.oldOk(), false, c.oldCost(), c.newCost(), c.newMs()) : c);
        }
        return out;
    }

    @Test
    void retirementStatusCountsDaysAndRanksByUrgency() {
        List<RolloutGate.Model> models = List.of(new RolloutGate.Model("b", "2026-11-30", false), new RolloutGate.Model("a", "2026-10-15", true), new RolloutGate.Model("c", "2026-08-05", false));
        assertEquals(List.of("c: -60 days, retired", "a: 11 days, urgent (tentative)", "b: 57 days, migrate now"), RolloutGate.retirementStatus(models, "2026-10-04"));
        assertEquals(List.of("x: 89 days, watch"), RolloutGate.retirementStatus(List.of(new RolloutGate.Model("x", "2027-01-01", false)), "2026-10-04"));
    }

    @Test
    void migrationRemovesWhatTheNewModelRefusesAndKeepsTheRest() {
        RolloutGate.Request old = new RolloutGate.Request("claude-sonnet-4-5-20250929", 0.7, 0.9, null, "budget", "tool", false, true);
        RolloutGate.Migration m = RolloutGate.migrateRequest(old);
        assertEquals(new RolloutGate.Request(RolloutGate.TARGET, null, null, null, "adaptive", "auto", true, false), m.request());
        assertEquals(6, m.changes().size());
        assertEquals("model set to claude-sonnet-5-5", m.changes().get(0));
        RolloutGate.Request clean = new RolloutGate.Request(RolloutGate.TARGET, null, null, null, "adaptive", "auto", true, false);
        assertEquals(new RolloutGate.Migration(clean, List.of()), RolloutGate.migrateRequest(clean));
    }

    @Test
    void theGateIsGoOnlyWhenNoCheckFails() {
        assertEquals(new RolloutGate.Verdict("go", List.of()), RolloutGate.gate(RolloutGate.suite(), Set.of(), 40, 2000));
        assertEquals(List.of("protected segment lost answers: refund", "cost up 35% over the 25% limit"), RolloutGate.gate(RolloutGate.suite(), Set.of("refund"), 25, 2000).reasons());
    }

    @Test
    void aCostRiseExactlyAtTheLimitPasses() {
        assertEquals("go", RolloutGate.gate(RolloutGate.suite(), Set.of(), 35, 2000).decision());
        assertEquals(List.of("cost up 35% over the 34% limit"), RolloutGate.gate(RolloutGate.suite(), Set.of(), 34, 2000).reasons());
    }

    @Test
    void theTailIsTheNearestRank95thPercentile() {
        assertEquals("go", RolloutGate.gate(RolloutGate.suite(), Set.of(), 40, 1800).decision());
        assertEquals(List.of("p95 latency 1800 ms over the 1799 ms limit"), RolloutGate.gate(RolloutGate.suite(), Set.of(), 40, 1799).reasons());
    }

    @Test
    void aMustPassFailureAndANetLossAreNamed() {
        List<String> reasons = RolloutGate.gate(broken("b1", "r1"), Set.of(), 40, 2000).reasons();
        assertEquals("must-pass failed: b1, r1", reasons.get(0));
        assertEquals("net loss: lost 3, gained 2", reasons.get(1));
    }

    @Test
    void aRolloutAdvancesHoldsOrRollsBack() {
        assertEquals("advance to 5", RolloutGate.rolloutStep(1, 2000, 6, 1000, 5));
        assertEquals("hold at 5", RolloutGate.rolloutStep(5, 300, 0, 1000, 5));
        assertEquals("rollback to 0", RolloutGate.rolloutStep(25, 50000, 400, 1000, 5));
        assertEquals("complete", RolloutGate.rolloutStep(100, 50000, 10, 1000, 5));
        assertEquals("advance to 100", RolloutGate.rolloutStep(25, 1000, 5, 1000, 5));
    }
}
