import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class EvalRunTest {
    @Test
    void exactGradingIgnoresCaseAndSpacingOnly() {
        EvalRun.Case c = new EvalRun.Case("x", "", "neutral", List.of());
        assertTrue(EvalRun.grade(c, " Neutral\n"));
        assertFalse(EvalRun.grade(c, "neutral."));
        assertFalse(EvalRun.grade(c, "positive"));
    }

    @Test
    void theFirstPromptPassesFourOfSixAndFailsTheGateTwice() {
        EvalRun.Report report = EvalRun.run(EvalRun.CASES, EvalRun.PROMPT_V1);
        assertEquals(List.of("sarcasm-1", "mixed-1"), report.results().stream().filter(r -> !r.passed()).map(EvalRun.Result::id).toList());
        assertEquals(0.667, Math.round(report.passRate() * 1000) / 1000.0);
        assertArrayEquals(new int[] {1, 3}, report.byTag().get("edge"));
        assertEquals(List.of("overall", "tag:edge"), EvalRun.gate(report, EvalRun.CRITERIA));
    }

    @Test
    void theSecondPromptHasABetterAverageAndOneRegression() {
        EvalRun.Report v1 = EvalRun.run(EvalRun.CASES, EvalRun.PROMPT_V1);
        EvalRun.Report v2 = EvalRun.run(EvalRun.CASES, EvalRun.PROMPT_V2);
        assertTrue(v2.passRate() > v1.passRate());
        assertEquals(new EvalRun.Diff(List.of("empty-1"), List.of("sarcasm-1", "mixed-1")), EvalRun.compare(v1, v2));
        assertEquals(List.of("tag:edge"), EvalRun.gate(v2, EvalRun.CRITERIA));
    }

    @Test
    void aRunThatChangesNothingHasNoRegressions() {
        EvalRun.Report v1 = EvalRun.run(EvalRun.CASES, EvalRun.PROMPT_V1);
        assertEquals(new EvalRun.Diff(List.of(), List.of()), EvalRun.compare(v1, v1));
    }
}
