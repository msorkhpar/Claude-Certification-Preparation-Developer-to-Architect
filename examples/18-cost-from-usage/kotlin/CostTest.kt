import com.anthropic.models.messages.MessageCountTokensParams
import com.anthropic.models.messages.Usage
import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CostTest {
    private fun usageOf(changes: Map<String, Any?> = emptyMap()): Usage =
        Scripted.client(message(listOf(text("x")), "end_turn", MODEL, USAGE + changes, null)).client().messages().create(params()).usage()

    @Test
    fun costAddsInputCacheWriteAndOutput() {
        // 120*2 + 4000*2*1.25 + 340*10 micro-dollars
        assertEquals((240 + 10000 + 3400) / 1_000_000.0, cost(MODEL, usageOf()), 1e-12)
    }

    @Test
    fun aCacheReadIsAFractionOfTheInputPriceThatDependsOnTheModel() {
        val usage = usageOf(map("cache_read_input_tokens", 1_000_000, "cache_creation_input_tokens", 0, "cache_creation", map("ephemeral_5m_input_tokens", 0, "ephemeral_1h_input_tokens", 0), "output_tokens", 0, "input_tokens", 0))
        assertEquals(0.2, cost("claude-sonnet-5-5", usage), 1e-12)
        assertEquals(0.2, cost("claude-opus-5-5", usage), 1e-12)
        assertEquals(0.25, cost("claude-fable-5-1", usage), 1e-12)
        assertEquals(0.1, cost("claude-haiku-4-5-20251001", usage), 1e-12)
    }

    @Test
    fun batchHalvesTheWholeCost() {
        val usage = usageOf()
        assertEquals(cost(MODEL, usage) / 2, cost(MODEL, usage, batch = true), 1e-12)
    }

    @Test
    fun countTokensIsASeparateFreeCallToItsOwnPath() {
        val rig = Scripted.client(map("input_tokens", 99))
        val counted = rig.client().messages().countTokens(MessageCountTokensParams.builder().model(MODEL).addUserMessage(QUESTION).build()).inputTokens()
        assertEquals(99L, counted)
        assertTrue(rig.http().urls[0].endsWith("/v1/messages/count_tokens"))
        assertFalse(rig.http().requests[0].has("max_tokens"))
    }
}
