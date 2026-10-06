import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ErrorFlowTest {
    @Test
    fun aTransientFailureIsRetriedWithDoublingWaitsUntilItWorks() {
        val (result, waits) = run(Scripted(ToolError("transient", "busy."), ToolError("transient", "busy."), "ok"), emptyMap())
        assertEquals("ok", result.content)
        assertEquals(3, result.attempts)
        assertEquals(listOf(100, 200), waits)
    }

    @Test
    fun aTimeoutWithoutAKeyIsNotRepeatedAndNamesTheCheckToMake() {
        val tool = Scripted(ToolError("timeout", "No answer."), "created")
        val (result, waits) = run(tool, emptyMap())
        assertEquals("outcome_unknown", result.kind)
        assertEquals(1, tool.n)
        assertEquals(emptyList<Int>(), waits)
        assertEquals("check the state first", nextStep(result))
    }

    @Test
    fun theSameKeyGoesOutWithEveryRetry() {
        val tool = Scripted(ToolError("timeout", "No answer."), "created")
        val (result, _) = run(tool, emptyMap(), Options(key = "k-1"))
        assertFalse(result.isError)
        assertEquals(listOf<String?>("k-1", "k-1"), tool.seen)
    }

    @Test
    fun anEmptyResultIsAcceptedAndEveryScenarioHasANextStep() {
        val (result, _) = run(Scripted(emptyList<String>()), emptyMap(), Options(readOnly = true))
        assertEquals("accept the empty result", nextStep(result))
        for (s in scenarios()) assertTrue(nextStep(run(s.tool, emptyMap(), s.options).first).isNotEmpty())
    }
}
