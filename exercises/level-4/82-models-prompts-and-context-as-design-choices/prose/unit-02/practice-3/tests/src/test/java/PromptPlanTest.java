import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptPlanTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> with(Map<String, Object> base, String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<>(base);
        m.put(key, value);
        return m;
    }

    private static final Map<String, Object> ROLE = map("name", "role", "static", true, "text", "r".repeat(400)); // 100 tokens
    private static final Map<String, Object> POLICY = map("name", "policy", "static", true, "text", "p".repeat(1648)); // 412 tokens: the prefix is exactly 512
    private static final Map<String, Object> HISTORY = map("name", "history", "static", false, "priority", 1, "text", "h".repeat(200)); // 50 tokens
    private static final Map<String, Object> QUESTION = map("name", "question", "static", false, "priority", 9, "text", "Q: {q}");
    private static final Map<String, Object> EXTRA = map("name", "extra", "static", false, "priority", 1, "text", "e".repeat(160)); // 40 tokens

    private static Map<String, Object> assemble(List<Map<String, Object>> modules, Map<String, String> variables, int budget) {
        Map<String, Object> result = PromptPlan.assemble(modules, variables, budget);
        assertNotNull(result, "assemble returned nothing");
        return result;
    }

    private static Map<String, Object> assemble(List<Map<String, Object>> modules, Map<String, String> variables) {
        return assemble(modules, variables, 10_000);
    }

    @SuppressWarnings("unchecked")
    private static List<String> names(Map<String, Object> prompt) {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> b : (List<Map<String, Object>>) prompt.get("blocks")) out.add((String) b.get("name"));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static String text(Map<String, Object> prompt, int index) {
        return (String) ((List<Map<String, Object>>) prompt.get("blocks")).get(index).get("text");
    }

    private static String refused(List<Map<String, Object>> modules, Map<String, String> variables, int budget) {
        try {
            PromptPlan.assemble(modules, variables, budget);
        } catch (IllegalArgumentException error) {
            return error.getMessage();
        }
        return fail("the modules were accepted");
    }

    private static String refused(List<Map<String, Object>> modules, Map<String, String> variables) {
        return refused(modules, variables, 10_000);
    }

    @Test
    void m1_staticModulesComeFirstAndTheBreakpointFollowsTheLastOne() {
        Map<String, Object> prompt = assemble(List.of(QUESTION, ROLE, HISTORY, POLICY), Map.of("q", "hello"));
        assertEquals(List.of("role", "policy", "question", "history"), names(prompt));
        assertEquals(1, prompt.get("breakpoint"));
        assertEquals(564, prompt.get("tokens"));
        assertEquals(List.of(), prompt.get("dropped"));
    }

    @Test
    void e1_aVariableInAStaticModuleIsRefused() {
        assertTrue(refused(List.of(ROLE, with(POLICY, "text", "policy for {customer}"), QUESTION), Map.of("q", "x", "customer", "Ana")).contains("static"));
    }

    @Test
    void e2_dynamicVariablesAreFilledAndAMissingOneIsRefused() {
        Map<String, Object> prompt = assemble(List.of(ROLE, POLICY, QUESTION), Map.of("q", "hello", "unused", "x"));
        assertEquals("Q: hello", text(prompt, 2));
        assertTrue(refused(List.of(ROLE, POLICY, QUESTION), Map.of()).contains("missing variable: q"));
    }

    @Test
    void e3_theLowestPriorityDynamicModuleIsDroppedFirstAndATieDropsTheLaterOne() {
        List<Map<String, Object>> modules = List.of(ROLE, POLICY, HISTORY, QUESTION, EXTRA);
        Map<String, Object> one = assemble(modules, Map.of("q", "hello"), 570);
        assertEquals(List.of("extra"), one.get("dropped"));
        assertEquals(List.of("role", "policy", "history", "question"), names(one));
        assertEquals(564, one.get("tokens"));
        Map<String, Object> two = assemble(modules, Map.of("q", "hello"), 520);
        assertEquals(List.of("extra", "history"), two.get("dropped"));
        assertEquals(List.of("role", "policy", "question"), names(two));
        assertEquals(514, two.get("tokens"));
    }

    @Test
    void e4_staticModulesAreNeverDroppedAndABudgetTheyExceedIsRefused() {
        assertTrue(refused(List.of(ROLE, POLICY, HISTORY, QUESTION), Map.of("q", "hello"), 400).contains("over budget"));
        assertTrue(refused(List.of(ROLE, POLICY), Map.of(), 511).contains("over budget"));
        assertEquals(512, assemble(List.of(ROLE, POLICY), Map.of(), 512).get("tokens"));
    }

    @Test
    void e5_aPrefixUnderTheMinimumGetsNoBreakpoint() {
        assertEquals(512, PromptPlan.MIN_CACHEABLE);
        assertEquals(1, assemble(List.of(ROLE, POLICY), Map.of()).get("breakpoint"));
        assertNull(assemble(List.of(ROLE, with(POLICY, "text", "p".repeat(1644))), Map.of()).get("breakpoint"));
        assertNull(assemble(List.of(HISTORY, QUESTION), Map.of("q", "x")).get("breakpoint"));
    }

    private static final List<Map<String, Object>> MODELS = List.of(
            map("name", "small", "tier", 1, "latency_ms", 300, "price_out", 1),
            map("name", "mid2", "tier", 2, "latency_ms", 900, "price_out", 5),
            map("name", "big", "tier", 3, "latency_ms", 2500, "price_out", 25),
            map("name", "mid", "tier", 2, "latency_ms", 900, "price_out", 5));

    private static String pick(int tier, int latency) {
        return PromptPlan.chooseModel(map("tier", tier, "max_latency_ms", latency), MODELS);
    }

    @Test
    void e6_theCheapestModelThatMeetsTheTierAndTheLatencyWinsAndTiesGoByName() {
        assertEquals("small", pick(1, 5000));
        assertEquals("mid", pick(2, 1000));
        assertEquals("big", pick(3, 5000));
        assertNull(pick(3, 1000));
        assertNull(pick(2, 500));
        assertNull(pick(1, 100));
    }

    private static int reuse(Map<String, Object> a, Map<String, Object> b) {
        Integer result = PromptPlan.reusablePrefix(a, b);
        assertNotNull(result, "reusablePrefix returned nothing");
        return result;
    }

    @Test
    void e7_onlyAnIdenticalStaticPrefixCanBeReused() {
        Map<String, Object> base = assemble(List.of(ROLE, POLICY, HISTORY, QUESTION), Map.of("q", "one"));
        assertEquals(512, reuse(base, assemble(List.of(ROLE, POLICY, HISTORY, QUESTION), Map.of("q", "a different question"))));
        assertEquals(0, reuse(base, assemble(List.of(with(ROLE, "text", "R".repeat(400)), POLICY, HISTORY, QUESTION), Map.of("q", "one"))));
        assertEquals(0, reuse(base, assemble(List.of(ROLE, with(POLICY, "text", "P".repeat(1648)), HISTORY, QUESTION), Map.of("q", "one"))));
        assertEquals(512, reuse(base, assemble(List.of(ROLE, POLICY, with(HISTORY, "text", "other"), QUESTION), Map.of("q", "one"))));
        assertEquals(0, reuse(base, assemble(List.of(HISTORY, QUESTION), Map.of("q", "one"))));
        assertEquals(0, reuse(assemble(List.of(HISTORY), Map.of()), assemble(List.of(HISTORY), Map.of())));
    }
}
