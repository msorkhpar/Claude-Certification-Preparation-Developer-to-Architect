/** The given types. Header names are lower case. */
data class Request(val method: String, val url: String, val headers: Map<String, String>, val body: String)

data class Response(val status: Int, val headers: Map<String, String>, val body: String)

fun interface Transport {
    fun send(request: Request): Response
}

/** A non-2xx reply: status, error type, detail and the request id (null when there is none). */
class ApiError(val status: Int, val errorType: String, val detail: String, val requestId: String?) :
    RuntimeException("$status $errorType: $detail")
