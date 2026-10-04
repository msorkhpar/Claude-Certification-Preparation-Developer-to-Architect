import static harness.Scripted.message;
import static harness.Scripted.text;
import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.models.messages.Message;
import harness.Scripted;
import java.util.List;
import org.junit.jupiter.api.Test;

class ThinkingTest {
    @Test
    void theRequestCarriesAdaptiveThinkingAndEffortInOutputConfig() {
        Scripted.Rig rig = Scripted.client(message(List.of(text("x"))));
        Thinking.request(rig.client(), "medium");
        var sent = rig.http().requests.get(0);
        assertEquals("adaptive", sent.at("/thinking/type").asText());
        assertEquals(1, sent.get("thinking").size());
        assertEquals("medium", sent.at("/output_config/effort").asText());
        assertFalse(sent.toString().contains("budget_tokens"));
        assertFalse(sent.has("temperature"));
    }

    @Test
    void aThinkingBlockCanBeEmptyAndBilledTokensExceedVisibleOnes() {
        Scripted.Rig rig = Scripted.client(message(List.of(Thinking.THINKING_BLOCK, text("A")), "end_turn", Thinking.MODEL, Thinking.USAGE, null));
        Message reply = Thinking.request(rig.client(), "high");
        assertTrue(reply.content().get(0).isThinking());
        assertEquals("", reply.content().get(0).asThinking().thinking());
        assertEquals(1650, Thinking.thinkingTokens(reply.usage()));
        assertEquals(1900, reply.usage().outputTokens());
    }

    @Test
    void aTurnMayHaveNoThinkingBlockAtAll() {
        Message reply = Thinking.request(Scripted.client(message(List.of(text("B.")))).client(), "low");
        assertEquals(1, reply.content().size());
        assertTrue(reply.content().get(0).isText());
    }
}
