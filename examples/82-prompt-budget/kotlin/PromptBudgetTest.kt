import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PromptBudgetTest {
    private val vars = mapOf("customer" to "Ana", "tier" to "gold", "question" to "Q")

    @Test
    fun tokensAreTheCeilingOfCharactersOverFour() {
        assertEquals(listOf(0, 1, 1, 2), listOf("", "a", "abcd", "abcde").map { tokens(it) })
    }

    @Test
    fun staticModulesComeFirstAndTheBreakpointFollowsTheLastOne() {
        val prompt = assemble(listOf(MODULES[2], MODULES[0], MODULES[3], MODULES[1]), vars)
        assertEquals(listOf("role", "policy", "customer", "question"), prompt.blocks.map { it.name })
        assertEquals("policy", prompt.blocks[prompt.breakpoint!!].name)
    }

    @Test
    fun dynamicVariablesAreFilledAndStaticTextIsLeftAlone() {
        val prompt = assemble(MODULES, vars)
        assertEquals("Customer: Ana. Tier: gold.", prompt.blocks[2].text)
        assertFalse(prompt.blocks[1].text.contains("{"))
    }

    @Test
    fun aPrefixUnderTheMinimumGetsNoBreakpoint() {
        val prompt = assemble(MODULES.filter { it.name != "policy" }, vars)
        assertTrue(prompt.prefixTokens < MIN_CACHEABLE)
        assertNull(prompt.breakpoint)
    }

    @Test
    fun thePrefixSurvivesADynamicChangeAndBreaksOnAStaticOne() {
        val base = assemble(MODULES, vars)
        val other = assemble(MODULES, mapOf("customer" to "Ben", "tier" to "basic", "question" to "Other"))
        assertEquals(cachedPrefix(base), cachedPrefix(other))
        assertNotEquals("", cachedPrefix(base))
        val edited = MODULES.map { if (it.name == "role") it.copy(text = it.text + " Extra.") else it }
        assertNotEquals(cachedPrefix(base), cachedPrefix(assemble(edited, vars)))
    }
}
