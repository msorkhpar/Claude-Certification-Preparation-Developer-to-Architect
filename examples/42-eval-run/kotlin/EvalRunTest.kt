import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EvalRunTest {
    @Test
    fun exactGradingIgnoresCaseAndSpacingOnly() {
        val case = Case("x", "", "neutral", emptyList())
        assertTrue(grade(case, " Neutral\n"))
        assertFalse(grade(case, "neutral."))
        assertFalse(grade(case, "positive"))
    }

    @Test
    fun theFirstPromptPassesFourOfSixAndFailsTheGateTwice() {
        val report = run(CASES, PROMPT_V1)
        assertEquals(listOf("sarcasm-1", "mixed-1"), report.results.filter { !it.passed }.map { it.id })
        assertEquals(0.667, Math.round(report.passRate * 1000) / 1000.0)
        assertEquals(1 to 3, report.byTag["edge"])
        assertEquals(listOf("overall", "tag:edge"), gate(report, CRITERIA))
    }

    @Test
    fun theSecondPromptHasABetterAverageAndOneRegression() {
        val v1 = run(CASES, PROMPT_V1)
        val v2 = run(CASES, PROMPT_V2)
        assertTrue(v2.passRate > v1.passRate)
        assertEquals(Diff(listOf("empty-1"), listOf("sarcasm-1", "mixed-1")), compare(v1, v2))
        assertEquals(listOf("tag:edge"), gate(v2, CRITERIA))
    }

    @Test
    fun aRunThatChangesNothingHasNoRegressions() {
        val v1 = run(CASES, PROMPT_V1)
        assertEquals(Diff(emptyList(), emptyList()), compare(v1, v1))
    }
}
