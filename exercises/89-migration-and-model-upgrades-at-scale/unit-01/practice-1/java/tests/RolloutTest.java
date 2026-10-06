import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RolloutTest {
    private static Rollout.Case mk(String id, String segment, boolean mustPass, boolean oldOk, boolean newOk, int oldCost, int newCost, int newMs) {
        return new Rollout.Case(id, segment, mustPass, oldOk, newOk, oldCost, newCost, newMs);
    }

    /** Ten cases: billing a1, a2 (must pass), refund b1 (must pass) b2 b3, faq c1 to c5. By default b3 is lost and c4 is gained; the ids given are set to fail or to pass in the new model. */
    private static List<Rollout.Case> base(String... flips) {
        List<Rollout.Case> rows = new ArrayList<>(List.of(mk("a1", "billing", true, true, true, 4, 5, 900), mk("a2", "billing", true, true, true, 4, 5, 1000), mk("b1", "refund", true, true, true, 6, 7, 1500),
            mk("b2", "refund", false, true, true, 6, 7, 1600), mk("b3", "refund", false, true, false, 6, 7, 1700)));
        for (int i = 1; i <= 5; i++) rows.add(mk("c" + i, "faq", false, i != 4 && i != 5, i != 5, 2, 2, 550 + 50 * i));
        List<Rollout.Case> out = new ArrayList<>();
        for (Rollout.Case c : rows) {
            boolean flip = List.of(flips).contains(c.id());
            out.add(flip ? mk(c.id(), c.segment(), c.mustPass(), c.oldOk(), !c.newOk(), c.oldCost(), c.newCost(), c.newMs()) : c);
        }
        return out;
    }

    private static List<Rollout.Case> fixed() {
        return base("b3");
    }

    private static Rollout.Verdict verdict(List<Rollout.Case> cases, Set<String> protectedSegments, int maxCostUp, int maxP95) {
        Rollout.Verdict result = Rollout.gate(cases, protectedSegments, maxCostUp, maxP95);
        assertNotNull(result, "gate returned nothing");
        return result;
    }

    private static Rollout.Verdict verdict(List<Rollout.Case> cases, Set<String> protectedSegments) {
        return verdict(cases, protectedSegments, 20, 2000);
    }

    private static List<String> lines(List<String> result) {
        assertNotNull(result, "a list was expected");
        return result;
    }

    @Test
    void m1_aChangeThatRegressesNothingAndStaysInsideItsLimitsGetsAGoWithNoReasons() {
        assertEquals(new Rollout.Verdict("go", List.of()), verdict(fixed(), Set.of("refund")));
    }

    @Test
    void e1_aMustPassCaseThatFailsBlocksTheChangeAndTheIdsAreListedInOrder() {
        assertEquals(List.of("must-pass failed: a1"), verdict(base("b3", "a1"), Set.of()).reasons());
        assertEquals("must-pass failed: a2, b1", verdict(base("b3", "a2", "b1"), Set.of()).reasons().get(0));
    }

    @Test
    void e2_aProtectedSegmentThatLostAnswersBlocksTheChangeEvenWhenGainsElsewhereMatchTheLosses() {
        assertEquals(List.of("protected segment lost answers: refund"), verdict(base(), Set.of("refund")).reasons());
        assertEquals(new Rollout.Verdict("go", List.of()), verdict(base(), Set.of()));
    }

    @Test
    void e3_moreLossesThanGainsBlocksTheChangeAndBothCountsAreNamed() {
        assertEquals(new Rollout.Verdict("no-go", List.of("net loss: lost 2, gained 1")), verdict(base("b3", "c1", "c2"), Set.of()));
    }

    @Test
    void e4_aCostRiseOverTheLimitBlocksTheChangeAndARiseExactlyAtTheLimitDoesNot() {
        assertEquals("go", verdict(fixed(), Set.of(), 13, 2000).decision());
        assertEquals(List.of("cost up 13% over the 12% limit"), verdict(fixed(), Set.of(), 12, 2000).reasons());
        List<Rollout.Case> cheaper = new ArrayList<>();
        for (Rollout.Case c : fixed()) cheaper.add(mk(c.id(), c.segment(), c.mustPass(), c.oldOk(), c.newOk(), c.oldCost(), 1, c.newMs()));
        assertEquals("go", verdict(cheaper, Set.of(), 0, 2000).decision());
    }

    @Test
    void e5_theTailIsTheNearestRank95thPercentileAndASingleSlowCaseDoesNotBlock() {
        List<Rollout.Case> rows = new ArrayList<>();
        for (int i = 1; i <= 40; i++) rows.add(mk("c" + i, "faq", false, true, true, 1, 1, i == 40 ? 9000 : 1000));
        assertEquals(new Rollout.Verdict("go", List.of()), verdict(rows, Set.of(), 20, 2000));
        List<Rollout.Case> slow = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Rollout.Case c = rows.get(i);
            slow.add(i < 3 ? mk(c.id(), c.segment(), c.mustPass(), c.oldOk(), c.newOk(), c.oldCost(), c.newCost(), 3000) : c);
        }
        assertEquals(List.of("p95 latency 3000 ms over the 2000 ms limit"), verdict(slow, Set.of(), 20, 2000).reasons());
    }

    @Test
    void e6_aRollOutAdvancesWhenHealthyHoldsWithTooFewRequestsAndRollsBackToZeroWhenErrorsPassTheLimit() {
        assertEquals("advance to 5", Rollout.rolloutStep(1, 2000, 6, 1000, 5));
        assertEquals("hold at 5", Rollout.rolloutStep(5, 300, 0, 1000, 5));
        assertEquals("hold at 5", Rollout.rolloutStep(5, 300, 300, 1000, 5));
        assertEquals("rollback to 0", Rollout.rolloutStep(25, 50000, 400, 1000, 5));
        assertEquals("advance to 100", Rollout.rolloutStep(25, 1000, 5, 1000, 5));
        assertEquals("complete", Rollout.rolloutStep(100, 50000, 10, 1000, 5));
    }

    @Test
    void e7_theRetirementCalendarCountsDaysRanksTheNearestFirstAndNamesTheLevel() {
        List<Rollout.Model> models = List.of(new Rollout.Model("b", "2026-11-30", false), new Rollout.Model("a", "2026-10-18", true), new Rollout.Model("c", "2026-08-05", false),
            new Rollout.Model("d", "2027-01-01", false), new Rollout.Model("e", "2026-10-19", false));
        assertEquals(List.of("c: -60 days, retired", "a: 14 days, urgent (tentative)", "e: 15 days, migrate now", "b: 57 days, migrate now", "d: 89 days, watch"), lines(Rollout.retirementStatus(models, "2026-10-04")));
        assertEquals(List.of("z: 60 days, migrate now"), lines(Rollout.retirementStatus(List.of(new Rollout.Model("z", "2026-12-03", false)), "2026-10-04")));
        assertEquals(List.of("z: 61 days, watch"), lines(Rollout.retirementStatus(List.of(new Rollout.Model("z", "2026-12-04", false)), "2026-10-04")));
    }

    @Test
    void e8_migrationRemovesTheSettingsTheNewModelRefusesAndNamesEachChange() {
        Rollout.Request old = new Rollout.Request("claude-sonnet-4-5-20250929", 0.7, 0.9, 40.0, "disabled", "any", false, true);
        Rollout.Migration result = Rollout.migrateRequest(old);
        assertNotNull(result, "migrateRequest returned nothing");
        assertEquals(new Rollout.Request(Rollout.TARGET, null, null, null, "between_tools", "auto", true, false), result.request());
        assertEquals(List.of("model set to claude-sonnet-5-5", "removed temperature", "removed top_p", "removed top_k", "thinking disabled replaced by between_tools",
            "forced tool choice replaced by auto with strict tools", "assistant prefill removed; state the format in the instructions"), result.changes());
        Rollout.Request clean = new Rollout.Request(Rollout.TARGET, null, null, null, "adaptive", "auto", false, false);
        Rollout.Migration unchanged = Rollout.migrateRequest(clean);
        assertNotNull(unchanged, "migrateRequest returned nothing");
        assertEquals(new Rollout.Migration(clean, List.of()), unchanged);
        Rollout.Migration budget = Rollout.migrateRequest(new Rollout.Request(Rollout.TARGET, null, null, null, "budget", "tool", false, false));
        assertNotNull(budget, "migrateRequest returned nothing");
        assertEquals("adaptive", budget.request().thinking());
    }
}
