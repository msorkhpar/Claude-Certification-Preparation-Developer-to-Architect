private val log = System.getLogger("extractor")

/** Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md. JSON values are Map, List, String, Long, Double, Boolean or null. */

/** No JSON object could be read from the model's text. */
class ParseError(message: String) : RuntimeException(message)

typealias Ask = (List<Map<String, Any?>>) -> Map<String, Any?>

/** The index of the first { and of the last } in the text; -1 for a brace that is not there. */
private fun objectSpan(body: String): Pair<Int, Int> {
    // TODO 1 of 8 (finish this to pass m1, e1 and e3): where the JSON object sits in the text.
    // Receives the text. Returns Pair(first, last): the index of the first '{' and of the last '}', or -1 for a brace that is not there.
    // Example: objectSpan("Sure! {\"a\": 1} Done.") -> Pair(6, 13)
    return Pair(-1, -1)
}

/** The JSON value in a model reply: the body of a code fence, else the span from the first { to the last }. */
fun parseJson(text: String): Any? {
    log.log(System.Logger.Level.DEBUG, "parseJson input: {0}", text)
    var body = text
    val fence = text.indexOf("```")
    if (fence != -1) {
        val start = text.indexOf('\n', fence)
        val end = text.indexOf("```", if (start != -1) start else fence + 3)
        if (start != -1 && end != -1) body = text.substring(start + 1, end)
    }
    val (first, last) = objectSpan(body)
    if (first == -1 || last < first) throw ParseError("no JSON object found in the reply")
    try {
        return Json.parse(body.substring(first, last + 1))
    } catch (e: IllegalArgumentException) {
        throw ParseError("invalid JSON: ${e.message}")
    }
}

/** Is this value an integer for the schema? A whole number, or a Double with no fraction; never a Boolean. */
private fun isInteger(v: Any?): Boolean {
    // TODO 2 of 8 (finish this to pass e6): is this value an integer for the schema?
    // Receives any value. Returns true for a Long, Int, Short or Byte and for a Double with no fraction; false otherwise (2.5, a Boolean).
    // Example: isInteger(2.0) -> true, isInteger(2.5) -> false, isInteger(true) -> false
    return false
}

private fun typeMatches(type: String, v: Any?): Boolean = when (type) {
    "string" -> v is String
    "integer" -> isInteger(v)
    "number" -> v is Number
    "boolean" -> v is Boolean
    "array" -> v is List<*>
    "object" -> v is Map<*, *>
    "null" -> v == null
    else -> throw IllegalArgumentException("unknown type $type")
}

private fun problem(path: String, message: String): Map<String, Any?> = linkedMapOf("path" to path, "message" to message)

/** The problems of a number outside minimum and maximum. */
private fun rangeErrors(schema: Map<String, Any?>, value: Number, path: String): List<Map<String, Any?>> {
    // TODO 3 of 8 (finish this to pass e2): the problems of a number outside minimum and maximum.
    // Receives the schema, a number and its path. Returns problems {path, message}: "must be at least <minimum>" first, then
    // "must be at most <maximum>", each only when the schema has that key and the value breaks it.
    // Example: rangeErrors(mapOf("minimum" to 0L), -1, "$.total") -> [{path=$.total, message=must be at least 0}]
    return emptyList()
}

/** The problems of missing required keys. */
private fun requiredErrors(schema: Map<String, Any?>, value: Map<*, *>, path: String): List<Map<String, Any?>> {
    // TODO 4 of 8 (finish this to pass e2): the problems of missing required keys.
    // Receives the schema, an object and its path. Returns one {path: "<path>.<key>", message: "is required"} per key of the schema's
    // "required" list that the object lacks, in the schema's order.
    // Example: requiredErrors(mapOf("required" to listOf("a")), mapOf<String, Any?>(), "$") -> [{path=$.a, message=is required}]
    return emptyList()
}

/** The problems of keys the schema does not list, when additionalProperties is false. */
private fun extraErrors(schema: Map<String, Any?>, value: Map<*, *>, path: String): List<Map<String, Any?>> {
    // TODO 5 of 8 (finish this to pass e2): the problems of keys the schema does not list.
    // Receives the schema, an object and its path. When "additionalProperties" is false, returns one
    // {path: "<path>.<key>", message: "is not allowed"} per key that is not in "properties"; otherwise an empty list.
    // Example: schema {additionalProperties=false, properties={}}, object {x=1}, path "$" -> [{path=$.x, message=is not allowed}]
    return emptyList()
}

