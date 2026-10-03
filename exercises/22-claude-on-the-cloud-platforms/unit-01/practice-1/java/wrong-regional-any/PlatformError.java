/** The request cannot be built for this platform. field() names the offending part. */
class PlatformError extends RuntimeException {
    private final String field;

    PlatformError(String field, String reason) {
        super(field + ": " + reason);
        this.field = field;
    }

    String field() {
        return field;
    }
}
