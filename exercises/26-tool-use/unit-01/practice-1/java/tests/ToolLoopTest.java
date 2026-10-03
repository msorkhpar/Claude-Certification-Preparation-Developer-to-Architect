import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ToolLoopTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> text(String t) {
        return map("type", "text", "text", t);
    }

    private static Map<String, Object> toolUse(String id, String name, Object... input) {
        return map("type", "tool_use", "id", id, "name", name, "input", map(input));
    }

    private static Map<String, Object> reply(List<Map<String, Object>> content, String stopReason) {
        return map("id", "msg_illustrative", "type", "message", "role", "assistant", "model", "claude-sonnet-5-5", "content", content, "stop_reason", stopReason,
                "usage", map("input_tokens", 1, "output_tokens", 1));
    }

    private static Map<String, Object> reply(List<Map<String, Object>> content) {
        return reply(content, "end_turn");
    }

    /** A hand-written, illustrative model: returns the next reply and records a copy of each request. */
    private static final class Scripted implements Function<Map<String, Object>, Map<String, Object>> {
        final List<Map<String, Object>> replies = new ArrayList<>();
        final List<Map<String, Object>> seen = new ArrayList<>();

        @SafeVarargs
        Scripted(Map<String, Object>... replies) {
            this.replies.addAll(List.of(replies));
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> apply(Map<String, Object> request) {
            seen.add((Map<String, Object>) Json.parse(Json.stringify(request)));
            return replies.remove(0);
        }
    }

    private static final List<String> CALLS = new ArrayList<>();
    private static final Map<String, Object> SCHEMA = map("type", "object", "properties", map("city", map("type", "string")), "required", List.of("city"));

    private static List<Tool> tools() {
        return List.of(
                new Tool("get_weather", "Current weather for a city.", SCHEMA, a -> {
                    CALLS.add("get_weather " + Json.stringify(a));
                    return a.get("city") + ": 18 C";
                }),
                new Tool("get_time", "Local time for a city.", SCHEMA, a -> {
                    CALLS.add("get_time " + Json.stringify(a));
                    return a.get("city") + ": 14:05";
                }),
                new Tool("broken", "Always fails.", SCHEMA, a -> {
                    throw new ToolError("the weather service is down");
                }),
                new Tool("structured", "Returns an object.", SCHEMA, a -> map("city", a.get("city"), "temp_c", 18L, "tags", List.of("mild"))));
    }

    /** The RequestError field the call throws, "crash" for another exception, null when it returns. */
    private static String failureOf(Supplier<Object> call) {
        try {
            call.get();
        } catch (RequestError e) {
            return e.field();
        } catch (RuntimeException e) {
            return "crash";
        }
        return null;
    }

    /** The parsed JSON, or a marker string when the text is not JSON. */
    private static Object jsonOrText(String text) {
        try {
            return Json.parse(text);
        } catch (IllegalArgumentException e) {
            return "not JSON: " + text;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> messagesOf(Scripted m, int call) {
        return call < m.seen.size() ? (List<Map<String, Object>>) m.seen.get(call).get("messages") : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> contentOf(List<Map<String, Object>> messages, int index) {
        return index < messages.size() ? (List<Map<String, Object>>) messages.get(index).get("content") : List.of();
    }

    private static Object at(Map<String, Object> result, String key) {
        return result == null ? "no result" : result.get(key);
    }

    private static List<Object> roles(List<Map<String, Object>> messages) {
        return messages.stream().map(x -> x.get("role")).collect(Collectors.toList());
    }

    private static Map<String, Object> run(Scripted m, String model, int maxTurns, Map<String, Object> choice) {
        return ToolLoop.runAgent(m, tools(), "go", model, maxTurns, choice);
    }

    @Test
    void m1_aToolCallIsRunAndItsResultSentBackUntilTheModelEndsItsTurn() {
        CALLS.clear();
        List<Map<String, Object>> first = List.of(text("Let me check."), toolUse("tu_1", "get_weather", "city", "Oslo"));
        Scripted m = new Scripted(reply(first, "tool_use"), reply(List.of(text("It is 18 C in Oslo."))));
        Map<String, Object> r = ToolLoop.runAgent(m, tools(), "Weather in Oslo?");
        assertEquals(List.of("done", "It is 18 C in Oslo.", 2), List.of(at(r, "status"), at(r, "text"), at(r, "turns")));
        assertEquals(List.of("get_weather {\"city\":\"Oslo\"}"), CALLS);
        List<Map<String, Object>> second = messagesOf(m, 1);
        assertEquals(List.of("user", "assistant", "user"), roles(second));
        assertEquals(first, contentOf(second, 1));
        assertEquals(List.of(map("type", "tool_result", "tool_use_id", "tu_1", "content", "Oslo: 18 C")), contentOf(second, 2));
        Map<String, Object> sent = m.seen.get(0);
        assertEquals("claude-sonnet-5-5", sent.get("model"));
        @SuppressWarnings("unchecked") List<Map<String, Object>> defs = (List<Map<String, Object>>) sent.get("tools");
        assertEquals(List.of("get_weather", "get_time", "broken", "structured"), defs.stream().map(t -> t.get("name")).collect(Collectors.toList()));
        assertTrue(defs.stream().allMatch(t -> !t.containsKey("handler")) && !sent.containsKey("tool_choice"));
    }

    @Test
    void e1_parallelCallsGetOneUserMessageWithEveryResultInOrder() {
        CALLS.clear();
        List<Map<String, Object>> content = List.of(text("Checking both."), map("type", "server_tool_use", "id", "srvtoolu_1", "name", "web_search", "input", map("query", "x")),
                toolUse("tu_a", "get_time", "city", "Rome"), toolUse("tu_b", "get_weather", "city", "Rome"));
        Scripted m = new Scripted(reply(content, "tool_use"), reply(List.of(text("done"))));
        ToolLoop.runAgent(m, tools(), "Rome?");
        List<Map<String, Object>> second = messagesOf(m, 1);
        assertEquals(List.of("user", "assistant", "user"), roles(second));
        assertEquals(content, contentOf(second, 1));
        assertEquals(List.of("tool_result/tu_a/Rome: 14:05", "tool_result/tu_b/Rome: 18 C"),
                contentOf(second, 2).stream().map(r -> r.get("type") + "/" + r.get("tool_use_id") + "/" + r.get("content")).collect(Collectors.toList()));
        assertEquals(List.of("get_time {\"city\":\"Rome\"}", "get_weather {\"city\":\"Rome\"}"), CALLS);
    }

    @Test
    void e2_aFailingUnknownOrMalformedCallBecomesAnErrorResultAndTheLoopGoesOn() {
        CALLS.clear();
        List<Map<String, Object>> calls = List.of(toolUse("t1", "broken", "city", "Oslo"), toolUse("t2", "teleport", "city", "Oslo"), toolUse("t3", "get_weather"),
                toolUse("t4", "get_time", "city", "Oslo"));
        Scripted m = new Scripted(reply(calls, "tool_use"), reply(List.of(text("recovered"))));
        Map<String, Object> r = ToolLoop.runAgent(m, tools(), "go");
        assertEquals(List.of("done", "recovered"), List.of(at(r, "status"), at(r, "text")));
        List<Map<String, Object>> results = contentOf(messagesOf(m, 1), 2);
        assertEquals(List.of("t1/true", "t2/true", "t3/true", "t4/false"),
                results.stream().map(x -> x.get("tool_use_id") + "/" + x.getOrDefault("is_error", false)).collect(Collectors.toList()));
        assertTrue(String.valueOf(results.get(0).get("content")).contains("weather service is down"));
        assertTrue(String.valueOf(results.get(1).get("content")).contains("teleport") && String.valueOf(results.get(2).get("content")).contains("city"));
        assertEquals(List.of("get_time {\"city\":\"Oslo\"}"), CALLS);
    }

    @Test
    void e3_theNumberOfTurnsIsBounded() {
        List<Map<String, Object>> loop = new ArrayList<>();
        for (int i = 0; i < 10; i++) loop.add(reply(List.of(toolUse("tu_" + i, "get_time", "city", "Oslo")), "tool_use"));
        @SuppressWarnings("unchecked") Scripted m = new Scripted(loop.toArray(new Map[0]));
        Map<String, Object> r = run(m, "claude-sonnet-5-5", 3, null);
        assertEquals(List.of("max_turns", 3), List.of(at(r, "status"), at(r, "turns")));
        assertEquals(3, m.seen.size());
    }

    @Test
    void e4_refusalAndTruncationEndTheLoopAndAPausedTurnContinues() {
        Map<String, Object> refused = run(new Scripted(reply(List.of(text("I can't help.")), "refusal")), "claude-sonnet-5-5", 8, null);
        assertEquals(List.of("refused", 1), List.of(at(refused, "status"), at(refused, "turns")));
        Map<String, Object> cut = run(new Scripted(reply(List.of(text("The answer is")), "max_tokens")), "claude-sonnet-5-5", 8, null);
        assertEquals(List.of("truncated", "The answer is"), List.of(at(cut, "status"), at(cut, "text")));
        List<Map<String, Object>> paused = List.of(map("type", "server_tool_use", "id", "srvtoolu_1", "name", "web_search", "input", map("query", "x")));
        Scripted m = new Scripted(reply(paused, "pause_turn"), reply(List.of(text("found it"))));
        Map<String, Object> r = run(m, "claude-sonnet-5-5", 8, null);
        assertEquals(List.of("done", "found it", 2), List.of(at(r, "status"), at(r, "text"), at(r, "turns")));
        List<Map<String, Object>> again = messagesOf(m, 1);
        assertEquals(List.of("user", "assistant"), roles(again));
        assertEquals(paused, contentOf(again, 1));
    }

    @Test
    void e5_toolChoiceIsValidatedForTheModelAndAForcedChoiceAppliesToTheFirstRequestOnly() {
        for (Map<String, Object> choice : List.of(map("type", "any"), map("type", "tool", "name", "get_time"))) {
            for (String modelId : new String[] {"claude-sonnet-5-5", "claude-opus-5-5", "claude-fable-5-1"}) {
                assertEquals("tool_choice", failureOf(() -> run(new Scripted(reply(List.of(text("x")))), modelId, 8, choice)), choice + " " + modelId);
            }
        }
        assertEquals("tool_choice.type", failureOf(() -> run(new Scripted(reply(List.of(text("x")))), "claude-sonnet-5-5", 8, map("type", "maybe"))));
        assertEquals("tool_choice.name", failureOf(() -> run(new Scripted(reply(List.of(text("x")))), "claude-opus-5", 8, map("type", "tool", "name", "nope"))));
        assertEquals("tool_choice.disable_parallel_tool_use",
                failureOf(() -> run(new Scripted(reply(List.of(text("x")))), "claude-sonnet-5-5", 8, map("type", "auto", "disable_parallel_tool_use", "yes"))));
        Scripted m = new Scripted(reply(List.of(toolUse("tu_1", "get_time", "city", "Oslo")), "tool_use"), reply(List.of(text("ok"))));
        Map<String, Object> r = run(m, "claude-opus-5", 8, map("type", "tool", "name", "get_time"));
        assertEquals("done", at(r, "status"));
        assertEquals(List.of(map("type", "tool", "name", "get_time"), map("type", "auto")), m.seen.stream().map(x -> x.get("tool_choice")).collect(Collectors.toList()));
        Scripted auto = new Scripted(reply(List.of(toolUse("tu_1", "get_time", "city", "Oslo")), "tool_use"), reply(List.of(text("ok"))));
        run(auto, "claude-sonnet-5-5", 8, map("type", "auto", "disable_parallel_tool_use", true));
        Map<String, Object> kept = map("type", "auto", "disable_parallel_tool_use", true);
        assertEquals(List.of(kept, kept), auto.seen.stream().map(x -> x.get("tool_choice")).collect(Collectors.toList()));
        Scripted none = new Scripted(reply(List.of(text("ok"))));
        run(none, "claude-sonnet-5-5", 8, map("type", "none"));
        assertEquals(map("type", "none"), none.seen.get(0).get("tool_choice"));
    }

    @Test
    void e6_resultsThatAreNotTextAreSentAsJsonText() {
        Scripted m = new Scripted(reply(List.of(toolUse("tu_1", "structured", "city", "Oslo")), "tool_use"), reply(List.of(text("ok"))));
        ToolLoop.runAgent(m, tools(), "go");
        List<Map<String, Object>> results = contentOf(messagesOf(m, 1), 2);
        Object content = results.isEmpty() ? null : results.get(0).get("content");
        assertTrue(content instanceof String);
        assertEquals(map("city", "Oslo", "temp_c", 18L, "tags", List.of("mild")), jsonOrText((String) content));
    }
}
