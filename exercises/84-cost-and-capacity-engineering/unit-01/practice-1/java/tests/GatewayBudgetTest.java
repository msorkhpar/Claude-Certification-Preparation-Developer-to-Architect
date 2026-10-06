import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GatewayBudgetTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static final Map<String, Object> POLICY = map("allowed", List.of("haiku", "sonnet"), "routes", map("classify", "haiku", "draft", "sonnet", "review", "opus"), "default", "sonnet",
            "cheaper", map("opus", "sonnet", "sonnet", "haiku"));
    private static final Map<String, Object> PRICES = map("haiku", map("input", 100, "cache_read", 10, "output", 500), "sonnet", map("input", 200, "cache_read", 20, "output", 1000));

    private static String route(Map<String, Object> request, String status) {
        return GatewayBudget.route(request, POLICY, status);
    }

    private static String route(Map<String, Object> request) {
        return route(request, "allow");
    }

    private static String admit(long spend, long budget, long estimate) {
        String result = GatewayBudget.admit(spend, budget, estimate);
        assertNotNull(result, "admit returned nothing");
        return result;
    }

    private static List<Map<String, Object>> showback(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = GatewayBudget.showback(rows, PRICES);
        assertNotNull(result, "showback returned nothing");
        return result;
    }

    private static Map<String, Object> usage(String team, String model, long input, long cacheRead, long output) {
        return map("team", team, "model", model, "input", input, "cache_read", cacheRead, "output", output);
    }

    private static Map<String, Object> cost(String team, long cents) {
        return map("team", team, "cents", cents);
    }

    @Test
    void m1_aRequestFollowsTheRouteTableOfTheGateway() {
        assertEquals("haiku", route(map("task", "classify")));
        assertEquals("sonnet", route(map("task", "draft")));
        assertEquals("opus", route(map("task", "review")));
        assertEquals("sonnet", route(map("task", "translate")));
    }

    @Test
    void e1_aTeamNearItsBudgetIsMovedToACheaperModelAndATeamOverItIsRefused() {
        assertEquals("sonnet", route(map("task", "review"), "warn"));
        assertEquals("haiku", route(map("task", "draft"), "warn"));
        assertEquals("haiku", route(map("task", "classify"), "warn"));
        assertNull(route(map("task", "review"), "block"));
    }

    @Test
    void e2_aRequestIsAdmittedWarnedOrBlockedAgainstTheBudget() {
        assertEquals("allow", admit(0, 1000, 100));
        assertEquals("allow", admit(700, 1000, 99));
        assertEquals("warn", admit(700, 1000, 100));
        assertEquals("warn", admit(900, 1000, 100));
        assertEquals("block", admit(900, 1000, 101));
        assertEquals("block", admit(0, 0, 0));
        assertEquals("block", admit(0, -5, 0));
    }

    @Test
    void e3_showbackAddsEachTeamsTokensAtThePriceOfTheModelAndRefusesAnUnknownModel() {
        List<Map<String, Object>> rows = List.of(usage("a", "sonnet", 1_000_000, 5_000_000, 100_000), usage("b", "haiku", 2_000_000, 0, 1_000_000), usage("a", "haiku", 500_000, 0, 0));
        assertEquals(List.of(cost("b", 700), cost("a", 450)), showback(rows));
        assertEquals("a", showback(List.of(usage("b", "sonnet", 1, 0, 0), usage("a", "sonnet", 1, 0, 0))).get(0).get("team"));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> GatewayBudget.showback(List.of(usage("a", "other", 1, 0, 0)), PRICES));
        assertTrue(error.getMessage().contains("unknown model: other"));
        assertEquals(List.of(), showback(List.of()));
    }

    @Test
    void e4_showbackRoundsEachTeamsTotalToACentOnce() {
        assertEquals(List.of(cost("x", 1), cost("y", 1), cost("z", 0)),
                showback(List.of(usage("x", "sonnet", 2000, 0, 0), usage("x", "sonnet", 2000, 0, 0), usage("y", "sonnet", 2500, 0, 0), usage("z", "sonnet", 2499, 0, 0))));
    }

    private static String pick(long p95, long timeout, long margin) {
        String result = GatewayBudget.delivery(p95, timeout, margin);
        assertNotNull(result, "delivery returned nothing");
        return result;
    }

    @Test
    void e5_aCallerWithAHardLatencyLimitGetsAcceptAndPollWhenTheSlowCaseDoesNotFit() {
        assertEquals("sync", pick(8, 10, 25));
        assertEquals("accept-and-poll", pick(8, 10, 26));
        assertEquals("accept-and-poll", pick(30, 10, 0));
        assertEquals("sync", pick(10, 10, 0));
        assertEquals("accept-and-poll", pick(10, 10, 1));
    }

    @Test
    void e6_aModelPinnedByATeamIsHonouredOnlyWhenThePolicyAllowsIt() {
        assertEquals("sonnet", route(map("task", "classify", "model", "sonnet")));
        assertEquals("haiku", route(map("task", "classify", "model", "opus")));
        assertEquals("haiku", route(map("task", "review", "model", "haiku")));
        assertEquals("haiku", route(map("task", "draft", "model", "sonnet"), "warn"));
    }
}
