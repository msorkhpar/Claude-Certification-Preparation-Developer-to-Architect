/** The given types. Header names are lower case; body is the parsed JSON object, or an empty map when there is none. */
data class Response(val status: Int, val headers: Map<String, String>, val body: Map<String, Any?>)

/** max_attempts, the first delay, the cap on a delay and a jitter function applied to each computed delay. */
data class Policy(
    val maxAttempts: Int = 4,
    val baseDelay: Double = 0.5,
    val cap: Double = 8.0,
    val jitter: (Double) -> Double = { it },
)

/** The connection failed or timed out: no reply was received. */
class TransportError(message: String) : RuntimeException(message)

/** The call did not succeed. errorType is the API's error type, "connection_error" or "unknown". */
class CallFailed(val status: Int, val errorType: String, val attempts: Int, val requestId: String? = null) :
    RuntimeException("$status $errorType after $attempts attempt(s)")
