import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RefinementTest {
    private static Refinement.Task task(boolean diff, int files, boolean architectural, int approaches) {
        return new Refinement.Task(diff, files, architectural, approaches);
    }

    @Test
    void aChangeYouCanSayInOneSentenceIsDoneDirectly() {
        assertEquals(List.of("implement"), Refinement.chooseMode(task(true, 1, false, 1)));
    }

    @Test
    void largeArchitecturalOrAmbiguousChangesArePlannedFirst() {
        List<String> planned = List.of("explore", "plan", "implement");
        assertEquals(planned, Refinement.chooseMode(task(false, 1, true, 1)));
        assertEquals(planned, Refinement.chooseMode(task(false, 45, false, 1)));
        assertEquals(planned, Refinement.chooseMode(task(false, 1, false, 2)));
        assertEquals(planned, Refinement.chooseMode(task(true, 1, true, 1)));
    }

    @Test
    void interactingProblemsTravelTogetherAndIndependentOnesGoOneAtATime() {
        var issues = List.of(new Refinement.Issue("a", List.of("b")), new Refinement.Issue("b"), new Refinement.Issue("c"), new Refinement.Issue("d", List.of("c")), new Refinement.Issue("e"));
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d"), List.of("e")), Refinement.groupFeedback(issues));
        assertEquals(List.of(), Refinement.groupFeedback(List.of()));
    }

    @Test
    void aFailureReportNamesEachFailingTestWithInputAndExpectedOutput() {
        var results = List.of(new Refinement.Result("ok", 1, 2, 2), new Refinement.Result("empty", List.of(), List.of(), null));
        assertEquals("1 of 2 tests fail:\n- empty: input [], expected [], got None", Refinement.failureReport(results));
        assertEquals("All tests pass.", Refinement.failureReport(results.subList(0, 1)));
    }
}
