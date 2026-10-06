import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextBuilderTest {
    private static <T> T got(T value) {
        assertNotNull(value, "the method returned nothing");
        return value;
    }

    private static Map<String, String> order() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("order_id", "A-1042");
        m.put("purchase_date", "2026-09-02");
        m.put("items", "2 x kettle");
        m.put("return_window", "30 days");
        m.put("warehouse_bin", "R7-22");
        m.put("carrier_hash", "9f3c");
        m.put("refund_amount", "$129.50");
        return m;
    }

    private static ContextBuilder.FactEntry fact(String customer, String name, String value) {
        return new ContextBuilder.FactEntry(customer, name, value, "2026-09-02");
    }

    private static ContextBuilder.Message msg(String role, String kind, String id, String text) {
        return new ContextBuilder.Message(role, kind, id, text);
    }

    @Test
    void m1_trimmingKeepsOnlyTheNamedFieldsWithTheirExactValuesInTheNamedOrder() {
        assertEquals(List.of("refund_amount=$129.50", "order_id=A-1042"), got(ContextBuilder.trimRecord(order(), List.of("refund_amount", "order_id"))).entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList());
        assertFalse(got(ContextBuilder.trimRecord(order(), List.of("order_id", "items"))).containsKey("warehouse_bin"));
    }

    @Test
    void e1_aFieldThatTheRecordDoesNotHaveIsSkipped() {
        assertEquals(Map.of("order_id", "A-1042"), ContextBuilder.trimRecord(order(), List.of("order_id", "tracking_url")));
        assertEquals(Map.of(), ContextBuilder.trimRecord(Map.of(), List.of("order_id")));
    }

    @Test
    void e2_aNewerFactReplacesTheOldOneAndTheOldValueIsKeptAsHistory() {
        Map<String, ContextBuilder.Fact> start = got(ContextBuilder.updateFacts(Map.of(), "address", "12 Oak St", "2026-08-01"));
        assertEquals(Map.of("address", new ContextBuilder.Fact("12 Oak St", "2026-08-01", List.of())), start);
        Map<String, ContextBuilder.Fact> later = got(ContextBuilder.updateFacts(start, "address", "9 Elm Rd", "2026-09-10"));
        assertEquals(new ContextBuilder.Fact("9 Elm Rd", "2026-09-10", List.of("12 Oak St@2026-08-01")), later.get("address"));
        assertEquals(new ContextBuilder.Fact("12 Oak St", "2026-08-01", List.of()), start.get("address"));
        assertEquals(new ContextBuilder.Fact("9 Elm Rd", "2026-08-01", List.of("12 Oak St@2026-08-01")), got(ContextBuilder.updateFacts(start, "address", "9 Elm Rd", "2026-08-01")).get("address"));
    }

    @Test
    void e3_anOlderFactThatArrivesLateDoesNotReplaceTheCurrentOne() {
        Map<String, ContextBuilder.Fact> current = got(ContextBuilder.updateFacts(Map.of(), "address", "9 Elm Rd", "2026-09-10"));
        Map<String, ContextBuilder.Fact> after = got(ContextBuilder.updateFacts(current, "address", "12 Oak St", "2026-08-01"));
        assertEquals(new ContextBuilder.Fact("9 Elm Rd", "2026-09-10", List.of("12 Oak St@2026-08-01")), after.get("address"));
    }

    @Test
    void e4_theCaseFactsOfAnotherCustomerNeverEnterTheContext() {
        String text = got(ContextBuilder.buildContext("c1", List.of(fact("c1", "refund", "$129.50"), fact("c2", "refund", "$20.00")), "summary", List.of()));
        assertTrue(text.contains("$129.50") && !text.contains("$20.00"));
        assertFalse(got(ContextBuilder.buildContext("c3", List.of(fact("c1", "refund", "$129.50")), "summary", List.of())).contains("## Case facts"));
    }

    @Test
    void e5_theContextPutsCaseFactsFirstThenTheSummaryThenTheRecentMessages() {
        String text = ContextBuilder.buildContext("c1", List.of(fact("c1", "refund", "$129.50"), fact("c1", "order", "A-1042")), "They want the money back.", List.of(msg("user", "text", "", "Any news?")));
        assertEquals("## Case facts\nrefund: $129.50 (as of 2026-09-02)\norder: A-1042 (as of 2026-09-02)\n\n## Summary so far\nThey want the money back.\n\n## Recent messages\nuser: Any news?", text);
    }

    @Test
    void e6_aSummaryThatLosesAnExactValueIsReported() {
        List<ContextBuilder.FactEntry> facts = List.of(fact("c1", "refund", "$129.50"), fact("c1", "deadline", "2026-09-30"), fact("c1", "order", "A-1042"));
        assertEquals(List.of("refund", "deadline"), ContextBuilder.missingFromSummary("Refund of about $130 for order A-1042, due end of month.", facts));
        assertEquals(List.of(), ContextBuilder.missingFromSummary("$129.50, 2026-09-30, A-1042", facts));
    }

    @Test
    void e7_theWindowDropsTheOldestMessagesAndKeepsAToolCallWithItsResult() {
        List<ContextBuilder.Message> messages = List.of(msg("user", "text", "", "x".repeat(40)), msg("assistant", "tool_use", "t1", "y".repeat(40)), msg("user", "tool_result", "t1", "z".repeat(40)), msg("assistant", "text", "", "done....."));
        assertEquals(List.of("d"), got(ContextBuilder.window(messages, 14)).stream().map(m -> m.text().substring(0, 1)).toList());
        assertEquals(List.of("tool_use", "tool_result", "text"), got(ContextBuilder.window(messages, 23)).stream().map(ContextBuilder.Message::kind).toList());
        assertEquals(4, got(ContextBuilder.window(messages, 1000)).size());
        assertEquals(List.of(), got(ContextBuilder.window(messages, 0)));
    }
}
