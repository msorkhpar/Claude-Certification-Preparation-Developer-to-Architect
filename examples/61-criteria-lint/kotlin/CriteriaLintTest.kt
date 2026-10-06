import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CriteriaLintTest {
    @Test
    fun aCriterionThatNamesNoPatternIsLintedAndAConcreteOneIsNot() {
        assertEquals(listOf("vague-report", "no-skip", "no-high-example", "no-low-example"), lintCriterion(VAGUE_CRITERION))
        assertEquals(emptyList<String>(), lintCriterion(GOOD_CRITERION))
        assertEquals(listOf("vague-skip"), lintCriterion(GOOD_CRITERION.copy(skip = "Use your judgment.")))
    }

    @Test
    fun aSetOfExamplesNeedsTwoToFourBothVerdictsAndAReasonEach() {
        val report = Example("report", "r")
        val skip = Example("skip", "s")
        assertEquals(emptyList<String>(), lintExamples(listOf(report, skip)))
        assertEquals(listOf("two-to-four", "both-verdicts"), lintExamples(listOf(report)))
        assertEquals(listOf("two-to-four"), lintExamples(listOf(report, skip, report, skip, report)))
        assertEquals(listOf("both-verdicts", "reason-missing"), lintExamples(listOf(report, Example("report", ""))))
    }

    @Test
    fun aCategoryIsSwitchedOffOnlyWithEnoughReviewsAndALowShareAccepted() {
        val verdicts = repeat("bug", "accepted", 9) + repeat("bug", "dismissed", 1) + repeat("style", "accepted", 2) + repeat("style", "dismissed", 6) + repeat("naming", "dismissed", 3)
        val table = trust(verdicts)
        assertEquals(Row(10, 9, 0.9, false), table["bug"])
        assertEquals(Row(8, 2, 0.25, true), table["style"])
        assertFalse(table.getValue("naming").off)
        assertEquals(0.0, table.getValue("naming").precision)
    }
}
