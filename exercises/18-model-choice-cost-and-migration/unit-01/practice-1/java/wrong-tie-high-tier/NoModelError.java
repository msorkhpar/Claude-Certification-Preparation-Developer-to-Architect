/** No model in the catalog can take the task. */
class NoModelError extends RuntimeException {
    NoModelError(String message) {
        super(message);
    }
}
