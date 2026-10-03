/** Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract. */

/** The API would answer 400. [param] names the offending parameter. */
class RejectedRequest(val param: String, reason: String) : RuntimeException("$param: $reason")

fun buildParams(model: String, maxTokens: Int, options: Map<String, Any?> = emptyMap()): Map<String, Any?>? {
    // TODO: return the request parameters for the model, or throw RejectedRequest for a request the API would refuse.
    return null
}
