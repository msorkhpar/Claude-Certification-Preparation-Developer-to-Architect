import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EvalKitTest {
    private static final Map<String, Integer> COSTS = Map.of("order status", 1, "refund", 20, "policy", 5, "complaint", 10);

    private static void add(List<EvalKit.Result> out, String segment, int total, int right) {
        for (int i = 0; i < total; i++) out.add(new EvalKit.Result(segment, i < right));
    }

    private static void add(List<EvalKit.Paired> out, int n, String segment, boolean oldOk, boolean newOk) {
        for (int i = 0; i < n; i++) out.add(new EvalKit.Paired(segment, oldOk, newOk));
    }

    private static List<EvalKit.Line> table(List<EvalKit.Result> results, Map<String, Integer> costs) {
        List<EvalKit.Line> result = EvalKit.segmentTable(results, costs);
        assertNotNull(result, "segmentTable returned nothing");
        return result;
    }

    private static EvalKit.Gate gate(List<EvalKit.Paired> pairs, Set<String> protectedSegments) {
        EvalKit.Gate result = EvalKit.shadowGate(pairs, protectedSegments);
        assertNotNull(result, "shadowGate returned nothing");
        return result;
    }

    @Test
    void m1_aSegmentTableReportsAccuracyAndErrorCostWithTheCostliestSegmentFirst() {
        List<EvalKit.Result> results = new ArrayList<>();
        add(results, "order status", 30, 30);
        add(results, "refund", 8, 5);
        add(results, "policy", 10, 9);
        add(results, "complaint", 4, 4);
        assertEquals(List.of(new EvalKit.Line("refund", 8, 5, 63, 60), new EvalKit.Line("policy", 10, 9, 90, 5), new EvalKit.Line("complaint", 4, 4, 100, 0), new EvalKit.Line("order status", 30, 30, 100, 0)), table(results, COSTS));
    }

    @Test
    void e1_aSegmentWithNoCostEntryCostsOnePerErrorAndNoResultsGiveAnEmptyTable() {
        List<EvalKit.Result> odd = new ArrayList<>();
        add(odd, "odd", 3, 1);
        assertEquals(List.of(new EvalKit.Line("odd", 3, 1, 33, 2)), table(odd, COSTS));
        assertEquals(List.of(), table(List.of(), COSTS));
        List<EvalKit.Result> ties = new ArrayList<>();
        add(ties, "b", 2, 1);
        add(ties, "a", 2, 1);
        assertEquals(List.of(new EvalKit.Line("a", 2, 1, 50, 1), new EvalKit.Line("b", 2, 1, 50, 1)), table(ties, Map.of("a", 1, "b", 1)));
    }

    @Test
    void e2_aPercentileUsesTheNearestRankAndDoesNotNeedSortedInput() {
        List<Integer> values = List.of(4800, 800, 1000, 900);
        assertEquals(900, EvalKit.percentile(values, 50));
        assertEquals(4800, EvalKit.percentile(values, 95));
        assertEquals(4800, EvalKit.percentile(values, 100));
        assertEquals(800, EvalKit.percentile(values, 1));
        assertEquals(7, EvalKit.percentile(List.of(7), 50));
        assertEquals(0, EvalKit.percentile(List.of(), 50));
    }

    @Test
    void e3_aTestWithFewerCasesThanTheMinimumInEitherArmDecidesNothing() {
        assertEquals("too few cases", EvalKit.abVerdict(150, 199, 190, 400));
        assertEquals("too few cases", EvalKit.abVerdict(150, 400, 190, 199));
        assertEquals("new is better", EvalKit.abVerdict(100, 200, 160, 200));
        assertEquals("no clear difference", EvalKit.abVerdict(10, 100, 19, 100, 50));
        assertEquals("too few cases", EvalKit.abVerdict(10, 100, 19, 100));
    }

    @Test
    void e4_aDifferenceIsCalledOnlyWhenItClearsThe95PercentBarAndTheBetterSideIsNamed() {
        assertEquals("new is better", EvalKit.abVerdict(410, 500, 438, 500));
        assertEquals("old is better", EvalKit.abVerdict(438, 500, 410, 500));
        assertEquals("no clear difference", EvalKit.abVerdict(410, 500, 431, 500));
        assertEquals("no clear difference", EvalKit.abVerdict(0, 300, 0, 300));
        assertEquals("no clear difference", EvalKit.abVerdict(300, 300, 300, 300));
    }

    @Test
    void e5_aShadowRunIsHeldForARegressionInAProtectedSegmentOrForMoreLossesThanGains() {
        List<EvalKit.Paired> pairs = new ArrayList<>();
        add(pairs, 2, "refund", false, true);
        add(pairs, 1, "policy", true, false);
        add(pairs, 1, "complaint", true, false);
        add(pairs, 5, "policy", true, true);
        assertEquals(new EvalKit.Gate("hold", 2, 2, List.of("complaint")), gate(pairs, Set.of("refund", "complaint")));
        assertEquals(new EvalKit.Gate("ship", 2, 2, List.of()), gate(pairs, Set.of("refund")));
        List<EvalKit.Paired> worse = new ArrayList<>();
        add(worse, 3, "policy", true, false);
        add(worse, 2, "policy", false, true);
        assertEquals(new EvalKit.Gate("hold", 3, 2, List.of()), gate(worse, Set.of()));
    }

    @Test
    void e6_diagnosisChecksTheEvidenceThenTheGroundingThenTheFormatThenTheStrongerModel() {
        assertEquals("retrieval or data", EvalKit.diagnose(false, false, false, false));
        assertEquals("ungrounded answer", EvalKit.diagnose(true, false, false, false));
        assertEquals("format instructions", EvalKit.diagnose(true, true, false, false));
        assertEquals("prompt or task", EvalKit.diagnose(true, true, true, false));
        assertEquals("model mismatch", EvalKit.diagnose(true, true, true, true));
    }

    @Test
    void e7_modelChoiceTakesTheCheapestOptionThatMeetsTheAccuracyFloorAndTheLatencyLimit() {
        List<EvalKit.Option> options = List.of(new EvalKit.Option("small", 84, 900, 1), new EvalKit.Option("medium", 91, 1800, 3), new EvalKit.Option("large", 95, 4200, 9));
        assertEquals("medium", EvalKit.chooseModel(options, 90, 2000));
        assertEquals("none", EvalKit.chooseModel(options, 94, 2000));
        assertEquals("medium", EvalKit.chooseModel(options, 91, 1800));
        assertEquals("small", EvalKit.chooseModel(options, 80, 5000));
        assertEquals("a", EvalKit.chooseModel(List.of(new EvalKit.Option("b", 90, 100, 2), new EvalKit.Option("a", 90, 100, 2)), 90, 100));
    }
}
