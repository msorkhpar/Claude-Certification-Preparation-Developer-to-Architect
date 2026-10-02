import static org.junit.jupiter.api.Assertions.*;
import static java.util.Map.of;

import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class AgentTest {
    static final Map<String, Function<Map<String, Object>, Object>> TOOLS =
        Map.of("add", in -> String.valueOf((int) in.get("a") + (int) in.get("b")));

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> content(Map<String, Object> m) { return (List<Map<String, Object>>) m.get("content"); }

    @Test void twoTurnsReturnsFinalText() {
        var m = new Scripted.Model(
            Scripted.reply(List.of(Scripted.text("Adding."), Scripted.toolUse("tu_1", "add", of("a", 2, "b", 3))), "tool_use"),
            Scripted.reply(List.of(Scripted.text("The sum is 5.")), "end_turn"));
        assertEquals("The sum is 5.", Agent.run(m, TOOLS, "2+3?", 5));
        assertEquals(2, m.seen.size());
    }

    @Test void toolResultsAreOneUserTurn() {
        var m = new Scripted.Model(
            Scripted.reply(List.of(Scripted.toolUse("tu_1", "add", of("a", 2, "b", 3)),
                                   Scripted.toolUse("tu_2", "add", of("a", 1, "b", 1))), "tool_use"),
            Scripted.reply(List.of(Scripted.text("done")), "end_turn"));
        Agent.run(m, TOOLS, "go", 5);
        var second = m.seen.get(1);
        assertEquals(List.of("user", "assistant", "user"), second.stream().map(x -> x.get("role")).toList());
        var results = content(second.get(2));
        assertEquals(List.of("tool_result:tu_1:5", "tool_result:tu_2:2"),
            results.stream().map(r -> r.get("type") + ":" + r.get("tool_use_id") + ":" + r.get("content")).toList());
    }

    @Test void failingAndUnknownToolsBecomeErrorResults() {
        Map<String, Function<Map<String, Object>, Object>> tools = Map.of("boom", in -> { throw new IllegalStateException("bad input"); });
        var m = new Scripted.Model(
            Scripted.reply(List.of(Scripted.toolUse("tu_1", "boom", of()), Scripted.toolUse("tu_2", "nope", of())), "tool_use"),
            Scripted.reply(List.of(Scripted.text("recovered")), "end_turn"));
        assertEquals("recovered", Agent.run(m, tools, "go", 5));
        var res = content(m.seen.get(1).get(2));
        assertEquals(true, res.get(0).get("is_error"));
        assertTrue(res.get(0).get("content").toString().contains("bad input"));
        assertEquals(true, res.get(1).get("is_error"));
        assertTrue(res.get(1).get("content").toString().contains("nope"));
    }

    @Test void noToolUseReturnsImmediately() {
        var m = new Scripted.Model(Scripted.reply(List.of(Scripted.text("hi")), "end_turn"));
        assertEquals("hi", Agent.run(m, TOOLS, "hello", 5));
        assertEquals(1, m.seen.size());
    }

    @Test void turnCapThrows() {
        var loop = new Scripted.Model[0];
        var replies = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < 10; i++) replies.add(Scripted.reply(List.of(Scripted.toolUse("tu_" + i, "add", of("a", 1, "b", 1))), "tool_use"));
        var m = new Scripted.Model(replies.toArray(new Map[0]));
        assertThrows(IllegalStateException.class, () -> Agent.run(m, TOOLS, "go", 3));
    }

    @Test void textInToolUseTurnDoesNotEndTheLoop() {
        var m = new Scripted.Model(
            Scripted.reply(List.of(Scripted.text("I will call a tool."), Scripted.toolUse("tu_1", "add", of("a", 1, "b", 2))), "tool_use"),
            Scripted.reply(List.of(Scripted.text("3")), "end_turn"));
        assertEquals("3", Agent.run(m, TOOLS, "go", 5));
    }
}
