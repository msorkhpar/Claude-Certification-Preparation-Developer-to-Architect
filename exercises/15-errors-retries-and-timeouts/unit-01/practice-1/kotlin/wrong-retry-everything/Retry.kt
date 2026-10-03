/** A retry policy for API calls. See ../../statement.md for the contract. */
private fun error(response: Response): Map<*, *> = response.body["error"] as? Map<*, *> ?: emptyMap<Any, Any>()

private fun retryable(response: Response): Boolean {
    val s = response.status
    if (s >= 400) {
        val details = error(response)["details"] as? Map<*, *>
        return details?.get("error_code") != "enforced_spend_limit_reached"
    }
    return false
}

private fun failure(response: Response, attempts: Int) =
    CallFailed(response.status, error(response)["type"]?.toString() ?: "unknown", attempts, response.headers["request-id"])

private fun retryAfter(response: Response): Double = response.headers["retry-after"]?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

fun callWithRetry(send: () -> Response, sleep: (Double) -> Unit, policy: Policy = Policy()): Response {
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
            if (attempt >= policy.maxAttempts) throw CallFailed(0, "connection_error", attempt)
        }
        val delay = policy.jitter(minOf(policy.cap, policy.baseDelay * Math.pow(2.0, (attempt - 1).toDouble())))
        sleep(maxOf(delay, wait))
    }
}
