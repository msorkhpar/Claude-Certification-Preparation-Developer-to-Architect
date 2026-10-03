/** The stream carried an error event, or ended before message_stop (errorType "incomplete_stream"). Given. */
class StreamError extends RuntimeException {
    private final String errorType;
    private final String detail;

    StreamError(String errorType, String detail) {
        super(errorType + ": " + detail);
        this.errorType = errorType;
        this.detail = detail;
    }

    String errorType() { return errorType; }
    String detail() { return detail; }
}
