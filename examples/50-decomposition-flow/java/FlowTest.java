import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FlowTest {
    @Test
    void theAdaptiveRunEndsWhenThePlannerSaysDoneAndKeepsEveryStep() {
        Flow.Run run = Flow.runAdaptive("goal");
        assertEquals("done", run.status());
        assertEquals(List.of("list the test files", "run the failing test", "read the module under test"), run.steps().stream().map(Flow.Step::subtask).toList());
        assertEquals("the failure is in parse()", run.summary());
    }

    @Test
    void theStepLimitEndsARunThatThePlannerDoesNot() {
        Flow.Run run = Flow.runAdaptive("goal", 2);
        assertEquals("step_limit", run.status());
        assertEquals(2, run.steps().size());
    }

    @Test
    void thePlannerChoiceDependsOnTheLastResult() {
        assertTrue(Flow.plan("g", List.of(new Flow.Step("list the test files", "3 files"), new Flow.Step("run the failing test", "all passed"))).done());
        assertEquals("read the module under test",
            Flow.plan("g", List.of(new Flow.Step("list the test files", "3 files"), new Flow.Step("run the failing test", "1 failure in test_parse"))).next());
    }

    @Test
    void everyFileHasASummaryAndAWorkerAnswerExistsForEachSubtask() {
        assertEquals(Flow.CHANGE.keySet(), Flow.SUMMARIES.keySet());
        assertEquals("3 files", Flow.work("list the test files"));
    }
}
