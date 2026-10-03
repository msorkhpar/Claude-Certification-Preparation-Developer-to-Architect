import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DistributeTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> tool(String name, String tag, Object... more) {
        Map<String, Object> m = map("name", name, "tags", List.of(tag));
        for (int i = 0; i < more.length; i += 2) m.put((String) more[i], more[i + 1]);
        return m;
    }

    private static List<Map<String, Object>> catalog() {
        return new ArrayList<>(List.of(tool("web_search", "web"), tool("fetch_page", "web"), tool("load_document", "documents"), tool("extract_data_points", "documents"),
                tool("summarize_content", "synthesis"), tool("verify_fact", "web", "scoped", true), tool("write_report", "reports"), tool("publish_report", "reports", "irreversible", true)));
    }

    private static Map<String, Object> role(String specialisation, String... extra) {
        return extra.length == 0 ? map("specialisation", List.of(specialisation)) : map("specialisation", List.of(specialisation), "extra", List.of(extra));
    }

    private static Map<String, Map<String, Object>> roles() {
        Map<String, Map<String, Object>> r = new LinkedHashMap<>();
        r.put("searcher", role("web"));
        r.put("analyst", role("documents"));
        r.put("synthesizer", role("synthesis", "verify_fact"));
        r.put("reporter", role("reports"));
        return r;
    }

    private static Map<String, Map<String, Object>> one(String name, Map<String, Object> role) {
        Map<String, Map<String, Object>> r = new LinkedHashMap<>();
        r.put(name, role);
        return r;
    }

    private static Map<String, List<String>> assign(Map<String, Map<String, Object>> roles, int budget) {
        Map<String, List<String>> result = Distribute.assignTools(roles, catalog(), budget);
        assertNotNull(result, "assignTools returned nothing");
        return result;
    }

    private static Map<String, Object> policy() {
        return map("tools", map("process_refund", map("cap", 500, "irreversible", true), "lookup_order", map("cap", null, "irreversible", false)));
    }

    private static Map<String, Object> call(Object... over) {
        Map<String, Object> c = map("id", "c1", "tool", "process_refund", "amount", 50, "customer", "C-1", "verified_customer", "C-1");
        for (int i = 0; i < over.length; i += 2) c.put((String) over[i], over[i + 1]);
        return c;
    }

    private static Map<String, Object> decide(Map<String, Object> c, Set<String> approvals) {
        Map<String, Object> answer = Distribute.authorize(c, policy(), approvals);
        assertNotNull(answer, "authorize returned nothing");
        return answer;
    }

    private static Map<String, Object> plan(String model, String need, List<String> tools, String forced, boolean manual) {
        Map<String, Object> result = Distribute.planTurn(model, need, tools, forced, manual);
        assertNotNull(result, "planTurn returned nothing");
        return result;
    }

    private static Map<String, Object> turn(Object choice, List<String> tools, boolean strict) {
        return map("tool_choice", choice, "tools", tools, "strict", strict, "verify_call", strict);
    }

    @Test
    void m1_eachRoleGetsOnlyTheToolsOfItsSpecialisationInCatalogOrder() {
        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("searcher", List.of("web_search", "fetch_page", "verify_fact"));
        expected.put("analyst", List.of("load_document", "extract_data_points"));
        expected.put("synthesizer", List.of("summarize_content", "verify_fact"));
        expected.put("reporter", List.of("write_report"));
        assertEquals(expected, assign(roles(), 5));
    }

    @Test
    void e1_aRoleOverItsBudgetOrGivenAnUnknownOrUnscopedOutsideToolOrADuplicateCatalogNameIsRefused() {
        int refused = 0;
        List<Map<String, Object>> duplicated = catalog();
        duplicated.add(tool("web_search", "web"));
        Object[][] cases = {
            {roles(), catalog(), 2},
            {one("synthesizer", role("synthesis", "nope")), catalog(), 5},
            {one("synthesizer", role("synthesis", "fetch_page")), catalog(), 5},
            {roles(), duplicated, 5},
            {one("nobody", map("specialisation", List.of())), catalog(), 5},
        };
        for (Object[] c : cases) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> r = (Map<String, Map<String, Object>>) c[0];
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cat = (List<Map<String, Object>>) c[1];
                Distribute.assignTools(r, cat, (Integer) c[2]);
            } catch (IllegalArgumentException expected) {
                refused++;
            }
        }
        assertEquals(5, refused);
        assertEquals(List.of("web_search", "fetch_page", "verify_fact"), assign(roles(), 3).get("searcher"));
        assertEquals(List.of("web_search", "fetch_page", "verify_fact"), assign(one("searcher", role("web", "fetch_page")), 5).get("searcher"));
    }

    @Test
    void e2_anIrreversibleToolIsGivenOnlyByAnExplicitGrant() {
        assertFalse(assign(roles(), 5).get("reporter").contains("publish_report"));
        assertEquals(List.of("write_report", "publish_report"), assign(one("reporter", role("reports", "publish_report")), 5).get("reporter"));
        assertEquals(List.of("load_document", "extract_data_points", "publish_report"), assign(one("analyst", role("documents", "publish_report")), 5).get("analyst"));
    }

    @Test
    void e3_aModelThatAcceptsForcingGetsTheNativeChoiceAndTheOthersGetAutoWithStrictToolsAndOneNamedTool() {
        List<String> tools = List.of("extract_metadata", "enrich");
        assertEquals(map("tool_choice", map("type", "tool", "name", "extract_metadata"), "tools", tools, "strict", false, "verify_call", false), plan("claude-opus-5", "named", tools, "extract_metadata", false));
        assertEquals(map("tool_choice", map("type", "any"), "tools", tools, "strict", false, "verify_call", false), plan("claude-opus-5", "any", tools, null, false));
        for (String model : List.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")) {
            assertEquals(turn(map("type", "auto"), List.of("extract_metadata"), true), plan(model, "named", tools, "extract_metadata", false));
            assertEquals(turn(map("type", "auto"), tools, true), plan(model, "any", tools, null, false));
            assertEquals(map("type", "auto"), plan(model, "free", tools, null, false).get("tool_choice"));
            assertEquals(map("type", "none"), plan(model, "none", tools, null, false).get("tool_choice"));
        }
        assertEquals(List.of("enrich"), plan("claude-opus-5", "named", tools, "enrich", true).get("tools"));
        int refused = 0;
        for (String need : List.of("sometimes", "named")) {
            try {
                Distribute.planTurn("claude-opus-5", need, tools);
            } catch (IllegalArgumentException expected) {
                refused++;
            }
        }
        assertEquals(2, refused);
    }

    @Test
    void e4_aChangeOfToolChoiceCostsTheCachedMessagesAChangeOfToolsCostsEverythingAndARepeatCostsNothing() {
        List<String> tools = List.of("extract_metadata", "enrich");
        Map<String, Object> free = map("tool_choice", map("type", "auto"), "tools", tools);
        assertEquals("none", Distribute.cacheImpact(null, free));
        assertEquals("none", Distribute.cacheImpact(free, map("tool_choice", map("type", "auto"), "tools", List.of("extract_metadata", "enrich"))));
        Map<String, Object> any = map("tool_choice", map("type", "any"), "tools", tools);
        assertEquals("messages", Distribute.cacheImpact(free, any));
        assertEquals("messages", Distribute.cacheImpact(any, free));
        Map<String, Object> a = map("tool_choice", map("type", "tool", "name", "a"), "tools", tools), b = map("tool_choice", map("type", "tool", "name", "b"), "tools", tools);
        assertEquals("messages", Distribute.cacheImpact(a, b));
        assertEquals("none", Distribute.cacheImpact(a, a));
        Map<String, Object> fallback = plan("claude-sonnet-5-5", "named", tools, "extract_metadata", false);
        assertEquals("all", Distribute.cacheImpact(free, fallback));
        assertEquals("all", Distribute.cacheImpact(free, map("tool_choice", map("type", "auto"), "tools", List.of("enrich"))));
    }

    @Test
    void e5_aReplyIsCheckedAgainstTheCallThatWasRequired() {
        Map<String, Object> text = map("type", "text", "text", "I think so."), useA = map("type", "tool_use", "name", "a"), useB = map("type", "tool_use", "name", "b");
        assertEquals("missed_call", Distribute.checkTurn(List.of(text), "any"));
        assertEquals("ok", Distribute.checkTurn(List.of(text, useB), "any"));
        assertEquals("missed_call", Distribute.checkTurn(List.of(), "named", "a"));
        assertEquals("wrong_tool", Distribute.checkTurn(List.of(useB), "named", "a"));
        assertEquals("ok", Distribute.checkTurn(List.of(useA, useB), "named", "a"));
        assertEquals("wrong_tool", Distribute.checkTurn(List.of(useB, useA), "named", "a"));
        assertEquals("ok", Distribute.checkTurn(List.of(text), "free"));
        assertEquals("ok", Distribute.checkTurn(List.of(useA), "none"));
    }

    @Test
    void e6_anUnknownToolAWrongOwnerOrABadAmountIsRefused() {
        Map<String, Object> unknown = decide(call("tool", "delete_account"), Set.of());
        assertEquals(false, unknown.get("allowed"));
        assertEquals("unknown_tool", unknown.get("code"));
        assertEquals(false, unknown.get("escalate"));
        for (String other : new String[] {"C-2", null}) {
            Map<String, Object> answer = decide(call("customer", other), Set.of());
            assertEquals(false, answer.get("allowed"));
            assertEquals("not_owner", answer.get("code"));
            assertEquals(false, answer.get("escalate"));
        }
        assertEquals("not_owner", decide(call("verified_customer", null), Set.of()).get("code"));
        for (Object amount : new Object[] {0, -5, 12.5, "50", true, null}) {
            Map<String, Object> answer = decide(call("amount", amount), Set.of("c1"));
            assertEquals(false, answer.get("allowed"), String.valueOf(amount));
            assertEquals("bad_amount", answer.get("code"), String.valueOf(amount));
        }
        Map<String, Object> free = decide(map("id", "c2", "tool", "lookup_order", "customer", "C-1", "verified_customer", "C-1"), Set.of());
        assertEquals(true, free.get("allowed"));
        assertEquals("ok", free.get("code"));
    }

    @Test
    void e7_theCapIsInclusiveAndAnIrreversibleCallNeedsApprovalThatNeverLiftsTheCap() {
        assertEquals("ok", decide(call("amount", 500), Set.of("c1")).get("code"));
        Map<String, Object> waiting = decide(call("amount", 50), Set.of());
        assertEquals(false, waiting.get("allowed"));
        assertEquals("needs_approval", waiting.get("code"));
        assertEquals(true, waiting.get("escalate"));
        assertEquals(true, decide(call("amount", 50), Set.of("c1")).get("allowed"));
        assertEquals("needs_approval", decide(call("amount", 50), Set.of("c9")).get("code"));
        Map<String, Object> over = decide(call("amount", 501), Set.of("c1"));
        assertEquals(false, over.get("allowed"));
        assertEquals("over_cap", over.get("code"));
        assertEquals(true, over.get("escalate"));
        assertTrue(((String) over.get("message")).contains("500"));
        assertEquals("not_owner", decide(call("amount", 501, "customer", "C-2"), Set.of()).get("code"));
    }
}
