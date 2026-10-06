import java.util.Map;

/** The given types. Header names are lower case. */
record Request(String method, String url, Map<String, String> headers, String body) {}

record Response(int status, Map<String, String> headers, String body) {}

interface Transport {
    Response send(Request request);
}

/** A non-2xx reply: status, error type, detail and the request id (null when there is none). */
class ApiError extends RuntimeException {
    private final int status;
    private final String errorType;
    private final String detail;
    private final String requestId;

    ApiError(int status, String errorType, String detail, String requestId) {
        super(status + " " + errorType + ": " + detail);
        this.status = status;
        this.errorType = errorType;
        this.detail = detail;
        this.requestId = requestId;
    }

    int status() { return status; }
    String errorType() { return errorType; }
    String detail() { return detail; }
    String requestId() { return requestId; }
}
