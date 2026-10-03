import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RouterTest {
    // Prices in dollars per million tokens, read from the pricing page on 2026-10-02 (a dollar per MTok is a micro-dollar per token).
    private static Map<String, Object> model(String id, int tier, long context, long maxOutput, double input, double output, double mult) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("tier", tier);
        m.put("context", context);
        m.put("max_output", maxOutput);
        m.put("input", input);
        m.put("output", output);
        m.put("cache_read_multiplier", mult);
        return m;
    }

    private static final Map<String, Object> HAIKU = model("claude-haiku-4-5-20251001", 1, 200_000, 64_000, 1, 5, 0.1);
    private static final Map<String, Object> SONNET = model("claude-sonnet-5-5", 2, 1_000_000, 128_000, 2, 10, 0.1);
    private static final Map<String, Object> OPUS = model("claude-opus-5-5", 3, 1_000_000, 128_000, 4, 20, 0.05);
    private static final Map<String, Object> FABLE = model("claude-fable-5-1", 4, 1_000_000, 128_000, 10, 50, 0.025);
    private static final List<Map<String, Object>> CATALOG = List.of(HAIKU, SONNET, OPUS, FABLE);

    private static Map<String, Object> usage(Object... kv) {
        Map<String, Object> u = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) u.put((String) kv[i], kv[i + 1]);
        return u;
    }

    private static Map<String, Object> task(int minTier, Integer maxTokens, boolean batch, Map<String, Object> usage) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("min_tier", minTier);
        if (maxTokens != null) t.put("max_tokens", maxTokens);
        if (batch) t.put("batch", true);
        t.put("usage", usage);
        return t;
    }

    private static Map<String, Object> task(int minTier, Map<String, Object> usage) {
        return task(minTier, null, false, usage);
    }

    private static double cost(Map<String, Object> model, Map<String, Object> usage) {
        return Router.requestCost(model, usage, false);
    }

    private static List<Map<String, Object>> reversed() {
        List<Map<String, Object>> r = new ArrayList<>(CATALOG);
        Collections.reverse(r);
        return r;
    }

    private static Throwable raised(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            return e;
        }
        return null;
    }

    @Test
    void m1_costOfAPlainRequestAndTheCheapestModelThatMeetsTheTier() {
        assertEquals(5400.0, cost(SONNET, usage("input_tokens", 1200, "output_tokens", 300)));
        assertEquals(2000.0, cost(HAIKU, usage("input_tokens", 1000, "output_tokens", 200)));
        assertEquals(HAIKU.get("id"), Router.route(CATALOG, task(1, usage("input_tokens", 1000, "output_tokens", 200))));
        assertEquals(SONNET.get("id"), Router.route(CATALOG, task(2, usage("input_tokens", 1000, "output_tokens", 200))));
    }

    @Test
    void e1_cacheReadsAndWritesArePricedByTheirOwnMultipliers() {
        Map<String, Object> split = usage("ephemeral_5m_input_tokens", 1000, "ephemeral_1h_input_tokens", 2000);
        Map<String, Object> u = usage("input_tokens", 100, "output_tokens", 50, "cache_read_input_tokens", 4000,
                "cache_creation_input_tokens", 3000, "cache_creation", split);
        assertEquals(23200.0, cost(OPUS, u)); // 400 + 5000 + 16000 + 800 + 1000
        assertEquals(250000.0, cost(FABLE, usage("cache_read_input_tokens", 1_000_000)));
        assertEquals(200000.0, cost(SONNET, usage("cache_read_input_tokens", 1_000_000)));
        // without the per-lifetime split, the written tokens are the 5-minute kind
        assertEquals(7500.0, cost(SONNET, usage("cache_creation_input_tokens", 3000)));
    }

    @Test
    void e2_theBatchDiscountHalvesEveryPartOfTheCost() {
        Map<String, Object> split = usage("ephemeral_5m_input_tokens", 1000, "ephemeral_1h_input_tokens", 2000);
        Map<String, Object> u = usage("input_tokens", 100, "output_tokens", 50, "cache_read_input_tokens", 4000, "cache_creation", split);
        assertEquals(11600.0, Router.requestCost(OPUS, u, true));
        assertEquals(23200.0, cost(OPUS, u));
        assertEquals(2500.0, Router.requestCost(HAIKU, usage("output_tokens", 1000), true));
    }

    @Test
    void e3_theRouterPicksByCostAndTierNotByTheOrderOfTheCatalog() {
        assertEquals(OPUS.get("id"), Router.route(reversed(), task(3, usage("input_tokens", 500, "output_tokens", 100))));
        assertEquals(HAIKU.get("id"), Router.route(reversed(), task(1, usage("input_tokens", 500, "output_tokens", 100))));
        assertEquals(OPUS.get("id"), Router.route(List.of(FABLE, OPUS), task(1, usage("input_tokens", 500, "output_tokens", 100))));
        // batch changes every price by the same factor, so it never changes the winner
        assertEquals(SONNET.get("id"), Router.route(CATALOG, task(2, null, true, usage("input_tokens", 500))));
    }

    @Test
    void e4_aModelWhoseContextOrOutputLimitIsTooSmallIsSkipped() {
        Map<String, Object> big = usage("input_tokens", 250_000, "cache_read_input_tokens", 50_000, "output_tokens", 100);
        assertEquals(SONNET.get("id"), Router.route(CATALOG, task(1, big)));
        assertEquals(SONNET.get("id"), Router.route(CATALOG, task(1, 100_000, false, usage("input_tokens", 100, "output_tokens", 100))));
        assertEquals(HAIKU.get("id"), Router.route(CATALOG, task(1, 64_000, false, usage("input_tokens", 100))));
    }

    @Test
    void e5_deprecatedModelsAreSkippedAndAnEmptyChoiceRaises() {
        Map<String, Object> old = new LinkedHashMap<>(HAIKU);
        old.put("deprecated", true);
        assertEquals(SONNET.get("id"), Router.route(List.of(old, SONNET), task(1, usage("input_tokens", 100))));
        assertTrue(raised(() -> Router.route(List.of(old), task(1, usage("input_tokens", 100)))) instanceof NoModelError);
        assertTrue(raised(() -> Router.route(CATALOG, task(5, usage("input_tokens", 100)))) instanceof NoModelError);
        assertTrue(raised(() -> Router.route(CATALOG, task(1, usage("input_tokens", 2_000_000)))) instanceof NoModelError);
    }

    @Test
    void e6_aTieGoesToTheLowerTier() {
        Map<String, Object> reads = usage("cache_read_input_tokens", 1_000_000);
        assertEquals(200000.0, cost(SONNET, reads));
        assertEquals(200000.0, cost(OPUS, reads));
        assertEquals(SONNET.get("id"), Router.route(CATALOG, task(2, reads)));
        assertEquals(SONNET.get("id"), Router.route(reversed(), task(2, reads)));
    }
}
