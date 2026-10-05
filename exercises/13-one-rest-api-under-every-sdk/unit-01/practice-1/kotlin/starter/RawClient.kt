/** A raw Messages API client over an injected transport. See ../../statement.md for the contract. */
private val log = System.getLogger("rawClient")

const val URL = "https://api.anthropic.com/v1/messages"

private fun present(s: String?) = s != null && s.isNotBlank()

private fun validate(apiKey: String, messages: List<Map<String, Any?>>, maxTokens: Int) {
    // TODO 1 of 8 (finish this to pass e2): refuse bad input before anything is sent.
    // Receives the key, the messages and maxTokens. Throws IllegalArgumentException (use require) when the key is blank (use present),
    // the messages are empty or maxTokens is below 1; otherwise returns nothing.
    // Example: validate("  ", emptyList(), 8) -> IllegalArgumentException
}

private fun headersFor(apiKey: String): Map<String, String> {
    // TODO 2 of 8 (finish this to pass m1): the three request headers.
    // Receives the API key. Returns a map with exactly x-api-key (the key), anthropic-version (2023-06-01) and content-type
    // (application/json). Example: headersFor("k")["anthropic-version"] -> "2023-06-01"
    return emptyMap()
}

private fun bodyFor(model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String?): Map<String, Any?> {
    // TODO 3 of 8 (finish this to pass m1 and e1): the request body as a map.
    // Receives the model, the messages, maxTokens (store it as a Long) and the system text (may be null). Returns a map with model,
    // max_tokens and messages, plus system only when it is present and not blank (use present).
    // Example: bodyFor("m", emptyList(), 8, "  ") -> {model=m, max_tokens=8, messages=[]}
    return emptyMap()
}

fun buildRequest(apiKey: String, model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String? = null): Request {
    log.log(System.Logger.Level.DEBUG, "buildRequest input: {0} {1} {2} {3}", model, messages, maxTokens, system)
    validate(apiKey, messages, maxTokens)
    return Request("POST", URL, headersFor(apiKey), Json.stringify(bodyFor(model, messages, maxTokens, system)))
}

private fun errorParts(parsed: Any?, text: String): Pair<String, String> {
    // TODO 4 of 8 (finish this to pass e4 and e5): the error type and message of an error reply.
    // Receives the parsed JSON body (null when it is not JSON) and the raw body text. For a body shaped like
    // {"error": {"type": ..., "message": ...}} returns that type and message as text; for anything else returns "unknown" and the
    // first 200 characters of the text, trimmed.
    // Example: errorParts(mapOf("error" to mapOf("type" to "x", "message" to "m")), "") -> Pair("x", "m"), errorParts(null, " <html> ") -> Pair("unknown", "<html>")
    return Pair("", "")
}

private fun pickRequestId(headerId: String?, bodyId: String?): String? {
    // TODO 5 of 8 (finish this to pass e4): which request id the error carries.
    // Receives the id from the request-id header and the id from the body, either may be null. Returns the header's when there is one,
    // else the body's, else null. Example: pickRequestId("h", "b") -> "h", pickRequestId(null, "b") -> "b"
    return null
}

private fun redact(message: String, apiKey: String): String {
    // TODO 6 of 8 (finish this to pass e6): keep the key out of the error.
    // Receives the message and the API key. Returns the message with every occurrence of the key replaced by [redacted].
    // Example: redact("bad key sk-1", "sk-1") -> "bad key [redacted]"
    return message
}

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
private fun parseMessage(text: String): Map<String, Any?> {
    // TODO 7 of 8 (finish this to pass e3): the message of a good reply.
    // Receives the response body text. Returns the parsed JSON object (Json.parse gives a Map).
    // Example: parseMessage("{\"stop_reason\": \"end_turn\"}") -> {stop_reason=end_turn}
    return emptyMap()
}

fun sendMessages(transport: Transport, apiKey: String, model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String? = null): Map<String, Any?> {
    val request = buildRequest(apiKey, model, messages, maxTokens, system)
    val response = transport.send(request)
    if (response.status in 200..299) return parseMessage(response.body)
    throw errorFrom(response, apiKey)
}

fun textOf(message: Map<String, Any?>): String {
    // TODO 8 of 8 (finish this to pass e3): the text of a message.
    // Receives a message map with a content list of blocks. Returns the text of the blocks whose type is "text", joined with nothing
    // between them; every other block type is ignored.
    // Example: a message with blocks text "a", tool_use, text "b" -> "ab"
    return ""
}
