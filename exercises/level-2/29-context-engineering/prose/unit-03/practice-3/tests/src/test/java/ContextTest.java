import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ContextTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> user(String text) {
        return map("role", "user", "content", text);
    }

    private static Map<String, Object> said(String text) {
        return map("role", "assistant", "content", List.of(map("type", "text", "text", text)));
    }

    private static Map<String, Object> call(String id, String name, Object... input) {
        return map("role", "assistant", "content", List.of(map("type", "tool_use", "id", id, "name", name, "input", map(input))));
    }

    private static Map<String, Object> result(String id, String text, boolean error) {
        Map<String, Object> block = map("type", "tool_result", "tool_use_id", id, "content", text);
        if (error) block.put("is_error", true);
        return map("role", "user", "content", List.of(block));
    }

    private static Map<String, Object> result(String id, String text) {
        return result(id, text, false);
    }

    private static String rep(String s, int n) {
        return s.repeat(n);
    }

    @SafeVarargs
    private static List<Map<String, Object>> list(Map<String, Object>... items) {
        return new ArrayList<>(List.of(items));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> blocksOf(List<Map<String, Object>> messages) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (messages == null) return out;
        for (Map<String, Object> m : messages) if (m.get("content") instanceof List<?> l) for (Object b : l) out.add((Map<String, Object>) b);
        return out;
    }

    private static List<Map<String, Object>> resultBlocks(List<Map<String, Object>> messages) {
        return blocksOf(messages).stream().filter(b -> "tool_result".equals(b.get("type"))).collect(Collectors.toList());
    }

    /** Tool results without their call, and calls without their result. */
    private static List<Object> orphans(List<Map<String, Object>> messages) {
        TreeSet<Object> calls = blocksOf(messages).stream().filter(b -> "tool_use".equals(b.get("type"))).map(b -> b.get("id")).collect(Collectors.toCollection(TreeSet::new));
        TreeSet<Object> answered = resultBlocks(messages).stream().map(b -> b.get("tool_use_id")).collect(Collectors.toCollection(TreeSet::new));
        TreeSet<Object> both = new TreeSet<>(calls);
        both.retainAll(answered);
        TreeSet<Object> all = new TreeSet<>(calls);
        all.addAll(answered);
        all.removeAll(both);
        return new ArrayList<>(all);
    }

    @SuppressWarnings("unchecked")
    private static String firstText(Map<String, Object> m) {
        Object content = m.get("content");
        if (content == null) return "no result";
        return content instanceof String s ? s : (String) ((List<Map<String, Object>>) content).get(0).get("text");
    }

    private static List<Map<String, Object>> conversation() {
        return list(user("find the invoice"), call("t1", "search", "q", "invoice"), result("t1", rep("A", 400)), call("t2", "read", "doc", "seven"), result("t2", rep("B", 400), true),
                call("t3", "search", "q", "total"), result("t3", rep("C", 400)), call("t4", "calc", "expr", "1+1"), result("t4", rep("D", 40)), said("The total is 2."));
    }

    private static List<Map<String, Object>> turnsConversation() {
        return list(user("first question"), said("first answer " + rep("x", 80)),
                user("second question"), call("a1", "search"), result("a1", rep("R", 300)), said("second answer"),
                user("third question"), said("third answer " + rep("y", 60)),
                user("fourth question"), call("a2", "search"), result("a2", rep("S", 100)), said("fourth answer"));
    }

    private static final class Summariser implements Function<List<Map<String, Object>>, String> {
        final List<String> texts = new ArrayList<>();
        final List<List<Map<String, Object>>> calls = new ArrayList<>();

        Summariser(String... texts) {
            Collections.addAll(this.texts, texts);
        }

        @Override
        @SuppressWarnings("unchecked")
        public String apply(List<Map<String, Object>> messages) {
            calls.add((List<Map<String, Object>>) Json.parse(Json.stringify(messages)));
            return texts.remove(0);
        }
    }

    private static List<Map<String, Object>> orEmpty(List<Map<String, Object>> messages) {
        return messages == null ? List.of(map("no", "result")) : messages;
    }

    @Test
    void m1_anOverBudgetConversationBecomesASummaryAndTheNewestTurn() {
        List<Map<String, Object>> messages = turnsConversation();
        int budget = Context.countTokens(messages) / 2;
        Summariser s = new Summariser("The user asked three things.");
        List<Map<String, Object>> done = orEmpty(Context.compact(messages, budget, s, 1));
        assertEquals(1, s.calls.size());
        assertEquals(messages.subList(0, 8), s.calls.get(0));
        assertEquals(List.of("user", "assistant", "user", "assistant"), done.stream().map(m -> m.get("role")).collect(Collectors.toList()));
        assertEquals(List.of(map("type", "text", "text", "<summary>\nThe user asked three things.\n</summary>"), map("type", "text", "text", "fourth question")), done.get(0).get("content"));
        assertEquals(messages.subList(9, 12), done.subList(1, done.size()));
        assertTrue(Context.countTokens(done) <= budget);
    }

    @Test
    void e1_oldToolResultsAreClearedButTheirCallsAndFlagsStay() {
        List<Map<String, Object>> messages = conversation();
        List<Map<String, Object>> before = conversation();
        List<Map<String, Object>> cleared = orEmpty(Context.clearToolResults(messages, 2, List.of(), "[cleared]"));
        assertEquals(before, messages);
        assertEquals(List.of("t1/true/false", "t2/true/true", "t3/false/false", "t4/false/false"), resultBlocks(cleared).stream()
                .map(b -> b.get("tool_use_id") + "/" + "[cleared]".equals(b.get("content")) + "/" + b.getOrDefault("is_error", false)).collect(Collectors.toList()));
        assertEquals(messages.stream().filter(m -> "assistant".equals(m.get("role"))).collect(Collectors.toList()),
                cleared.stream().filter(m -> "assistant".equals(m.get("role"))).collect(Collectors.toList()));
        assertEquals(List.of("gone", "gone", "gone", "gone"), resultBlocks(Context.clearToolResults(messages, 0, List.of(), "gone")).stream().map(b -> b.get("content")).collect(Collectors.toList()));
        assertEquals(List.of("t1/true", "t2/false", "t3/false", "t4/false"), resultBlocks(Context.clearToolResults(messages, 2, List.of("read"), "[cleared]")).stream()
                .map(b -> b.get("tool_use_id") + "/" + "[cleared]".equals(b.get("content"))).collect(Collectors.toList()));
    }

    @Test
    void e2_theWindowDropsWholeTurnsAndNeverSplitsAToolCallFromItsResult() {
        List<Map<String, Object>> messages = turnsConversation();
        int total = Context.countTokens(messages);
        for (int budget = total; budget > 0; budget -= 7) {
            List<Map<String, Object>> kept = orEmpty(Context.window(messages, budget));
            assertEquals(List.of(), orphans(kept), "budget " + budget);
            assertTrue(List.of("first question", "second question", "third question", "fourth question").contains(firstText(kept.get(0))), "budget " + budget);
        }
        List<Map<String, Object>> two = orEmpty(Context.window(messages, Context.countTokens(messages.subList(6, 12)) + 1));
        assertEquals(List.of("third question", "fourth question"), two.stream().filter(m -> "user".equals(m.get("role")) && m.get("content") instanceof String).map(m -> m.get("content")).collect(Collectors.toList()));
        assertEquals(messages, orEmpty(Context.window(messages, total)));
        assertEquals(messages.subList(8, 12), orEmpty(Context.window(messages, 1)));
        List<Map<String, Object>> expected = new ArrayList<>(messages.subList(0, 2));
        expected.addAll(messages.subList(8, 12));
        assertEquals(expected, orEmpty(Context.window(messages, Context.countTokens(expected) + 1, true)));
    }

    @Test
    void e3_aConversationWithinBudgetOrWithNothingOlderIsLeftAlone() {
        List<Map<String, Object>> messages = turnsConversation();
        Summariser s = new Summariser("unused");
        assertEquals(messages, orEmpty(Context.compact(messages, Context.countTokens(messages), s)));
        List<Map<String, Object>> single = list(user("one long question " + rep("z", 400)), said("answer"));
        assertEquals(single, orEmpty(Context.compact(single, 5, s)));
        assertEquals(messages, orEmpty(Context.compact(messages, 10_000, s, 2)));
        assertEquals(0, s.calls.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void e4_aSecondCompactionFoldsTheEarlierSummaryIntoTheNewOne() {
        List<Map<String, Object>> messages = turnsConversation();
        Summariser s = new Summariser("SUMMARY ONE", "SUMMARY TWO");
        List<Map<String, Object>> first = orEmpty(Context.compact(messages, 60, s, 1));
        List<Map<String, Object>> grown = new ArrayList<>(first);
        grown.addAll(List.of(said("noted"), user("fifth question " + rep("q", 200)), said("fifth answer " + rep("w", 200))));
        List<Map<String, Object>> second = orEmpty(Context.compact(grown, 60, s, 1));
        assertEquals(2, s.calls.size());
        List<Map<String, Object>> older = s.calls.size() > 1 ? s.calls.get(1) : List.of();
        assertTrue(blocksOf(older).stream().anyMatch(b -> String.valueOf(b.get("text")).startsWith("<summary>\nSUMMARY ONE")));
        List<Object> summaries = blocksOf(second).stream().filter(b -> "text".equals(b.get("type")) && ((String) b.get("text")).startsWith("<summary>")).map(b -> b.get("text")).collect(Collectors.toList());
        assertEquals(List.of("<summary>\nSUMMARY TWO\n</summary>"), summaries);
    }

    private static final List<Map<String, Object>> DOCS = List.of(map("title", "Policy", "text", "The grass is green. The sky is blue."), map("title", "Notes", "text", "Water is essential for life."));

    private static Map<String, Object> cite(int doc, int start, int end, String cited) {
        return map("type", "char_location", "cited_text", cited, "document_index", doc, "start_char_index", start, "end_char_index", end);
    }

    private static Map<String, Object> textBlock(String text, Map<String, Object>... citations) {
        Map<String, Object> b = map("type", "text", "text", text);
        if (citations.length > 0) b.put("citations", List.of(citations));
        return b;
    }

    @Test
    @SuppressWarnings("unchecked")
    void e5_aCitationThatDoesNotMatchItsDocumentIsReported() {
        Map<String, Object> good = textBlock("Grass is green.", cite(0, 0, 19, "The grass is green."));
        assertEquals(List.of(), orEmpty(Context.verifyCitations(List.of(good, textBlock("No source.")), DOCS)));
        Map<String, Object> page = map("type", "page_location", "cited_text", "The", "document_index", 0, "start_page_number", 1, "end_page_number", 2);
        Map<String, Object> block = map("type", "text", "text", "x", "citations", List.of(cite(0, 0, 19, "The grass is green."), cite(0, 0, 19, "The grass is red."), cite(5, 0, 3, "The"),
                cite(0, 20, 99, "The sky is blue."), cite(1, 3, 3, ""), page));
        List<Map<String, Object>> got = orEmpty(Context.verifyCitations(List.of(block), DOCS));
        assertEquals(List.of("0/1/text_mismatch", "0/2/unknown_document", "0/3/bad_range", "0/4/bad_range", "0/5/unsupported_type"),
                got.stream().map(p -> p.get("block") + "/" + p.get("citation") + "/" + p.get("problem")).collect(Collectors.toList()));
        assertEquals(List.of(), orEmpty(Context.verifyCitations(List.of(textBlock("x", cite(0, 20, 36, "The sky is blue."))), DOCS)));
    }

    @Test
    void e6_footnotesNumberEachDistinctSourceOnceInOrderOfAppearance() {
        List<Map<String, Object>> blocks = List.of(textBlock("Grass is green. ", cite(0, 0, 19, "The grass is green.")), textBlock("Water matters. ", cite(1, 0, 28, "Water is essential for life.")),
                textBlock("Again, green.", cite(0, 0, 19, "The grass is green.")), textBlock(" No source here."));
        assertEquals("Grass is green. [1]Water matters. [2]Again, green.[1] No source here.\n\nSources:\n[1] Policy: \"The grass is green.\"\n[2] Notes: \"Water is essential for life.\"", Context.footnotes(blocks, DOCS));
        assertEquals("Plain.", Context.footnotes(List.of(textBlock("Plain.")), DOCS));
    }
}
