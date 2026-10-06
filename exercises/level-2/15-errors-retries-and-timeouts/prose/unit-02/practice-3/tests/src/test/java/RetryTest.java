import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetryTest {
    private static final Response OK = new Response(200, Map.of(), Map.of("type", "message"));

    private static Response status(int code) {
        return status(code, Map.of(), null, null);
    }

    private static Response status(int code, Map<String, String> headers, String kind, Map<String, Object> details) {
        Map<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("type", kind == null ? "api_error" : kind);
        error.put("message", "x");
        if (details != null) error.put("details", details);
        return new Response(code, headers, Map.of("type", "error", "error", error));
    }

    /** What one run produced: the calls made, the waits requested, the result or the error. */
    private static final class Run {
        int calls;
        final List<Double> slept = new ArrayList<>();
        Response result;
        RuntimeException err;
    }

    private static Run run(Policy policy, Object... replies) {
        Run r = new Run();
        List<Object> queue = new ArrayList<>(List.of(replies));
        Send send = () -> {
            r.calls++;
            Object item = queue.remove(0);
            if (item instanceof RuntimeException e) throw e;
            return (Response) item;
        };
        try {
            r.result = Retry.callWithRetry(send, r.slept::add, policy);
        } catch (RuntimeException e) {
            r.err = e;
        }
        return r;
    }

    private static Run run(Object... replies) {
        return run(Policy.defaults(), replies);
    }

    private static Policy with(int maxAttempts, double base, double cap) {
        return new Policy(maxAttempts, base, cap, d -> d);
    }

    @Test
    void m1_overloadedTwiceThenSuccessReturnsTheGoodReplyAfterTwoWaits() {
        Run r = run(status(529, Map.of(), "overloaded_error", null), status(503), OK);
        assertNull(r.err);
        assertSame(OK, r.result);
        assertEquals(3, r.calls);
        assertEquals(List.of(0.5, 1.0), r.slept);
    }

    @Test
    void e1_clientErrorsAreNotRetried() {
        Object[][] cases = {{400, "invalid_request_error"}, {401, "authentication_error"}, {404, "not_found_error"}, {413, "request_too_large"}};
        for (Object[] c : cases) {
            Run r = run(status((int) c[0], Map.of("request-id", "req_1"), (String) c[1], null), OK);
            CallFailed err = assertInstanceOf(CallFailed.class, r.err, String.valueOf(c[0]));
            assertEquals(List.of(c[0], c[1], 1, "req_1"), List.of(err.status(), err.errorType(), err.attempts(), err.requestId()));
            assertTrue(r.calls == 1 && r.slept.isEmpty());
        }
    }

    @Test
    void e2_retryAfterIsAFloorForTheWait() {
        Run r = run(with(5, 0.5, 8.0), status(429, Map.of("retry-after", "3"), "rate_limit_error", null),
            status(429, Map.of("retry-after", "1"), "rate_limit_error", null), status(529, Map.of("retry-after", "1"), null, null), OK);
        assertNull(r.err);
        assertEquals(List.of(3.0, 1.0, 2.0), r.slept); // waits: max(0.5, 3), max(1, 1), max(2, 1)
    }

    @Test
    void e3_delayDoublesUpToTheCapAndTheJitterIsAppliedLast() {
        Run r = run(with(7, 1.0, 5.0), status(500), status(500), status(500), status(500), status(500), status(500), OK);
        assertNull(r.err);
        assertEquals(List.of(1.0, 2.0, 4.0, 5.0, 5.0, 5.0), r.slept);
        Run halved = run(new Policy(4, 0.5, 8.0, d -> d / 2), status(500), status(500), OK);
        assertEquals(List.of(0.25, 0.5), halved.slept);
    }

    @Test
    void e4_aSpendCap429IsNotRetried() {
        Response cap = status(429, Map.of("request-id", "req_cap"), "rate_limit_error", Map.of("error_code", "enforced_spend_limit_reached"));
        Run r = run(cap, OK);
        CallFailed err = assertInstanceOf(CallFailed.class, r.err);
        assertEquals(List.of(429, 1, "req_cap"), List.of(err.status(), err.attempts(), err.requestId()));
        assertTrue(r.calls == 1 && r.slept.isEmpty());
    }

    @Test
    void e5_connectionErrorsAreRetriedLikeServerErrors() {
        Run r = run(new TransportError("reset"), new TransportError("timeout"), OK);
        assertNull(r.err);
        assertSame(OK, r.result);
        assertEquals(3, r.calls);
        assertEquals(List.of(0.5, 1.0), r.slept);
        Run dead = run(with(2, 0.5, 8.0), new TransportError("a"), new TransportError("b"));
        CallFailed err = assertInstanceOf(CallFailed.class, dead.err);
        assertEquals(List.of("0", "connection_error", "2"), List.of(String.valueOf(err.status()), err.errorType(), String.valueOf(err.attempts())));
        assertNull(err.requestId());
    }

    @Test
    void e6_givingUpReportsTheLastReplyAndDoesNotWaitAfterTheLastAttempt() {
        Run r = run(with(3, 0.5, 8.0), status(500, Map.of("request-id", "req_a"), null, null), status(503, Map.of("request-id", "req_b"), null, null),
            status(529, Map.of("request-id", "req_c"), "overloaded_error", null), OK);
        CallFailed err = assertInstanceOf(CallFailed.class, r.err);
        assertTrue(r.calls == 3 && r.slept.size() == 2);
        assertEquals(List.of("529", "overloaded_error", "3", "req_c"),
            List.of(String.valueOf(err.status()), err.errorType(), String.valueOf(err.attempts()), err.requestId()));
    }
}
