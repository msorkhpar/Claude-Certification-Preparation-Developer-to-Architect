/** The batch would be refused. field() names the offending part. */
class BatchError extends RuntimeException {
    private final String field;

    BatchError(String field, String reason) {
        super(field + ": " + reason);
        this.field = field;
    }

    String field() {
        return field;
    }
}
