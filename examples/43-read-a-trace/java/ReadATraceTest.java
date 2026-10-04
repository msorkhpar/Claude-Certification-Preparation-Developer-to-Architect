import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReadATraceTest {
    private static ReadATrace.Failure failure(List<ReadATrace.Event> trace) {
        return ReadATrace.firstFailure(trace).orElseThrow();
    }

    @Test
    void anEmptyReplyAfterTextFollowingAToolResultIsOurMessageStructure() {
        ReadATrace.Failure f = failure(ReadATrace.TRACES.get("A: a tool loop that ends in silence"));
        assertEquals(List.of(3, "empty reply", "integration"), List.of(f.index(), f.what(), f.origin()));
    }

    @Test
    void theSameEmptyReplyWithoutThatTextIsTheModel() {
        ReadATrace.Failure f = failure(List.of(new ReadATrace.Request(List.of("tool_result")), new ReadATrace.Response(200, "end_turn", List.of())));
        assertEquals(List.of(1, "empty reply", "model"), List.of(f.index(), f.what(), f.origin()));
    }

    @Test
    void a529IsTheServiceAndTheNextActionIsARetry() {
        ReadATrace.Failure f = failure(ReadATrace.TRACES.get("B: a busy service and a retry"));
        assertEquals(new ReadATrace.Failure(1, "overloaded_error", "service", "retry with back-off"), f);
    }

    @Test
    void jsonInsideACodeFenceIsTheParserAndProseWithoutJsonIsTheModel() {
        ReadATrace.Failure fenced = failure(ReadATrace.TRACES.get("C: JSON in a code fence"));
        assertEquals(List.of(2, "parse failure", "integration"), List.of(fenced.index(), fenced.what(), fenced.origin()));
        ReadATrace.Failure prose = failure(List.of(new ReadATrace.Parse(false, "I am not sure")));
        assertEquals(List.of(0, "parse failure", "model"), List.of(prose.index(), prose.what(), prose.origin()));
    }

    @Test
    void aCleanTraceHasNoFailure() {
        assertTrue(ReadATrace.firstFailure(List.of(new ReadATrace.Request(List.of("text")), new ReadATrace.Response(200, "end_turn", List.of("text")))).isEmpty());
    }
}
