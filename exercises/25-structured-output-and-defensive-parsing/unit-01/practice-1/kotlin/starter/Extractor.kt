/** Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md. JSON values are Map, List, String, Long, Double, Boolean or null. */

/** No JSON object could be read from the model's text. */
class ParseError(message: String) : RuntimeException(message)

typealias Ask = (List<Map<String, Any?>>) -> Map<String, Any?>

fun parseJson(text: String): Any? {
    // TODO: the JSON value in a reply: a code fence's body, else the span from the first { to the last }. Throw ParseError when there is none.
    return null
}

fun validate(schema: Map<String, Any?>, value: Any?, path: String = "$"): List<Map<String, Any?>>? {
    // TODO: one {path, message} per way value breaks schema; an empty list when it conforms.
    return null
}

fun extract(ask: Ask, document: String, schema: Map<String, Any?>, maxAttempts: Int = 3, evidenceFields: List<String> = emptyList()): Map<String, Any?>? {
    // TODO: ask, parse, validate and, on a problem, re-prompt with the errors, at most maxAttempts calls.
    return null
}
