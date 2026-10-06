import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ExtractorTest {
    private static final String DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business.";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static final Map<String, Object> SCHEMA = map("type", "object", "required", List.of("vendor", "total", "currency", "evidence"),
            "properties", map("vendor", map("type", "string"), "total", map("type", "number", "minimum", 0),
                    "currency", map("type", "string", "enum", List.of("USD", "EUR", "GBP")),
                    "items", map("type", "array", "items", map("type", "object", "required", List.of("name", "qty"),
                            "properties", map("name", map("type", "string"), "qty", map("type", "integer", "minimum", 1)))),
                    "evidence", map("type", "string")),
            "additionalProperties", false);
    private static final Map<String, Object> GOOD = map("vendor", "Acme Tools", "total", 120.5, "currency", "EUR", "evidence", "Total due: 120.50 EUR");

    private static Map<String, Object> with(Map<String, Object> base, Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>(base);
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> reply(String text, String stopReason) {
        return map("id", "msg_illustrative", "type", "message", "role", "assistant", "model", "claude-sonnet-5-5",
                "content", List.of(map("type", "text", "text", text)), "stop_reason", stopReason, "usage", map("input_tokens", 1, "output_tokens", 1));
    }

    private static Map<String, Object> reply(String text) {
        return reply(text, "end_turn");
    }

    /** A hand-written, illustrative model: returns the next reply and records the messages it was sent. */
    private static final class Scripted implements Function<List<Map<String, Object>>, Map<String, Object>> {
        final List<Map<String, Object>> replies = new ArrayList<>();
        final List<List<Map<String, Object>>> seen = new ArrayList<>();

        @SafeVarargs
        Scripted(Map<String, Object>... replies) {
            this.replies.addAll(List.of(replies));
        }

        @Override
        public Map<String, Object> apply(List<Map<String, Object>> messages) {
            seen.add(new ArrayList<>(messages));
            return replies.remove(0);
        }
    }

    /** The parsed value, or the string ParseError when the text holds no JSON object. */
    private static Object parsed(String text) {
        try {
            return Extractor.parseJson(text);
        } catch (ParseError e) {
            return "ParseError";
        }
    }

    private static List<Object> paths(List<Map<String, Object>> errors) {
        return errors == null ? List.of("no result") : errors.stream().map(e -> e.get("path")).collect(Collectors.toList());
    }

    private static Object at(Map<String, Object> result, String key) {
        return result == null ? "no result" : result.get(key);
    }

    private static String content(Scripted m, int call, int message) {
        return (String) m.seen.get(call).get(message).get("content");
    }

    @Test
    void m1_aValidReplyIsReturnedAfterOneCall() {
        Scripted m = new Scripted(reply(Json.stringify(GOOD)));
        Map<String, Object> r = Extractor.extract(m, DOC, SCHEMA, 3, List.of("evidence"));
        assertEquals(List.of("ok", GOOD, 1, List.of()), List.of(at(r, "status"), at(r, "value"), at(r, "attempts"), at(r, "errors")));
        assertEquals(1, m.seen.size());
        assertEquals("user", m.seen.get(0).get(0).get("role"));
        assertTrue(content(m, 0, 0).contains(DOC));
    }

    @Test
    void e1_jsonIsFoundInFencesAndProseAndBadTextIsRetried() {
        assertEquals(GOOD, parsed("Here you go:\n```json\n" + Json.stringify(GOOD) + "\n```\nHope that helps."));
        assertEquals(GOOD, parsed("Sure! " + Json.stringify(GOOD) + " Done."));
        for (String bad : new String[] {"I cannot find an invoice.", "{not json}", "```json\n```"}) {
            assertThrows(ParseError.class, () -> Extractor.parseJson(bad), bad);
        }
        Scripted m = new Scripted(reply("I think the total is 120.50"), reply(Json.stringify(GOOD)));
        Map<String, Object> r = Extractor.extract(m, DOC, SCHEMA);
        assertEquals(List.of("ok", 2), List.of(at(r, "status"), at(r, "attempts")));
        assertTrue(content(m, 1, 2).contains("$"));
    }

    @Test
    void e2_everySchemaViolationIsListedAndSentBackToTheModel() {
        Map<String, Object> bad = map("vendor", "Acme Tools", "total", "120.50", "currency", "usd", "extra", 1L, "evidence", "x");
        assertEquals(List.of("$.total", "$.currency", "$.extra"), paths(Extractor.validate(SCHEMA, bad)));
        assertEquals(List.of("$.vendor", "$.evidence", "$.total"), paths(Extractor.validate(SCHEMA, map("total", -1L, "currency", "EUR"))));
        Map<String, Object> items = with(GOOD, "items", List.of(map("name", "bolt", "qty", 2L), map("name", "nut", "qty", 0L), map("name", "gear")));
        assertEquals(List.of("$.items[1].qty", "$.items[2].qty"), paths(Extractor.validate(SCHEMA, items)));
        assertEquals(List.of(), Extractor.validate(SCHEMA, GOOD) == null ? List.of("no result") : Extractor.validate(SCHEMA, GOOD));
        String first = Json.stringify(bad);
        Scripted m = new Scripted(reply(first), reply(Json.stringify(GOOD)));
        Map<String, Object> r = Extractor.extract(m, DOC, SCHEMA);
        assertEquals(List.of("ok", 2), List.of(at(r, "status"), at(r, "attempts")));
        List<Map<String, Object>> again = m.seen.get(1);
        assertEquals(List.of("user", "assistant", "user"), again.stream().map(x -> x.get("role")).collect(Collectors.toList()));
        assertEquals(first, content(m, 1, 1));
        for (String p : new String[] {"$.total", "$.currency", "$.extra"}) assertTrue(content(m, 1, 2).contains(p), p);
    }

    @Test
    void e3_theNumberOfAttemptsIsBounded() {
        Map<String, Object> wrong = reply(Json.stringify(map("vendor", 1L)));
        Scripted m = new Scripted(wrong, wrong, wrong, wrong, wrong);
        Map<String, Object> r = Extractor.extract(m, DOC, SCHEMA, 3, List.of());
        assertEquals(List.of("failed", 3), List.of(at(r, "status"), at(r, "attempts")));
        assertNull(r == null ? "no result" : r.get("value"));
        assertEquals(3, m.seen.size());
        @SuppressWarnings("unchecked") List<Map<String, Object>> errors = r == null ? null : (List<Map<String, Object>>) r.get("errors");
        assertTrue(paths(errors).contains("$.vendor"));
        Scripted once = new Scripted(wrong, wrong, wrong);
        assertEquals("failed", at(Extractor.extract(once, DOC, SCHEMA, 1, List.of()), "status"));
        assertEquals(1, once.seen.size());
    }

    @Test
    void e4_aRefusalOrACutOffReplyIsNotRetried() {
        Scripted refusal = new Scripted(reply("I can't help with that.", "refusal"), reply(Json.stringify(GOOD)));
        Map<String, Object> r = Extractor.extract(refusal, DOC, SCHEMA);
        assertEquals(List.of("refused", 1), List.of(at(r, "status"), at(r, "attempts")));
        assertNull(r == null ? "no result" : r.get("value"));
        assertEquals(1, refusal.seen.size());
        Scripted cut = new Scripted(reply("{\"vendor\": \"Acme", "max_tokens"), reply(Json.stringify(GOOD)));
        r = Extractor.extract(cut, DOC, SCHEMA);
        assertEquals(List.of("truncated", 1), List.of(at(r, "status"), at(r, "attempts")));
        assertEquals(1, cut.seen.size());
    }

    @Test
    void e5_aQuoteThatIsNotInTheDocumentIsRejected() {
        Map<String, Object> invented = with(GOOD, "evidence", "Total due: 999.00 USD");
        Scripted m = new Scripted(reply(Json.stringify(invented)), reply(Json.stringify(GOOD)));
        Map<String, Object> r = Extractor.extract(m, DOC, SCHEMA, 3, List.of("evidence"));
        assertEquals(List.of("ok", 2), List.of(at(r, "status"), at(r, "attempts")));
        assertTrue(content(m, 1, 2).contains("$.evidence"));
        Scripted unchecked = new Scripted(reply(Json.stringify(invented)));
        assertEquals("ok", at(Extractor.extract(unchecked, DOC, SCHEMA), "status"));
    }

    @Test
    void e6_typesAreExactBooleansAreNotNumbersAndIntegersHaveNoFraction() {
        assertEquals(List.of("$"), paths(Extractor.validate(map("type", "integer"), true)));
        assertEquals(List.of("$"), paths(Extractor.validate(map("type", "number"), false)));
        assertEquals(List.of("$"), paths(Extractor.validate(map("type", "integer"), 2.5)));
        assertEquals(List.of(), Extractor.validate(map("type", "integer"), 2.0) == null ? List.of("no result") : Extractor.validate(map("type", "integer"), 2.0));
        assertEquals(List.of(), Extractor.validate(map("type", "number"), 3L) == null ? List.of("no result") : Extractor.validate(map("type", "number"), 3L));
        assertEquals(List.of("$"), paths(Extractor.validate(map("type", "string"), null)));
        assertEquals(List.of("$[1]"), paths(Extractor.validate(map("type", "array", "items", map("type", "boolean")), List.of(true, 1L))));
    }
}
