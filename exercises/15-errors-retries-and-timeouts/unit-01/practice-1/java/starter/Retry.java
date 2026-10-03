/** A retry policy for API calls. See ../../statement.md for the contract. */
final class Retry {
    private Retry() {}

    static Response callWithRetry(Send send, Sleep sleep, Policy policy) {
        // TODO: call send.send() up to policy.maxAttempts() times; sleep between attempts; return the first good Response.
        return null;
    }
}
