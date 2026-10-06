import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PromptPlanTest {
    private val role: Map<String, Any?> = mapOf("name" to "role", "static" to true, "text" to "r".repeat(400)) // 100 tokens
    private val policy: Map<String, Any?> = mapOf("name" to "policy", "static" to true, "text" to "p".repeat(1648)) // 412 tokens: the prefix is exactly 512
    private val history: Map<String, Any?> = mapOf("name" to "history", "static" to false, "priority" to 1, "text" to "h".repeat(200)) // 50 tokens
    private val question: Map<String, Any?> = mapOf("name" to "question", "static" to false, "priority" to 9, "text" to "Q: {q}")
    private val extra: Map<String, Any?> = mapOf("name" to "extra", "static" to false, "priority" to 1, "text" to "e".repeat(160)) // 40 tokens

    private fun with(base: Map<String, Any?>, key: String, value: Any?): Map<String, Any?> = LinkedHashMap(base).also { it[key] = value }

    private fun built(modules: List<Map<String, Any?>>, variables: Map<String, String>, budget: Int = 10_000): Map<String, Any?> {
        val result = assemble(modules, variables, budget)
        assertNotNull(result, "assemble returned nothing")
        return result!!
    }

    @Suppress("UNCHECKED_CAST")
    private fun names(prompt: Map<String, Any?>): List<String> = (prompt["blocks"] as List<Map<String, Any?>>).map { it["name"] as String }

    @Suppress("UNCHECKED_CAST")
    private fun text(prompt: Map<String, Any?>, index: Int): String = (prompt["blocks"] as List<Map<String, Any?>>)[index]["text"] as String

    private fun refused(modules: List<Map<String, Any?>>, variables: Map<String, String>, budget: Int = 10_000): String {
        try {
            assemble(modules, variables, budget)
        } catch (error: IllegalArgumentException) {
            return error.message ?: ""
        }
        return fail("the modules were accepted")
    }

    @Test
    fun m1_staticModulesComeFirstAndTheBreakpointFollowsTheLastOne() {
        val prompt = built(listOf(question, role, history, policy), mapOf("q" to "hello"))
        assertEquals(listOf("role", "policy", "question", "history"), names(prompt))
        assertEquals(1, prompt["breakpoint"])
        assertEquals(564, prompt["tokens"])
        assertEquals(emptyList<String>(), prompt["dropped"])
    }

    @Test
    fun e1_aVariableInAStaticModuleIsRefused() {
        assertTrue(refused(listOf(role, with(policy, "text", "policy for {customer}"), question), mapOf("q" to "x", "customer" to "Ana")).contains("static"))
    }

    @Test
    fun e2_dynamicVariablesAreFilledAndAMissingOneIsRefused() {
        val prompt = built(listOf(role, policy, question), mapOf("q" to "hello", "unused" to "x"))
        assertEquals("Q: hello", text(prompt, 2))
        assertTrue(refused(listOf(role, policy, question), emptyMap()).contains("missing variable: q"))
    }

    @Test
    fun e3_theLowestPriorityDynamicModuleIsDroppedFirstAndATieDropsTheLaterOne() {
        val modules = listOf(role, policy, history, question, extra)
        val one = built(modules, mapOf("q" to "hello"), 570)
        assertEquals(listOf("extra"), one["dropped"])
        assertEquals(listOf("role", "policy", "history", "question"), names(one))
        assertEquals(564, one["tokens"])
        val two = built(modules, mapOf("q" to "hello"), 520)
        assertEquals(listOf("extra", "history"), two["dropped"])
        assertEquals(listOf("role", "policy", "question"), names(two))
        assertEquals(514, two["tokens"])
    }

    @Test
    fun e4_staticModulesAreNeverDroppedAndABudgetTheyExceedIsRefused() {
        assertTrue(refused(listOf(role, policy, history, question), mapOf("q" to "hello"), 400).contains("over budget"))
        assertTrue(refused(listOf(role, policy), emptyMap(), 511).contains("over budget"))
        assertEquals(512, built(listOf(role, policy), emptyMap(), 512)["tokens"])
    }

    @Test
    fun e5_aPrefixUnderTheMinimumGetsNoBreakpoint() {
        assertEquals(512, MIN_CACHEABLE)
        assertEquals(1, built(listOf(role, policy), emptyMap())["breakpoint"])
        assertNull(built(listOf(role, with(policy, "text", "p".repeat(1644))), emptyMap())["breakpoint"])
        assertNull(built(listOf(history, question), mapOf("q" to "x"))["breakpoint"])
    }

    private val models: List<Map<String, Any?>> = listOf(
        mapOf("name" to "small", "tier" to 1, "latency_ms" to 300, "price_out" to 1),
        mapOf("name" to "mid2", "tier" to 2, "latency_ms" to 900, "price_out" to 5),
        mapOf("name" to "big", "tier" to 3, "latency_ms" to 2500, "price_out" to 25),
        mapOf("name" to "mid", "tier" to 2, "latency_ms" to 900, "price_out" to 5),
    )

    private fun pick(tier: Int, latency: Int): String? = chooseModel(mapOf("tier" to tier, "max_latency_ms" to latency), models)

    @Test
    fun e6_theCheapestModelThatMeetsTheTierAndTheLatencyWinsAndTiesGoByName() {
        assertEquals("small", pick(1, 5000))
        assertEquals("mid", pick(2, 1000))
        assertEquals("big", pick(3, 5000))
        assertNull(pick(3, 1000))
        assertNull(pick(2, 500))
        assertNull(pick(1, 100))
    }

    private fun reuse(a: Map<String, Any?>, b: Map<String, Any?>): Int {
        val result = reusablePrefix(a, b)
        assertNotNull(result, "reusablePrefix returned nothing")
        return result!!
    }

    @Test
    fun e7_onlyAnIdenticalStaticPrefixCanBeReused() {
        val base = built(listOf(role, policy, history, question), mapOf("q" to "one"))
        assertEquals(512, reuse(base, built(listOf(role, policy, history, question), mapOf("q" to "a different question"))))
        assertEquals(0, reuse(base, built(listOf(with(role, "text", "R".repeat(400)), policy, history, question), mapOf("q" to "one"))))
        assertEquals(0, reuse(base, built(listOf(role, with(policy, "text", "P".repeat(1648)), history, question), mapOf("q" to "one"))))
        assertEquals(512, reuse(base, built(listOf(role, policy, with(history, "text", "other"), question), mapOf("q" to "one"))))
        assertEquals(0, reuse(base, built(listOf(history, question), mapOf("q" to "one"))))
        assertEquals(0, reuse(built(listOf(history), emptyMap()), built(listOf(history), emptyMap())))
    }
}
