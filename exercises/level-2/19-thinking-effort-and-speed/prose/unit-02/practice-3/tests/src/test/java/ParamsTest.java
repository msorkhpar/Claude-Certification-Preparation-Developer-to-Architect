import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ParamsTest {
    private static final String FABLE = "claude-fable-5-1", OPUS = "claude-opus-5-5", SONNET = "claude-sonnet-5-5", HAIKU = "claude-haiku-4-5-20251001";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** The parameter named by the RejectedRequest, or null when the request was accepted. */
    private static String refused(String model, int maxTokens, Map<String, Object> options) {
        try {
            Params.buildParams(model, maxTokens, options);
        } catch (RejectedRequest e) {
            return e.param();
        } catch (RuntimeException e) {
            return "crash: " + e;
        }
        return null;
    }

    private static String refused(String model, Map<String, Object> options) {
        return refused(model, 4096, options);
    }

    private static Map<String, Object> orEmpty(Map<String, Object> m) {
        return m == null ? map() : m;
    }

    private static Map<String, Object> orEmpty(Map<String, Object> m, Map<String, Object> fallback) {
        return m == null ? fallback : m;
    }

    private static Map<String, Object> adaptive() {
        return map("type", "adaptive");
    }

    @Test
    void m1_eachCourseModelGetsTheRequestItAccepts() {
        assertEquals(map("model", FABLE, "max_tokens", 8000, "output_config", map("effort", "xhigh")),
                Params.buildParams(FABLE, 8000, map("effort", "xhigh")));
        assertEquals(map("model", OPUS, "max_tokens", 4096), Params.buildParams(OPUS, 4096, map()));
        assertEquals(map("model", SONNET, "max_tokens", 4096, "thinking", adaptive(), "output_config", map("effort", "medium")),
                Params.buildParams(SONNET, 4096, map("thinking", adaptive(), "effort", "medium")));
        assertEquals(map("model", HAIKU, "max_tokens", 4096, "thinking", map("type", "enabled", "budget_tokens", 2048)),
                Params.buildParams(HAIKU, 4096, map("thinking", map("type", "enabled", "budget_tokens", 2048))));
    }

    @Test
    void e1_thinkingModesAModelDoesNotHaveAreRefused() {
        for (String model : List.of(FABLE, OPUS, SONNET)) assertEquals("thinking", refused(model, map("thinking", map("type", "disabled"))), model);
        for (String model : List.of(OPUS, SONNET, FABLE)) {
            assertEquals("thinking", refused(model, map("thinking", map("type", "enabled", "budget_tokens", 2048))), model);
        }
        assertEquals("thinking", refused(HAIKU, map("thinking", adaptive())));
        assertNull(refused(HAIKU, map("thinking", map("type", "disabled"))));
        assertNull(refused(OPUS, map("thinking", adaptive())));
    }

    @Test
    void e2_theModeThatSkipsThinkingUpFrontIsSonnetOnlyAndNeedsHighEffortOrBelow() {
        Map<String, Object> between = map("type", "between_tools");
        assertEquals(between, orEmpty(Params.buildParams(SONNET, 4096, map("thinking", between))).get("thinking"));
        for (String level : List.of("low", "medium", "high")) assertNull(refused(SONNET, map("thinking", between, "effort", level)), level);
        for (String level : List.of("xhigh", "max")) assertEquals("thinking", refused(SONNET, map("thinking", between, "effort", level)), level);
        assertEquals("thinking", refused(OPUS, map("thinking", between)));
        assertEquals("thinking", refused(HAIKU, map("thinking", between)));
    }

    @Test
    void e3_effortNeedsASupportingModelAndARealLevel() {
        assertEquals("output_config.effort", refused(HAIKU, map("effort", "low")));
        for (String level : List.of("low", "medium", "high", "xhigh", "max")) {
            assertNull(refused(SONNET, map("effort", level)), level);
            assertNull(refused(FABLE, map("effort", level)), level);
        }
        assertEquals("output_config.effort", refused(OPUS, map("effort", "adaptive")));
        assertEquals("output_config.effort", refused(OPUS, map("effort", "extreme")));
    }

    @Test
    void e4_newerModelsRejectSamplingParametersAndHaikuKeepsThem() {
        assertEquals("temperature", refused(OPUS, map("temperature", 0.2)));
        assertEquals("top_p", refused(SONNET, map("top_p", 0.9)));
        assertEquals("top_k", refused(FABLE, map("top_k", 40)));
        assertNull(refused(OPUS, map("temperature", 1.0)));
        assertEquals(map("model", HAIKU, "max_tokens", 1024, "temperature", 0.2, "top_k", 40),
                Params.buildParams(HAIKU, 1024, map("temperature", 0.2, "top_k", 40)));
    }

    @Test
    void e5_fastModeIsOpusOnlyWithItsBetaHeaderAndNeverInABatch() {
        Map<String, Object> params = orEmpty(Params.buildParams(OPUS, 4096, map("speed", "fast")));
        assertEquals("fast", params.get("speed"));
        assertEquals(List.of("fast-mode-2026-02-01"), params.get("betas"));
        assertEquals("speed", refused(SONNET, map("speed", "fast")));
        assertEquals("speed", refused(HAIKU, map("speed", "fast")));
        assertEquals("speed", refused(OPUS, map("speed", "fast", "batch", true)));
        assertFalse(orEmpty(Params.buildParams(OPUS, 4096, map("speed", "standard")), map("speed", 1)).containsKey("speed"));
    }

    @Test
    void e6_aManualBudgetIsAtLeast1024AndBelowMaxTokens() {
        assertEquals("thinking.budget_tokens", refused(HAIKU, 4096, map("thinking", map("type", "enabled", "budget_tokens", 1023))));
        assertNull(refused(HAIKU, 2048, map("thinking", map("type", "enabled", "budget_tokens", 1024))));
        assertEquals("thinking.budget_tokens", refused(HAIKU, 2048, map("thinking", map("type", "enabled", "budget_tokens", 2048))));
        assertEquals("thinking.budget_tokens", refused(HAIKU, 2048, map("thinking", map("type", "enabled"))));
        assertEquals("max_tokens", refused(OPUS, 0, map()));
    }

    @Test
    void e7_effortLivesInOutputConfigAndNeverInsideThinking() {
        Map<String, Object> params = orEmpty(Params.buildParams(SONNET, 4096, map("thinking", adaptive(), "effort", "high")));
        assertEquals(adaptive(), params.get("thinking"));
        assertEquals(map("effort", "high"), params.get("output_config"));
        Map<String, Object> options = map("thinking", adaptive(), "effort", "low");
        Params.buildParams(SONNET, 4096, options);
        assertEquals(map("thinking", adaptive(), "effort", "low"), options);
    }
}
