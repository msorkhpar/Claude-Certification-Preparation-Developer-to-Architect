import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RefinementTest {
    private fun task(diff: Boolean = false, files: Int = 1, architectural: Boolean = false, approaches: Int = 1) = Task(diff, files, architectural, approaches)

    @Test
    fun aChangeYouCanSayInOneSentenceIsDoneDirectly() {
        assertEquals(listOf("implement"), chooseMode(task(diff = true)))
    }

    @Test
    fun largeArchitecturalOrAmbiguousChangesArePlannedFirst() {
        val planned = listOf("explore", "plan", "implement")
        assertEquals(planned, chooseMode(task(architectural = true)))
        assertEquals(planned, chooseMode(task(files = 45)))
        assertEquals(planned, chooseMode(task(approaches = 2)))
        assertEquals(planned, chooseMode(task(diff = true, architectural = true)))
    }

    @Test
    fun interactingProblemsTravelTogetherAndIndependentOnesGoOneAtATime() {
        val issues = listOf(Issue("a", listOf("b")), Issue("b"), Issue("c"), Issue("d", listOf("c")), Issue("e"))
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d"), listOf("e")), groupFeedback(issues))
        assertEquals(emptyList<List<String>>(), groupFeedback(emptyList()))
    }

    @Test
    fun aFailureReportNamesEachFailingTestWithInputAndExpectedOutput() {
        val results = listOf(Result("ok", 1, 2, 2), Result("empty", emptyList<Int>(), emptyList<Int>(), null))
        assertEquals("1 of 2 tests fail:\n- empty: input [], expected [], got None", failureReport(results))
        assertEquals("All tests pass.", failureReport(results.take(1)))
    }
}
