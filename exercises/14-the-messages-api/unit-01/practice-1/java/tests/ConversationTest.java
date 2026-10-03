import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConversationTest {
    @SuppressWarnings("unchecked")
    private static Map<String, Object> answer(String text, String stop, long in, long out) {
        return (Map<String, Object>) Json.parse(String.format("""
            {"id":"msg_x","type":"message","role":"assistant","model":"claude-sonnet-5-5",
             "content":[{"type":"text","text":"%s"}],"stop_reason":"%s","stop_sequence":null,
             "usage":{"input_tokens":%d,"output_tokens":%d}}""", text, stop, in, out));
    }

    private static Map<String, Object> answer(String text) {
        return answer(text, "end_turn", 10, 5);
    }

    /** A scripted send: replies in order (a RuntimeException is thrown); keeps the bodies it was given, without copying. */
    private static final class Script implements Send {
        final List<Object> replies;
        final List<Map<String, Object>> bodies = new ArrayList<>();

        Script(Object... replies) {
            this.replies = new ArrayList<>(List.of(replies));
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> send(Map<String, Object> body) {
            bodies.add(body); // no copy: a client that shares its list with us shows it here
            Object item = replies.remove(0);
            if (item instanceof RuntimeException e) throw e;
            return (Map<String, Object>) item;
        }
    }

    private static RuntimeException errorOf(Runnable call) {
        try {
            call.run();
        } catch (RuntimeException e) {
            return e;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> messages(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("messages");
    }

    private static List<String> roles(List<Map<String, Object>> turns) {
        return turns.stream().map(m -> (String) m.get("role")).toList();
    }

    @Test
    void m1_everyRequestCarriesTheWholeHistoryInOrder() {
        Script send = new Script(answer("Paris."), answer("Since 987."), answer("The Seine."));
        Conversation chat = new Conversation(send, "claude-sonnet-5-5", 64, null, null);
        assertEquals("Paris.", chat.say("Capital of France?").text());
        chat.say("Since when?");
        chat.say("Its river?");
        assertEquals(List.of(1, 3, 5), send.bodies.stream().map(b -> messages(b).size()).toList());
        assertEquals(List.of("user", "assistant", "user", "assistant", "user"), roles(messages(send.bodies.get(2))));
        assertEquals(Json.parse("{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Paris.\"}]}"), messages(send.bodies.get(1)).get(1));
        assertTrue("claude-sonnet-5-5".equals(send.bodies.get(0).get("model")) && Long.valueOf(64).equals(send.bodies.get(0).get("max_tokens")));
    }

    @Test
    void e1_usageAddsUpOverTheTurns() {
        Conversation chat = new Conversation(new Script(answer("a", "end_turn", 12, 4), answer("b", "end_turn", 30, 9)), "m", 64, null, null);
        chat.say("one");
        chat.say("two");
        assertEquals(Map.of("input_tokens", 42L, "output_tokens", 13L), chat.totals());
    }

    @Test
    void e2_aFailedCallLeavesNoDanglingUserTurn() {
        Script send = new Script(answer("ok"), new IllegalStateException("overloaded"), answer("fine"));
        Conversation chat = new Conversation(send, "m", 64, null, null);
        chat.say("first");
        RuntimeException err = errorOf(() -> chat.say("second"));
        assertInstanceOf(IllegalStateException.class, err);
        assertEquals(List.of("user", "assistant"), roles(chat.history()));
        chat.say("second again");
        assertEquals(List.of("user", "assistant", "user"), roles(messages(send.bodies.get(2))));
    }

    @Test
    void e3_stopReasonIsReportedAndMaxTokensMarksTheReplyTruncated() {
        Conversation chat = new Conversation(new Script(answer("Complete."), answer("Cut o", "max_tokens", 10, 5),
            answer("done", "stop_sequence", 10, 5)), "m", 64, null, null);
        Reply first = chat.say("a");
        Reply second = chat.say("b");
        Reply third = chat.say("c");
        assertEquals(List.of("end_turn", "false"), List.of(first.stopReason(), String.valueOf(first.truncated())));
        assertEquals(List.of("max_tokens", "true", "Cut o"), List.of(second.stopReason(), String.valueOf(second.truncated()), second.text()));
        assertEquals(List.of("stop_sequence", "false"), List.of(third.stopReason(), String.valueOf(third.truncated())));
    }

    @Test
    void e4_systemIsATopLevelFieldAndStopSequencesArePassedOn() {
        Script send = new Script(answer("x"), answer("y"));
        new Conversation(send, "m", 8, "Be brief.", List.of("END")).say("hi");
        assertEquals(1, send.bodies.size());
        Map<String, Object> body = send.bodies.get(0);
        assertEquals("Be brief.", body.get("system"));
        assertEquals(List.of("END"), body.get("stop_sequences"));
        assertTrue(roles(messages(body)).stream().noneMatch("system"::equals));
        new Conversation(send, "m", 8, null, null).say("hi");
        assertEquals(2, send.bodies.size());
        assertFalse(send.bodies.get(1).containsKey("system") || send.bodies.get(1).containsKey("stop_sequences"));
    }

    @Test
    void e5_eachRequestIsASnapshotAndHistoryIsACopy() {
        Script send = new Script(answer("a"), answer("b"));
        Conversation chat = new Conversation(send, "m", 8, null, null);
        chat.say("one");
        chat.say("two");
        assertEquals(2, send.bodies.size());
        assertEquals(1, messages(send.bodies.get(0)).size()); // a later turn must not change an earlier request
        chat.history().add(Map.of("role", "user", "content", "injected"));
        chat.history().get(0).put("content", "changed");
        assertEquals(4, chat.history().size());
        assertEquals("one", chat.history().get(0).get("content"));
    }

    @Test
    void e6_aBlankTurnIsRefusedBeforeAnythingIsSent() {
        Script send = new Script(answer("never used"));
        Conversation chat = new Conversation(send, "m", 8, null, null);
        for (String blank : new String[] {"", "   ", "\n"}) {
            assertInstanceOf(IllegalArgumentException.class, errorOf(() -> chat.say(blank)), blank);
        }
        assertEquals(0, send.bodies.size());
        assertEquals(0, chat.history().size());
    }
}
