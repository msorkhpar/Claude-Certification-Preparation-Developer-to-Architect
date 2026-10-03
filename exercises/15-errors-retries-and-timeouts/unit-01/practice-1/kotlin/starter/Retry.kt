/** A retry policy for API calls. See ../../statement.md for the contract. */
fun callWithRetry(send: () -> Response, sleep: (Double) -> Unit, policy: Policy = Policy()): Response {
    // TODO: call send() up to policy.maxAttempts times; sleep between attempts; return the first good Response.
    return Response(0, emptyMap(), emptyMap())
}
