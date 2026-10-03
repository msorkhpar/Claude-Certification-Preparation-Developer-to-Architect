/** One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md. */

/** The request cannot be built for this platform. [field] names the offending part. */
class PlatformError(val field: String, reason: String) : RuntimeException("$field: $reason")

fun buildRequest(platform: String, model: String, body: Map<String, Any?>, config: Map<String, Any?>): Map<String, Any?>? {
    // TODO: return a map {method, url, headers, body} for the platform, or throw PlatformError.
    return null
}

fun unsupportedFeatures(platform: String, features: List<String>): List<String>? {
    // TODO: return the features of the list that the platform lacks, in the order given.
    return null
}
