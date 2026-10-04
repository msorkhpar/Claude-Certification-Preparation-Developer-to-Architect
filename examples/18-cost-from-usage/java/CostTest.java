import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.models.messages.MessageCountTokensParams;
import com.anthropic.models.messages.Usage;
import harness.Scripted;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CostTest {
    private static Usage usageOf(Map<String, Object> changes) {
        Map<String, Object> usage = new HashMap<>(Cost.USAGE);
        usage.putAll(changes);
        return Scripted.client(message(List.of(text("x")), "end_turn", Cost.MODEL, usage, null)).client().messages().create(Cost.params()).usage();
    }

    @Test
    void costAddsInputCacheWriteAndOutput() {
        // 120*2 + 4000*2*1.25 + 340*10 micro-dollars
        assertEquals((240 + 10000 + 3400) / 1_000_000.0, Cost.cost(Cost.MODEL, usageOf(Map.of())), 1e-12);
    }

    @Test
    void aCacheReadIsAFractionOfTheInputPriceThatDependsOnTheModel() {
        Usage usage = usageOf(map("cache_read_input_tokens", 1_000_000, "cache_creation_input_tokens", 0,
            "cache_creation", map("ephemeral_5m_input_tokens", 0, "ephemeral_1h_input_tokens", 0), "output_tokens", 0, "input_tokens", 0));
        assertEquals(0.2, Cost.cost("claude-sonnet-5-5", usage), 1e-12);
        assertEquals(0.2, Cost.cost("claude-opus-5-5", usage), 1e-12);
        assertEquals(0.25, Cost.cost("claude-fable-5-1", usage), 1e-12);
        assertEquals(0.1, Cost.cost("claude-haiku-4-5-20251001", usage), 1e-12);
    }

    @Test
    void batchHalvesTheWholeCost() {
        Usage usage = usageOf(Map.of());
        assertEquals(Cost.cost(Cost.MODEL, usage) / 2, Cost.cost(Cost.MODEL, usage, true), 1e-12);
    }

    @Test
    void countTokensIsASeparateFreeCallToItsOwnPath() {
        Scripted.Rig rig = Scripted.client(map("input_tokens", 99));
        long counted = rig.client().messages().countTokens(MessageCountTokensParams.builder().model(Cost.MODEL).addUserMessage(Cost.QUESTION).build()).inputTokens();
        assertEquals(99, counted);
        assertTrue(rig.http().urls.get(0).endsWith("/v1/messages/count_tokens"));
        assertFalse(rig.http().requests.get(0).has("max_tokens"));
    }
}
