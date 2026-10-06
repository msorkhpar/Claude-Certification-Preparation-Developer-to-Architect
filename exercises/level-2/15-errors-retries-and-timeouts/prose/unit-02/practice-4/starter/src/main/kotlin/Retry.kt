/** A retry policy for API calls. See ../../statement.md for the contract. */
private val log = System.getLogger("retry")

private fun error(response: Response): Map<*, *> = response.body["error"] as? Map<*, *> ?: emptyMap<Any, Any>()

private fun statusRetryable(s: Int): Boolean {
    // TODO 1 of 7 (finish this to pass m1 and e1): may a reply with this status be tried again?
    // Receives the HTTP status. Returns true for 408, 409, 429 and every status of 500 and above (529 included); false for every other one.
    // Example: statusRetryable(529) -> true, statusRetryable(404) -> false
    return false
}

private fun spendCap(response: Response): Boolean {
    // TODO 2 of 7 (finish this to pass e4): is this 429 a spend cap rather than a rate limit?
    // Receives the Response. Returns true when its body's error.details.error_code is "enforced_spend_limit_reached" (use the error
    // helper; any level may be missing); retrying cannot succeed then.
    // Example: a body {error={details={error_code=enforced_spend_limit_reached}}} -> true; {} -> false
    return false
}

private fun retryable(response: Response): Boolean = statusRetryable(response.status) && !spendCap(response)

private fun errorType(response: Response): String {
    // TODO 3 of 7 (finish this to pass e1, e4 and e6): the API's error type of a reply.
    // Receives the Response. Returns body.error.type as text, or "unknown" when the body has none.
    // Example: a body {error={type=overloaded_error}} -> "overloaded_error"; {} -> "unknown"
    return ""
}

private fun requestId(response: Response): String? {
    // TODO 4 of 7 (finish this to pass e1, e4 and e6): the request id of a reply.
    // Receives the Response. Returns its request-id header (header names are lower case), or null when there is none.
    // Example: headers {request-id=req_1} -> "req_1"; {} -> null
    return null
}

private fun failure(response: Response, attempts: Int) =
    CallFailed(response.status, errorType(response), attempts, requestId(response))

private fun connectionFailure(attempts: Int): CallFailed {
    // TODO 5 of 7 (finish this to pass e5): the failure when the last attempt lost its connection.
    // Receives the number of attempts made. Returns a CallFailed with status 0, error type "connection_error", that many attempts and no
    // request id. Example: connectionFailure(2) -> CallFailed(0, "connection_error", 2)
    return CallFailed(0, "", attempts)
}

private fun retryAfter(response: Response): Double {
    // TODO 6 of 7 (finish this to pass e2): the wait the server asked for.
    // Receives the Response. Returns its retry-after header as seconds (never below 0); 0.0 when the header is absent or not a number.
    // Example: headers {retry-after=3} -> 3.0; {} -> 0.0; {retry-after=soon} -> 0.0
    return 0.0
}

private fun delayFor(attempt: Int, policy: Policy): Double {
    // TODO 7 of 7 (finish this to pass m1, e3 and e5): the wait before the next attempt.
    // Receives the attempt that just failed (1 for the first) and the policy. Returns
    // jitter(min(cap, baseDelay * 2^(attempt - 1))): the jitter is applied last, after the cap.
    // Example: baseDelay 1.0, cap 5.0, no jitter: attempt 3 -> 4.0, attempt 4 -> 5.0
    return 0.0
}

fun callWithRetry(send: () -> Response, sleep: (Double) -> Unit, policy: Policy = Policy()): Response {
    log.log(System.Logger.Level.DEBUG, "callWithRetry input: {0}", policy)
    var attempt = 0
    while (true) {
        attempt++
        var wait = 0.0
        try {
            val response = send()
            if (response.status < 400) return response
            if (!retryable(response) || attempt >= policy.maxAttempts) throw failure(response, attempt)
            wait = retryAfter(response)
        } catch (e: TransportError) {
            if (attempt >= policy.maxAttempts) throw connectionFailure(attempt)
        }
        sleep(maxOf(delayFor(attempt, policy), wait))
    }
}
