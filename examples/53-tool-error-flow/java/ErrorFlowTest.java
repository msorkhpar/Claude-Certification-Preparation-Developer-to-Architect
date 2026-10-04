import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ErrorFlowTest {
    @Test
    void aTransientFailureIsRetriedWithDoublingWaitsUntilItWorks() {
        ErrorFlow.Run run = ErrorFlow.run(new ErrorFlow.Scripted(new ErrorFlow.ToolError("transient", "busy."), new ErrorFlow.ToolError("transient", "busy."), "ok"), Map.of());
        assertEquals("ok", run.result().content());
        assertEquals(3, run.result().attempts());
        assertEquals(List.of(100, 200), run.waits());
    }

    @Test
    void aTimeoutWithoutAKeyIsNotRepeatedAndNamesTheCheckToMake() {
        var tool = new ErrorFlow.Scripted(new ErrorFlow.ToolError("timeout", "No answer."), "created");
        ErrorFlow.Run run = ErrorFlow.run(tool, Map.of());
        assertEquals("outcome_unknown", run.result().kind());
        assertEquals(1, tool.n);
        assertEquals(List.of(), run.waits());
        assertEquals("check the state first", ErrorFlow.nextStep(run.result()));
    }

    @Test
    void theSameKeyGoesOutWithEveryRetry() {
        var tool = new ErrorFlow.Scripted(new ErrorFlow.ToolError("timeout", "No answer."), "created");
        ErrorFlow.Run run = ErrorFlow.run(tool, Map.of(), new ErrorFlow.Options("k-1", false));
        assertFalse(run.result().isError());
        assertEquals(List.of("k-1", "k-1"), tool.seen);
    }

    @Test
    void anEmptyResultIsAcceptedAndEveryScenarioHasANextStep() {
        ErrorFlow.Run run = ErrorFlow.run(new ErrorFlow.Scripted(List.of()), Map.of(), new ErrorFlow.Options(null, true));
        assertEquals("accept the empty result", ErrorFlow.nextStep(run.result()));
        for (ErrorFlow.Scenario s : ErrorFlow.scenarios()) assertNotNull(ErrorFlow.nextStep(ErrorFlow.run(s.tool(), Map.of(), s.options()).result()));
    }
}
