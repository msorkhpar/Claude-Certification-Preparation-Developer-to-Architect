import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ParamsTest {
    private val fable = "claude-fable-5-1"
    private val opus = "claude-opus-5-5"
    private val sonnet = "claude-sonnet-5-5"
    private val haiku = "claude-haiku-4-5-20251001"

    /** The parameter named by the RejectedRequest, or null when the request was accepted. */
    private fun refused(model: String, options: Map<String, Any?> = emptyMap(), maxTokens: Int = 4096): String? = try {
        buildParams(model, maxTokens, options)
        null
    } catch (e: RejectedRequest) {
        e.param
    } catch (e: RuntimeException) {
        "crash: $e"
    }

    private val adaptive = mapOf("type" to "adaptive")
    private fun enabled(budget: Int) = mapOf("type" to "enabled", "budget_tokens" to budget)

    @Test
    fun m1_eachCourseModelGetsTheRequestItAccepts() {
        assertEquals(mapOf("model" to fable, "max_tokens" to 8000, "output_config" to mapOf("effort" to "xhigh")), buildParams(fable, 8000, mapOf("effort" to "xhigh")))
        assertEquals(mapOf("model" to opus, "max_tokens" to 4096), buildParams(opus, 4096))
        assertEquals(mapOf("model" to sonnet, "max_tokens" to 4096, "thinking" to adaptive, "output_config" to mapOf("effort" to "medium")),
            buildParams(sonnet, 4096, mapOf("thinking" to adaptive, "effort" to "medium")))
        assertEquals(mapOf("model" to haiku, "max_tokens" to 4096, "thinking" to enabled(2048)), buildParams(haiku, 4096, mapOf("thinking" to enabled(2048))))
    }

    @Test
    fun e1_thinkingModesAModelDoesNotHaveAreRefused() {
        for (model in listOf(fable, opus, sonnet)) assertEquals("thinking", refused(model, mapOf("thinking" to mapOf("type" to "disabled"))), model)
        for (model in listOf(opus, sonnet, fable)) assertEquals("thinking", refused(model, mapOf("thinking" to enabled(2048))), model)
        assertEquals("thinking", refused(haiku, mapOf("thinking" to adaptive)))
        assertNull(refused(haiku, mapOf("thinking" to mapOf("type" to "disabled"))))
        assertNull(refused(opus, mapOf("thinking" to adaptive)))
    }

    @Test
    fun e2_theModeThatSkipsThinkingUpFrontIsSonnetOnlyAndNeedsHighEffortOrBelow() {
        val between = mapOf("type" to "between_tools")
        assertEquals(between, buildParams(sonnet, 4096, mapOf("thinking" to between))?.get("thinking"))
        for (level in listOf("low", "medium", "high")) assertNull(refused(sonnet, mapOf("thinking" to between, "effort" to level)), level)
        for (level in listOf("xhigh", "max")) assertEquals("thinking", refused(sonnet, mapOf("thinking" to between, "effort" to level)), level)
        assertEquals("thinking", refused(opus, mapOf("thinking" to between)))
        assertEquals("thinking", refused(haiku, mapOf("thinking" to between)))
    }

    @Test
    fun e3_effortNeedsASupportingModelAndARealLevel() {
        assertEquals("output_config.effort", refused(haiku, mapOf("effort" to "low")))
        for (level in listOf("low", "medium", "high", "xhigh", "max")) {
            assertNull(refused(sonnet, mapOf("effort" to level)), level)
            assertNull(refused(fable, mapOf("effort" to level)), level)
        }
        assertEquals("output_config.effort", refused(opus, mapOf("effort" to "adaptive")))
        assertEquals("output_config.effort", refused(opus, mapOf("effort" to "extreme")))
    }

    @Test
    fun e4_newerModelsRejectSamplingParametersAndHaikuKeepsThem() {
        assertEquals("temperature", refused(opus, mapOf("temperature" to 0.2)))
        assertEquals("top_p", refused(sonnet, mapOf("top_p" to 0.9)))
        assertEquals("top_k", refused(fable, mapOf("top_k" to 40)))
        assertNull(refused(opus, mapOf("temperature" to 1.0)))
        assertEquals(mapOf("model" to haiku, "max_tokens" to 1024, "temperature" to 0.2, "top_k" to 40), buildParams(haiku, 1024, mapOf("temperature" to 0.2, "top_k" to 40)))
    }

    @Test
    fun e5_fastModeIsOpusOnlyWithItsBetaHeaderAndNeverInABatch() {
        val params = buildParams(opus, 4096, mapOf("speed" to "fast")) ?: emptyMap()
        assertEquals("fast", params["speed"])
        assertEquals(listOf("fast-mode-2026-02-01"), params["betas"])
        assertEquals("speed", refused(sonnet, mapOf("speed" to "fast")))
        assertEquals("speed", refused(haiku, mapOf("speed" to "fast")))
        assertEquals("speed", refused(opus, mapOf("speed" to "fast", "batch" to true)))
        assertFalse((buildParams(opus, 4096, mapOf("speed" to "standard")) ?: mapOf("speed" to 1)).containsKey("speed"))
    }

    @Test
    fun e6_aManualBudgetIsAtLeast1024AndBelowMaxTokens() {
        assertEquals("thinking.budget_tokens", refused(haiku, mapOf("thinking" to enabled(1023)), 4096))
        assertNull(refused(haiku, mapOf("thinking" to enabled(1024)), 2048))
        assertEquals("thinking.budget_tokens", refused(haiku, mapOf("thinking" to enabled(2048)), 2048))
        assertEquals("thinking.budget_tokens", refused(haiku, mapOf("thinking" to mapOf("type" to "enabled")), 2048))
        assertEquals("max_tokens", refused(opus, emptyMap(), 0))
    }

    @Test
    fun e7_effortLivesInOutputConfigAndNeverInsideThinking() {
        val params = buildParams(sonnet, 4096, mapOf("thinking" to adaptive, "effort" to "high")) ?: emptyMap()
        assertEquals(adaptive, params["thinking"])
        assertEquals(mapOf("effort" to "high"), params["output_config"])
        val options = mapOf("thinking" to adaptive, "effort" to "low")
        buildParams(sonnet, 4096, options)
        assertEquals(mapOf("thinking" to adaptive, "effort" to "low"), options)
    }
}
