import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import harness.Scripted;
import harness.ScriptedHttp;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConversationTest {
    private static ScriptedHttp drive(List<Conversation.Turn>[] out) {
        ScriptedHttp transport = Scripted.http(Conversation.REPLIES.toArray());
        out[0] = Conversation.run(Scripted.clientOn(transport, 0), Conversation.QUESTIONS);
        return transport;
    }

    @SuppressWarnings("unchecked")
    private static ScriptedHttp drive() {
        return drive(new List[1]);
    }

    @Test
    void everyRequestCarriesTheWholeHistory() {
        ScriptedHttp transport = drive();
        assertEquals(List.of(1, 3, 5), transport.requests.stream().map(r -> r.get("messages").size()).toList());
        List<String> roles = new java.util.ArrayList<>();
        transport.requests.get(2).get("messages").forEach(m -> roles.add(m.get("role").asText()));
        assertEquals(List.of("user", "assistant", "user", "assistant", "user"), roles);
    }

    @Test
    void assistantTurnsAreSentBackAsReceived() {
        JsonNode assistant = drive().requests.get(1).get("messages").get(1);
        assertEquals("assistant", assistant.get("role").asText());
        assertEquals(1, assistant.get("content").size());
        assertEquals("text", assistant.get("content").get(0).get("type").asText());
        assertEquals("Paris.", assistant.get("content").get(0).get("text").asText());
    }

    @SuppressWarnings("unchecked")
    @Test
    void usageAddsUpAndStopReasonsAreRead() {
        List<Conversation.Turn>[] out = new List[1];
        drive(out);
        Conversation.Turn last = out[0].get(2);
        assertEquals(89, last.input());
        assertEquals(12, last.output());
        assertEquals(List.of("end_turn", "max_tokens", "stop_sequence"), out[0].stream().map(t -> t.reply().stopReason().get().asString()).toList());
    }

    @Test
    void systemIsTopLevelNeverAMessageRole() {
        ScriptedHttp transport = drive();
        assertTrue(transport.requests.stream().allMatch(r -> !r.path("system").asText().isEmpty()));
        transport.requests.forEach(r -> r.get("messages").forEach(m -> assertNotEquals("system", m.get("role").asText())));
    }
}
