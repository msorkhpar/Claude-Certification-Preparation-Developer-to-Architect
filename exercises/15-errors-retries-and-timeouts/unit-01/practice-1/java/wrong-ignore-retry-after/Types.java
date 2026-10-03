import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/** The given types. Header names are lower case; body is the parsed JSON object, or an empty map when there is none. */
record Response(int status, Map<String, String> headers, Map<String, Object> body) {}

interface Send {
    Response send();
}

interface Sleep {
    void sleep(double seconds);
}

/** max_attempts, the first delay, the cap on a delay and a jitter function applied to each computed delay. */
record Policy(int maxAttempts, double baseDelay, double cap, DoubleUnaryOperator jitter) {
    static Policy defaults() {
        return new Policy(4, 0.5, 8.0, d -> d);
    }
}

/** The connection failed or timed out: no reply was received. */
class TransportError extends RuntimeException {
    TransportError(String message) {
        super(message);
    }
}

/** The call did not succeed. errorType is the API's error type, "connection_error" or "unknown". */
class CallFailed extends RuntimeException {
    private final int status;
    private final String errorType;
    private final int attempts;
    private final String requestId;

    CallFailed(int status, String errorType, int attempts, String requestId) {
        super(status + " " + errorType + " after " + attempts + " attempt(s)");
        this.status = status;
        this.errorType = errorType;
        this.attempts = attempts;
        this.requestId = requestId;
    }

    int status() { return status; }
    String errorType() { return errorType; }
    int attempts() { return attempts; }
    String requestId() { return requestId; }
}