/** One problem {path, message} per way value breaks schema; an empty list when it conforms. */
@Suppress("UNCHECKED_CAST")
fun validate(schema: Map<String, Any?>, value: Any?, path: String = "$"): List<Map<String, Any?>> {
    val errors = mutableListOf<Map<String, Any?>>()
    val type = schema["type"]
    if (type != null && !typeMatches(type as String, value)) return listOf(problem(path, "must be of type $type"))
    if (schema.containsKey("enum") && value !in (schema["enum"] as List<Any?>)) errors += problem(path, "must be one of ${Json.stringify(schema["enum"])}")
    if (value is Number) errors += rangeErrors(schema, value, path)
    if (value is Map<*, *>) {
        errors += requiredErrors(schema, value, path)
        val properties = (schema["properties"] as? Map<String, Any?>) ?: emptyMap()
        for ((key, sub) in properties) if (value.containsKey(key)) errors += validate(sub as Map<String, Any?>, value[key], "$path.$key")
        errors += extraErrors(schema, value, path)
    }
    if (value is List<*> && schema.containsKey("items")) value.forEachIndexed { i, item -> errors += validate(schema["items"] as Map<String, Any?>, item, "$path[$i]") }
    return errors
}

@Suppress("UNCHECKED_CAST")
private fun textOf(reply: Map<String, Any?>): String =
    (reply["content"] as List<Map<String, Any?>>).filter { it["type"] == "text" }.joinToString("") { it["text"] as String }

private fun prompt(document: String, schema: Map<String, Any?>): String =
    "Extract the data from the document as one JSON object that follows this JSON Schema. Reply with the JSON only.\n" +
        "<schema>${Json.stringify(schema)}</schema>\n<document>\n$document\n</document>"

/** The message that sends the problems back to the model. */
private fun feedback(errors: List<Map<String, Any?>>): String {
    // TODO 6 of 8 (finish this to pass e2): the message that sends the problems back to the model.
    // Receives the list of problems. Returns "Your reply was rejected:", then one "- <path>: <message>" line per problem, then
    // "Return the corrected JSON only.", each on its own line.
    // Example: one problem {$.total, must be of type number} -> "Your reply was rejected:\n- $.total: must be of type number\nReturn the corrected JSON only."
    return ""
}

private fun result(status: String, value: Any?, attempts: Int, errors: List<Map<String, Any?>>): Map<String, Any?> =
    linkedMapOf("status" to status, "value" to value, "attempts" to attempts, "errors" to errors)

/** The quotes the document does not contain. */
private fun groundingErrors(value: Any?, document: String, evidenceFields: List<String>): List<Map<String, Any?>> {
    // TODO 7 of 8 (finish this to pass e5): the quotes the document does not contain.
    // Receives the parsed value, the document text and the names of the evidence fields. For each name whose value in the object is a
    // String that does not occur in the document, returns {path: "$.<name>", message: "is not found in the document"}.
    // Example: value {quote=x}, document "abc", fields [quote] -> [{path=$.quote, message=is not found in the document}]
    return emptyList()
}

/** "refused" or "truncated" for a reply that must not be retried, else null. */
private fun earlyStatus(reply: Map<String, Any?>): String? {
    // TODO 8 of 8 (finish this to pass e4): a reply that must not be retried.
    // Receives a reply. Returns "refused" when its stop_reason is "refusal", "truncated" when it is "max_tokens", else null.
    // Example: a reply with stop_reason "max_tokens" -> "truncated"
    return null
}

/** Ask, parse, validate and, on a problem, re-prompt with the errors, at most [maxAttempts] calls. */
fun extract(ask: Ask, document: String, schema: Map<String, Any?>, maxAttempts: Int = 3, evidenceFields: List<String> = emptyList()): Map<String, Any?> {
    val messages = mutableListOf<Map<String, Any?>>(mapOf("role" to "user", "content" to prompt(document, schema)))
    var errors: List<Map<String, Any?>> = emptyList()
    for (attempt in 1..maxAttempts) {
        val reply = ask(messages.toList())
        val early = earlyStatus(reply)
        if (early != null) return result(early, null, attempt, emptyList())
        val text = textOf(reply)
        var value: Any? = null
        try {
            value = parseJson(text)
            val found = validate(schema, value).toMutableList()
            found += groundingErrors(value, document, evidenceFields)
            errors = found
        } catch (e: ParseError) {
            value = null
            errors = listOf(problem("$", e.message ?: "parse error"))
        }
        if (errors.isEmpty()) return result("ok", value, attempt, emptyList())
        if (attempt < maxAttempts) {
            messages += mapOf("role" to "assistant", "content" to text)
            messages += mapOf("role" to "user", "content" to feedback(errors))
        }
    }
    return result("failed", null, maxAttempts, errors)
}
