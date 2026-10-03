import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class AgentLoopTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> text(String t) {
        return map("type", "text", "text", t);
    }

    private static Map<String, Object> call(String id, String name, Object... input) {
        return map("type", "tool_use", "id", id, "name", name, "input", map(input));
    }

    private static Map<String, Object> reply(String stopReason, Map<String, Object>... content) {
        return map("stop_reason", stopReason, "content", new ArrayList<>(Arrays.asList(content)));
    }

    private static Map<String, Object> result(String id, String content) {
        return map("type", "tool_result", "tool_use_id", id, "content", content);
    }

    private static Map<String, Object> errorResult(String id, String content) {
        return map("type", "tool_result", "tool_use_id", id, "content", content, "is_error", true);
    }

    /** A scripted model: replies in order, or the last one again when it repeats; it keeps a snapshot of every request. */
    private static final class ScriptedModel implements Function<List<Map<String, Object>>, Map<String, Object>> {
        final List<Map<String, Object>> replies;
        final boolean repeat;
        final List<List<Map<String, Object>>> seen = new ArrayList<>();

        @SafeVarargs
        ScriptedModel(boolean repeat, Map<String, Object>... replies) {
            this.repeat = repeat;
            this.replies = new ArrayList<>(Arrays.asList(replies));
        }

        public Map<String, Object> apply(List<Map<String, Object>> messages) {
            seen.add(new ArrayList<>(messages));
            if (replies.size() > 1 || !repeat) {
                if (replies.isEmpty()) return fail("the loop called the model again after the script ended");
                return replies.remove(0);
            }
            return replies.get(0);
        }
    }

    /** Handlers that record every call. */
    private static final class RecordingTools extends LinkedHashMap<String, Function<Map<String, Object>, String>> {
        final List<String> calls = new ArrayList<>();

        RecordingTools with(String name, Function<Map<String, Object>, String> fn) {
            put(name, input -> {
                calls.add(name + " " + input);
                return fn.apply(input);
            });
            return this;
        }
    }

    private static final Function<Map<String, Object>, String> LOOKUP = input -> "record " + input.get("n");

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> messages(Map<String, Object> r) {
        assertNotNull(r, "runAgent returned null");
        return (List<Map<String, Object>>) r.get("messages");
    }

    private static List<Object> roles(Map<String, Object> r) {
        return messages(r).stream().map(m -> m.get("role")).toList();
    }

    @SuppressWarnings("unchecked")
    @Test
    void m1_aRunAlternatesModelAndToolsUntilTheModelEndsItsTurn() {
        ScriptedModel model = new ScriptedModel(false, reply("tool_use", text("Looking."), call("t1", "lookup", "n", 1)), reply("end_turn", text("Record 1 found.")));
        RecordingTools tools = new RecordingTools().with("lookup", LOOKUP);
        Map<String, Object> r = AgentLoop.runAgent(model, tools, "find record 1");
        assertNotNull(r, "runAgent returned null");
        assertEquals(List.of("done", "Record 1 found.", 2), List.of(r.get("status"), r.get("text"), r.get("turns")));
        assertEquals(List.of("user", "assistant", "user", "assistant"), roles(r));
        assertEquals(map("role", "user", "content", "find record 1"), messages(r).get(0));
        assertEquals(List.of(result("t1", "record 1")), messages(r).get(2).get("content"));
        assertEquals(List.of(1, 3), model.seen.stream().map(List::size).toList());
        assertEquals(List.of("lookup {n=1}"), tools.calls);
    }

    @Test
    void e1_theStopReasonDecidesAndTheWordsOfTheTextDoNot() {
        ScriptedModel model = new ScriptedModel(false, reply("tool_use", text("All done. Saving now."), call("t1", "lookup", "n", 7)), reply("end_turn", text("Saved.")));
        RecordingTools tools = new RecordingTools().with("lookup", LOOKUP);
        Map<String, Object> r = AgentLoop.runAgent(model, tools, "go");
        assertNotNull(r, "runAgent returned null");
        assertEquals(List.of("done", 2), List.of(r.get("status"), r.get("turns")));
        assertEquals(List.of("lookup {n=7}"), tools.calls);
        ScriptedModel announce = new ScriptedModel(false, reply("end_turn", text("Next I will call the lookup tool.")));
        RecordingTools again = new RecordingTools().with("lookup", LOOKUP);
        Map<String, Object> ended = AgentLoop.runAgent(announce, again, "go");
        assertNotNull(ended, "runAgent returned null");
        assertEquals(List.of("done", 1, 0, 1), List.of(ended.get("status"), ended.get("turns"), again.calls.size(), announce.seen.size()));
    }

    @Test
    void e2_everyCallOfATurnIsAnsweredInOneUserMessageInOrder() {
        Map<String, Object> first = reply("tool_use", text("Three at once."), call("a", "lookup", "n", 1), call("b", "lookup", "n", 2), call("c", "lookup", "n", 3));
        Map<String, Object> r = AgentLoop.runAgent(new ScriptedModel(false, first, reply("end_turn", text("ok"))), new RecordingTools().with("lookup", LOOKUP), "go");
        assertEquals(List.of("user", "assistant", "user", "assistant"), roles(r));
        assertEquals(first.get("content"), messages(r).get(1).get("content"));
        assertEquals(List.of(result("a", "record 1"), result("b", "record 2"), result("c", "record 3")), messages(r).get(2).get("content"));
    }

    @Test
    void e3_aFailingOrUnknownToolBecomesAnErrorResultAndTheRunGoesOn() {
        ScriptedModel model = new ScriptedModel(false, reply("tool_use", call("a", "broken"), call("b", "missing"), call("c", "lookup", "n", 5)), reply("end_turn", text("Partly done.")));
        RecordingTools tools = new RecordingTools().with("broken", input -> {
            throw new IllegalStateException("database offline");
        }).with("lookup", LOOKUP);
        Map<String, Object> r = AgentLoop.runAgent(model, tools, "go");
        assertNotNull(r, "runAgent returned null");
        assertEquals("done", r.get("status"));
        assertEquals(List.of(errorResult("a", "database offline"), errorResult("b", "Unknown tool: missing"), result("c", "record 5")), messages(r).get(2).get("content"));
        assertEquals(List.of("broken {}", "lookup {n=5}"), tools.calls);
    }

    @Test
    void e4_theTurnLimitIsABackstopThatEndsOnlyARunTheModelHasNotEnded() {
        ScriptedModel endless = new ScriptedModel(true, reply("tool_use", call("t", "lookup", "n", 1)));
        RecordingTools tools = new RecordingTools().with("lookup", LOOKUP);
        Map<String, Object> r = AgentLoop.runAgent(endless, tools, "go", 3);
        assertNotNull(r, "runAgent returned null");
        assertEquals(List.of("max_turns", 3, 3, 3), List.of(r.get("status"), r.get("turns"), endless.seen.size(), tools.calls.size()));
        assertEquals("user", messages(r).get(messages(r).size() - 1).get("role"));
        assertEquals(7, messages(r).size());
        ScriptedModel last = new ScriptedModel(false, reply("tool_use", call("a", "lookup", "n", 1)), reply("tool_use", call("b", "lookup", "n", 2)), reply("end_turn", text("Finished on the last turn.")));
        Map<String, Object> done = AgentLoop.runAgent(last, new RecordingTools().with("lookup", LOOKUP), "go", 3);
        assertNotNull(done, "runAgent returned null");
        assertEquals(List.of("done", 3, "Finished on the last turn."), List.of(done.get("status"), done.get("turns"), done.get("text")));
    }

    @Test
    void e5_aCutOffOrRefusedReplyEndsTheRunWithItsOwnStatus() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("max_tokens", "truncated");
        expected.put("refusal", "refused");
        expected.put("stop_sequence", "done");
        expected.put("some_new_reason", "unexpected");
        for (Map.Entry<String, String> e : expected.entrySet()) {
            ScriptedModel model = new ScriptedModel(false, reply(e.getKey(), text("partial words")));
            Map<String, Object> r = AgentLoop.runAgent(model, new RecordingTools().with("lookup", LOOKUP), "go");
            assertNotNull(r, "runAgent returned null");
            assertEquals(List.of(e.getValue(), "partial words", 1, 1), List.of(r.get("status"), r.get("text"), r.get("turns"), model.seen.size()), e.getKey());
        }
    }

    @Test
    void e6_aToolUseReplyWithoutAToolCallIsMalformedAndSendsNothingMore() {
        ScriptedModel model = new ScriptedModel(false, reply("tool_use", text("I will call a tool.")));
        Map<String, Object> r = AgentLoop.runAgent(model, new RecordingTools().with("lookup", LOOKUP), "go");
        assertNotNull(r, "runAgent returned null");
        assertEquals(List.of("malformed", 1, 1, 2), List.of(r.get("status"), r.get("turns"), model.seen.size(), messages(r).size()));
    }
}
