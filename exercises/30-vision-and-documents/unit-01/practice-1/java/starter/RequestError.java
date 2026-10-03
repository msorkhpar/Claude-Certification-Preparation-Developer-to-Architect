/** What your code throws, before any call, for a request the API would reject. field() names the part. */
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
