import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FlowTest {
    @Test
    fun theAdaptiveRunEndsWhenThePlannerSaysDoneAndKeepsEveryStep() {
        val run = runAdaptive("goal")
        assertEquals("done", run.status)
        assertEquals(listOf("list the test files", "run the failing test", "read the module under test"), run.steps.map { it.subtask })
        assertEquals("the failure is in parse()", run.summary)
    }

    @Test
    fun theStepLimitEndsARunThatThePlannerDoesNot() {
        val run = runAdaptive("goal", maxSteps = 2)
        assertEquals("step_limit", run.status)
        assertEquals(2, run.steps.size)
    }

    @Test
    fun thePlannerChoiceDependsOnTheLastResult() {
        assertTrue(plan("g", listOf(Step("list the test files", "3 files"), Step("run the failing test", "all passed"))).done)
        assertEquals("read the module under test", plan("g", listOf(Step("list the test files", "3 files"), Step("run the failing test", "1 failure in test_parse"))).next)
    }

    @Test
    fun everyFileHasASummaryAndAWorkerAnswerExistsForEachSubtask() {
        assertEquals(CHANGE.keys, SUMMARIES.keys)
        assertEquals("3 files", work("list the test files"))
    }
}
