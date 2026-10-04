import static org.junit.jupiter.api.Assertions.*;

import com.anthropic.errors.RateLimitException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BoundedTest {
    @Test
    void unboundedRunsEverythingAtOnce() {
        assertEquals(12, Bounded.runAll(null).peak());
    }

    @Test
    void aSemaphoreCapsInFlightRequests() {
        assertEquals(4, Bounded.runAll(4).peak());
    }

    @Test
    void oneFailureDoesNotCancelTheOthersAndOrderIsKept() {
        Bounded.Run run = Bounded.runAll(4);
        assertInstanceOf(RateLimitException.class, run.results().get(6).error());
        List<String> labels = IntStream.range(0, 12).filter(i -> i != 6).mapToObj(i -> run.results().get(i).label()).toList();
        assertEquals(IntStream.rangeClosed(1, 12).filter(n -> n != 7).mapToObj(n -> "label for ticket " + n).toList(), labels);
    }
}
