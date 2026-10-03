/** A raw Messages API client over an injected transport. See ../../statement.md for the contract. */
const val URL = "https://api.anthropic.com/v1/messages"

fun buildRequest(apiKey: String, model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String? = null): Request {
    // TODO: method, url, the three headers and the JSON body, as the statement says.
    return Request("GET", "", emptyMap(), "{}")
}

fun sendMessages(transport: Transport, apiKey: String, model: String, messages: List<Map<String, Any?>>, maxTokens: Int, system: String? = null): Map<String, Any?> {
    // TODO: build the request, call transport.send(request), return the parsed message or throw ApiError.
    return emptyMap()
}

fun textOf(message: Map<String, Any?>): String {
    // TODO: the text of the text blocks, joined.
    return ""
}
