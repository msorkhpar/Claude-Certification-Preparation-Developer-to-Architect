/** The request would be rejected with a 400. field() names the offending part. */
class RequestError extends RuntimeException {
    private final String field;

    RequestError(String field, String reason) {
        super(field + ": " + reason);
        this.field = field;
    }

    String field() {
        return field;
    }
}
