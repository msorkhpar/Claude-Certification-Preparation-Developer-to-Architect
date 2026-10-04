import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Scripted.toolUse;
import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ToolChoice;
import com.anthropic.models.messages.ToolChoiceTool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.anthropic.core.JsonValue;
import harness.Scripted;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolLoopTest {
    private static ToolUseBlock block(String id, String name, String city) {
        return com.anthropic.core.ObjectMappers.jsonMapper().convertValue(map("type", "tool_use", "id", id, "name", name, "input", map("city", city)), ToolUseBlock.class);
    }

    @Test
    void aFailingCallBecomesAnErrorResultWithAHint() {
        ToolResultBlockParam result = ToolLoop.runTool(block("t1", "get_weather", "Atlantis"));
        assertEquals(true, result.isError().get());
        assertTrue(result.content().get().string().get().contains("Known cities"));
    }

    @Test
    void allResultsGoBackInOneUserMessageInCallOrder() {
        Scripted.Rig rig = Scripted.client(ToolLoop.REPLIES.toArray());
        ToolLoop.Loop run = ToolLoop.loop(rig.client(), "q", null);
        assertEquals(List.of("user", "assistant", "user", "assistant"), run.messages().stream().map(m -> m.role().asString()).toList());
        List<ContentBlockParam> results = run.messages().get(2).content().blockParams().get();
        assertEquals(List.of("toolu_01", "toolu_02", "toolu_03"), results.stream().map(r -> r.asToolResult().toolUseId()).toList());
        assertEquals("end_turn", run.reply().stopReason().get().asString());
        assertEquals(2, rig.http().requests.size());
    }

    @Test
    void aForcedChoiceIsSentOnce() {
        Scripted.Rig rig = Scripted.client(message(List.of(toolUse("toolu_01", "get_time", map("city", "Oslo"))), "tool_use"), message(List.of(text("09:15"))));
        ToolLoop.loop(rig.client(), "time?", ToolChoice.ofTool(ToolChoiceTool.builder().name("get_time").build()));
        assertEquals("tool", rig.http().requests.get(0).at("/tool_choice/type").asText());
        assertEquals("get_time", rig.http().requests.get(0).at("/tool_choice/name").asText());
        assertFalse(rig.http().requests.get(1).has("tool_choice"));
    }
}
