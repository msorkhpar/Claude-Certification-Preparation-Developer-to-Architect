/** A retry policy for API calls. See ../../statement.md for the contract. */
private val log = System.getLogger("retry")

private fun error(response: Response): Map<*, *> = response.body["error"] as? Map<*, *> ?: emptyMap<Any, Any>()

private fun statusRetryable(s: Int): Boolean = s == 408 || s == 409 || s == 429 || s >= 500

private fun spendCap(response: Response): Boolean {
    val details = error(response)["details"] as? Map<*, *>
    return details?.get("error_code") == "enforced_spend_limit_reached"
}

private fun retryable(response: Response): Boolean = statusRetryable(response.status) && !spendCap(response)

private fun errorType(response: Response): String = error(response)["type"]?.toString() ?: "unknown"

private fun requestId(response: Response): String? = response.headers["request-id"]

private fun failure(response: Response, attempts: Int) =
    CallFailed(response.status, errorType(response), attempts, requestId(response))

private fun connectionFailure(attempts: Int) = CallFailed(0, "connection_error", attempts)

private fun retryAfter(response: Response): Double = response.headers["retry-after"]?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

private fun delayFor(attempt: Int, policy: Policy): Double =
    policy.jitter(minOf(policy.cap, policy.baseDelay * Math.pow(2.0, (attempt - 1).toDouble())))

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
