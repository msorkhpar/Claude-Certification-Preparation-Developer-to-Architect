import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.errors.SseException;
import com.anthropic.models.messages.Message;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StreamingTest {
    @Test
    void theTextStreamYieldsTheTextDeltasOnly() {
        assertEquals(List.of("Let me ", "check."), Streaming.readText(Streaming.clientFor(Streaming.EVENTS).client()).textPieces());
    }

    @Test
    void theFinalMessageAssemblesTextToolInputAndUsage() {
        Message finalMessage = Streaming.readText(Streaming.clientFor(Streaming.EVENTS).client()).message();
        assertEquals("tool_use", finalMessage.stopReason().get().asString());
        assertEquals("Let me check.", finalMessage.content().get(0).asText().text());
        assertEquals(Map.of("city", "Paris"), Streaming.toolInput(finalMessage.content().get(1)));
        assertEquals(52, finalMessage.usage().inputTokens());
        assertEquals(38, finalMessage.usage().outputTokens());
    }

    @Test
    void theRequestAsksForAStreamAndTheSdkDropsPingEvents() {
        harness.Scripted.Rig rig = Streaming.clientFor(Streaming.EVENTS);
        List<String> names = Streaming.rawEventNames(rig.client());
        assertTrue(rig.http().requests.get(0).get("stream").asBoolean());
        assertFalse(names.contains("ping"));
        assertEquals(13, names.size());
    }

    @Test
    void anErrorEventAfterA200Raises() {
        assertThrows(SseException.class, () -> Streaming.readText(Streaming.clientFor(Streaming.FAILING).client()));
    }
}
