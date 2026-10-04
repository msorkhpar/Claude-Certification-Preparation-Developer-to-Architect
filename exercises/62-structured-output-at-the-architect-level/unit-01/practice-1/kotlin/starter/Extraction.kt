/** An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md. Results are JSON-like maps. */

val CURRENCIES = listOf("USD", "EUR", "GBP", "other", "unclear")

/** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
@Suppress("unused")
val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

fun validate(record: Map<String, Any?>, document: String, required: List<String> = emptyList()): List<Map<String, Any?>>? {
    // TODO: the errors of a record, each a map with kind, field and message: syntax, semantic, ungrounded and absent.
    return null
}

fun extractDocument(document: String, callModel: (String, Map<String, Any?>?) -> Map<String, Any?>, required: List<String> = emptyList(), maxRetries: Int = 2): Map<String, Any?>? {
    // TODO: call the model, validate, retry with feedback only for errors a second look can fix, and report a status.
    return null
}

fun mergeChunks(records: List<Map<String, Any?>>): Map<String, Any?>? {
    // TODO: merge the records of the chunks of one long document, keeping the first value and recording a conflict.
    return null
}

fun accuracy(results: Map<String, Map<String, Any?>>, labels: Map<String, Map<String, Any?>>): Map<String, Any?>? {
    // TODO: the share of documents extracted correctly, measured on all of them and on the validated ones only.
    return null
}

fun requestChoice(model: String, tools: List<String>, forced: String? = null): Map<String, Any?>? {
    // TODO: the tool_choice of the request, with the fallback for models that reject a forced choice.
    return null
}
