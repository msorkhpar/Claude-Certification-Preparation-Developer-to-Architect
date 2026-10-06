import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CriteriaLintTest {
    @Test
    void aCriterionThatNamesNoPatternIsLintedAndAConcreteOneIsNot() {
        assertEquals(List.of("vague-report", "no-skip", "no-high-example", "no-low-example"), CriteriaLint.lintCriterion(CriteriaLint.VAGUE_CRITERION));
        assertEquals(List.of(), CriteriaLint.lintCriterion(CriteriaLint.GOOD_CRITERION));
        assertEquals(List.of("vague-skip"), CriteriaLint.lintCriterion(CriteriaLint.GOOD_CRITERION.withSkip("Use your judgment.")));
    }

    @Test
    void aSetOfExamplesNeedsTwoToFourBothVerdictsAndAReasonEach() {
        var report = new CriteriaLint.Example("report", "r");
        var skip = new CriteriaLint.Example("skip", "s");
        assertEquals(List.of(), CriteriaLint.lintExamples(List.of(report, skip)));
        assertEquals(List.of("two-to-four", "both-verdicts"), CriteriaLint.lintExamples(List.of(report)));
        assertEquals(List.of("two-to-four"), CriteriaLint.lintExamples(List.of(report, skip, report, skip, report)));
        assertEquals(List.of("both-verdicts", "reason-missing"), CriteriaLint.lintExamples(List.of(report, new CriteriaLint.Example("report", ""))));
    }

    @Test
    void aCategoryIsSwitchedOffOnlyWithEnoughReviewsAndALowShareAccepted() {
        List<CriteriaLint.Verdict> verdicts = new ArrayList<>();
        verdicts.addAll(CriteriaLint.repeat("bug", "accepted", 9));
        verdicts.addAll(CriteriaLint.repeat("bug", "dismissed", 1));
        verdicts.addAll(CriteriaLint.repeat("style", "accepted", 2));
        verdicts.addAll(CriteriaLint.repeat("style", "dismissed", 6));
        verdicts.addAll(CriteriaLint.repeat("naming", "dismissed", 3));
        var table = CriteriaLint.trust(verdicts);
        assertEquals(new CriteriaLint.Row(10, 9, 0.9, false), table.get("bug"));
        assertEquals(new CriteriaLint.Row(8, 2, 0.25, true), table.get("style"));
        assertFalse(table.get("naming").off());
        assertEquals(0.0, table.get("naming").precision());
    }
}
