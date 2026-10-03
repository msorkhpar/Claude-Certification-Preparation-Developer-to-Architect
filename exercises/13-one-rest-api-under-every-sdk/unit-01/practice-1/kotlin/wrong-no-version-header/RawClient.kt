/** A raw Messages API client over an injected transport. See ../../statement.md for the contract. */
const val URL = "https://api.anthropic.com/v1/messages"

private fun present(s: String?) = s != null && s.isNotBlank()

fun buildRequest(apiKey: String, model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String? = null): Request {
    require(present(apiKey)) { "apiKey is required" }
    require(messages.isNotEmpty()) { "messages must not be empty" }
    require(maxTokens >= 1) { "maxTokens must be at least 1" }
    val body = linkedMapOf<String, Any?>("model" to model, "max_tokens" to maxTokens.toLong(), "messages" to messages)
    if (present(system)) body["system"] = system
    val headers = linkedMapOf("x-api-key" to apiKey, "content-type" to "application/json")
    return Request("POST", URL, headers, Json.stringify(body))
}

private fun errorFrom(response: Response, apiKey: String): ApiError {
    var requestId = response.headers["request-id"]
    var kind = "unknown"
    var message = response.body.trim().take(200)
    try {
        val parsed = Json.parse(response.body)
        val error = (parsed as? Map<*, *>)?.get("error") as? Map<*, *>
        if (error != null) {
            kind = error["type"]?.toString() ?: "unknown"
            message = error["message"]?.toString() ?: ""
            if (requestId == null) requestId = (parsed as Map<*, *>)["request_id"]?.toString()
        }
    } catch (notJson: IllegalArgumentException) {
        // keep the raw text
    }
    return ApiError(response.status, kind, message.replace(apiKey, "[redacted]"), requestId)
}

@Suppress("UNCHECKED_CAST")
fun sendMessages(transport: Transport, apiKey: String, model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String? = null): Map<String, Any?> {
    val request = buildRequest(apiKey, model, messages, maxTokens, system)
    val response = transport.send(request)
    if (response.status in 200..299) return Json.parse(response.body) as Map<String, Any?>
    throw errorFrom(response, apiKey)
}

fun textOf(message: Map<String, Any?>): String =
    (message["content"] as? List<*>).orEmpty()
        .filterIsInstance<Map<*, *>>()
        .filter { it["type"] == "text" }
        .joinToString("") { it["text"].toString() }
