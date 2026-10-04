import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ErrorContextTest {
    @Test
    void theGenericStatusLosesThePartialResultAndTheCause() {
        assertEquals("found n1, n2, b1; sources unavailable: papers, filings", ErrorContext.generic(ErrorContext.OUTCOMES));
    }

    @Test
    void silentSuppressionReportsAFailedSourceAsASearchThatFoundNothing() {
        assertEquals("found n1, n2, b1; nothing found in: papers, patents, filings", ErrorContext.suppress(ErrorContext.OUTCOMES));
    }

    @Test
    void abortingOnTheFirstFailureLosesEveryLaterSource() {
        assertEquals("aborted at papers; found n1, n2", ErrorContext.terminate(ErrorContext.OUTCOMES));
        Map<String, ErrorContext.Outcome> one = new LinkedHashMap<>();
        one.put("a", new ErrorContext.Outcome("ok", List.of("x")));
        assertEquals("found x", ErrorContext.terminate(one));
    }

    @Test
    void structuredContextKeepsThePartialResultTheEmptyAnswerAndTheWayForward() {
        assertEquals("well supported: news, blogs; partial: papers (timeout, kept p1); no findings: patents; gaps: filings (permission, try: request access)", ErrorContext.structured(ErrorContext.OUTCOMES));
    }
}
