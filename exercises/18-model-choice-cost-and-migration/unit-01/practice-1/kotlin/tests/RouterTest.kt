import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RouterTest {
    // Prices in dollars per million tokens, read from the pricing page on 2026-10-02 (a dollar per MTok is a micro-dollar per token).
    private fun model(id: String, tier: Int, context: Long, maxOutput: Long, input: Double, output: Double, mult: Double): Map<String, Any?> =
        mapOf("id" to id, "tier" to tier, "context" to context, "max_output" to maxOutput, "input" to input, "output" to output, "cache_read_multiplier" to mult)

    private val haiku = model("claude-haiku-4-5-20251001", 1, 200_000, 64_000, 1.0, 5.0, 0.1)
    private val sonnet = model("claude-sonnet-5-5", 2, 1_000_000, 128_000, 2.0, 10.0, 0.1)
    private val opus = model("claude-opus-5-5", 3, 1_000_000, 128_000, 4.0, 20.0, 0.05)
    private val fable = model("claude-fable-5-1", 4, 1_000_000, 128_000, 10.0, 50.0, 0.025)
    private val catalog = listOf(haiku, sonnet, opus, fable)

    private fun usage(vararg kv: Pair<String, Any?>): Map<String, Any?> = mapOf(*kv)

    private fun task(minTier: Int, usage: Map<String, Any?>, maxTokens: Int? = null, batch: Boolean = false): Map<String, Any?> {
        val t = linkedMapOf<String, Any?>("min_tier" to minTier)
        if (maxTokens != null) t["max_tokens"] = maxTokens
        if (batch) t["batch"] = true
        t["usage"] = usage
        return t
    }

    private fun cost(model: Map<String, Any?>, usage: Map<String, Any?>) = requestCost(model, usage)

    private fun raised(block: () -> Unit): Throwable? = try { block(); null } catch (e: RuntimeException) { e }

    @Test
    fun m1_costOfAPlainRequestAndTheCheapestModelThatMeetsTheTier() {
        assertEquals(5400.0, cost(sonnet, usage("input_tokens" to 1200, "output_tokens" to 300)))
        assertEquals(2000.0, cost(haiku, usage("input_tokens" to 1000, "output_tokens" to 200)))
        assertEquals(haiku["id"], route(catalog, task(1, usage("input_tokens" to 1000, "output_tokens" to 200))))
        assertEquals(sonnet["id"], route(catalog, task(2, usage("input_tokens" to 1000, "output_tokens" to 200))))
    }

    @Test
    fun e1_cacheReadsAndWritesArePricedByTheirOwnMultipliers() {
        val split = usage("ephemeral_5m_input_tokens" to 1000, "ephemeral_1h_input_tokens" to 2000)
        val u = usage("input_tokens" to 100, "output_tokens" to 50, "cache_read_input_tokens" to 4000,
            "cache_creation_input_tokens" to 3000, "cache_creation" to split)
        assertEquals(23200.0, cost(opus, u)) // 400 + 5000 + 16000 + 800 + 1000
        assertEquals(250000.0, cost(fable, usage("cache_read_input_tokens" to 1_000_000)))
        assertEquals(200000.0, cost(sonnet, usage("cache_read_input_tokens" to 1_000_000)))
        // without the per-lifetime split, the written tokens are the 5-minute kind
        assertEquals(7500.0, cost(sonnet, usage("cache_creation_input_tokens" to 3000)))
    }

    @Test
    fun e2_theBatchDiscountHalvesEveryPartOfTheCost() {
        val split = usage("ephemeral_5m_input_tokens" to 1000, "ephemeral_1h_input_tokens" to 2000)
        val u = usage("input_tokens" to 100, "output_tokens" to 50, "cache_read_input_tokens" to 4000, "cache_creation" to split)
        assertEquals(11600.0, requestCost(opus, u, true))
        assertEquals(23200.0, cost(opus, u))
        assertEquals(2500.0, requestCost(haiku, usage("output_tokens" to 1000), true))
    }

    @Test
    fun e3_theRouterPicksByCostAndTierNotByTheOrderOfTheCatalog() {
        assertEquals(opus["id"], route(catalog.reversed(), task(3, usage("input_tokens" to 500, "output_tokens" to 100))))
        assertEquals(haiku["id"], route(catalog.reversed(), task(1, usage("input_tokens" to 500, "output_tokens" to 100))))
        assertEquals(opus["id"], route(listOf(fable, opus), task(1, usage("input_tokens" to 500, "output_tokens" to 100))))
        // batch changes every price by the same factor, so it never changes the winner
        assertEquals(sonnet["id"], route(catalog, task(2, usage("input_tokens" to 500), batch = true)))
    }

    @Test
    fun e4_aModelWhoseContextOrOutputLimitIsTooSmallIsSkipped() {
        val big = usage("input_tokens" to 250_000, "cache_read_input_tokens" to 50_000, "output_tokens" to 100)
        assertEquals(sonnet["id"], route(catalog, task(1, big)))
        assertEquals(sonnet["id"], route(catalog, task(1, usage("input_tokens" to 100, "output_tokens" to 100), maxTokens = 100_000)))
        assertEquals(haiku["id"], route(catalog, task(1, usage("input_tokens" to 100), maxTokens = 64_000)))
    }

    @Test
    fun e5_deprecatedModelsAreSkippedAndAnEmptyChoiceRaises() {
        val old = haiku + ("deprecated" to true)
        assertEquals(sonnet["id"], route(listOf(old, sonnet), task(1, usage("input_tokens" to 100))))
        assertTrue(raised { route(listOf(old), task(1, usage("input_tokens" to 100))) } is NoModelError)
        assertTrue(raised { route(catalog, task(5, usage("input_tokens" to 100))) } is NoModelError)
        assertTrue(raised { route(catalog, task(1, usage("input_tokens" to 2_000_000))) } is NoModelError)
    }

    @Test
    fun e6_aTieGoesToTheLowerTier() {
        val reads = usage("cache_read_input_tokens" to 1_000_000)
        assertEquals(200000.0, cost(sonnet, reads))
        assertEquals(200000.0, cost(opus, reads))
        assertEquals(sonnet["id"], route(catalog, task(2, reads)))
        assertEquals(sonnet["id"], route(catalog.reversed(), task(2, reads)))
    }
}
