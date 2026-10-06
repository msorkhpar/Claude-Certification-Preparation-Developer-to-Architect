/** A raw Messages API client over an injected transport. See ../../statement.md for the contract. */
private val log = System.getLogger("rawClient")

const val URL = "https://api.anthropic.com/v1/messages"

private fun present(s: String?) = s != null && s.isNotBlank()

private fun validate(apiKey: String, messages: List<Map<String, Any?>>, maxTokens: Int) {
    require(present(apiKey)) { "apiKey is required" }
    require(messages.isNotEmpty()) { "messages must not be empty" }
    require(maxTokens >= 1) { "maxTokens must be at least 1" }
}

private fun headersFor(apiKey: String): Map<String, String> =
    linkedMapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01", "content-type" to "application/json")

private fun bodyFor(model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String?): Map<String, Any?> {
    val body = linkedMapOf<String, Any?>("model" to model, "max_tokens" to maxTokens.toLong(), "messages" to messages)
    if (present(system)) body["system"] = system
    return body
}

fun buildRequest(apiKey: String, model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String? = null): Request {
    log.log(System.Logger.Level.DEBUG, "buildRequest input: {0} {1} {2} {3}", model, messages, maxTokens, system)
    validate(apiKey, messages, maxTokens)
    return Request("POST", URL, headersFor(apiKey), Json.stringify(bodyFor(model, messages, maxTokens, system)))
}

private fun errorParts(parsed: Any?, text: String): Pair<String, String> {
    val error = (parsed as? Map<*, *>)?.get("error") as? Map<*, *>
    if (error != null) return Pair(error["type"]?.toString() ?: "unknown", error["message"]?.toString() ?: "")
    return Pair("unknown", text.trim().take(200))
}

private fun pickRequestId(headerId: String?, bodyId: String?): String? = headerId ?: bodyId

private fun redact(message: String, apiKey: String): String = message.replace(apiKey, "[redacted]")

private fun errorFrom(response: Response, apiKey: String): ApiError {
    val parsed = try {
        Json.parse(response.body)
    } catch (notJson: IllegalArgumentException) {
        null
    }
    val (kind, message) = errorParts(parsed, response.body)
    val bodyId = (parsed as? Map<*, *>)?.get("request_id")?.toString()
    return ApiError(response.status, kind, redact(message, apiKey), pickRequestId(response.headers["request-id"], bodyId))
}

@Suppress("UNCHECKED_CAST")
private fun parseMessage(text: String): Map<String, Any?> = Json.parse(text) as Map<String, Any?>

fun sendMessages(transport: Transport, apiKey: String, model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String? = null): Map<String, Any?> {
    val request = buildRequest(apiKey, model, messages, maxTokens, system)
    val response = transport.send(request)
    if (response.status in 200..299) return parseMessage(response.body)
    throw errorFrom(response, apiKey)
}

fun textOf(message: Map<String, Any?>): String =
    (message["content"] as? List<*>).orEmpty()
        .filterIsInstance<Map<*, *>>()
        .filter { it["type"] == "text" }
        .joinToString("") { it["text"].toString() }
