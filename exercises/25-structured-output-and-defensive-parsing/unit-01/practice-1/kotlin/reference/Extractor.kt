private val log = System.getLogger("extractor")

/** Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md. JSON values are Map, List, String, Long, Double, Boolean or null. */

/** No JSON object could be read from the model's text. */
class ParseError(message: String) : RuntimeException(message)

typealias Ask = (List<Map<String, Any?>>) -> Map<String, Any?>

/** The index of the first { and of the last } in the text; -1 for a brace that is not there. */
private fun objectSpan(body: String): Pair<Int, Int> = Pair(body.indexOf('{'), body.lastIndexOf('}'))

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
private fun isInteger(v: Any?): Boolean = v is Long || v is Int || v is Short || v is Byte || (v is Double && !v.isInfinite() && !v.isNaN() && v == Math.rint(v))

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
    val errors = mutableListOf<Map<String, Any?>>()
    if (schema.containsKey("minimum") && value.toDouble() < (schema["minimum"] as Number).toDouble()) errors += problem(path, "must be at least ${schema["minimum"]}")
    if (schema.containsKey("maximum") && value.toDouble() > (schema["maximum"] as Number).toDouble()) errors += problem(path, "must be at most ${schema["maximum"]}")
    return errors
}

/** The problems of missing required keys. */
@Suppress("UNCHECKED_CAST")
private fun requiredErrors(schema: Map<String, Any?>, value: Map<*, *>, path: String): List<Map<String, Any?>> =
    ((schema["required"] as? List<Any?>) ?: emptyList()).filter { !value.containsKey(it) }.map { problem("$path.$it", "is required") }

/** The problems of keys the schema does not list, when additionalProperties is false. */
@Suppress("UNCHECKED_CAST")
private fun extraErrors(schema: Map<String, Any?>, value: Map<*, *>, path: String): List<Map<String, Any?>> {
    if (schema["additionalProperties"] != false) return emptyList()
    val properties = (schema["properties"] as? Map<String, Any?>) ?: emptyMap()
    return value.keys.filter { it !in properties }.map { problem("$path.$it", "is not allowed") }
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
private fun feedback(errors: List<Map<String, Any?>>): String =
    "Your reply was rejected:\n" + errors.joinToString("") { "- ${it["path"]}: ${it["message"]}\n" } + "Return the corrected JSON only."

private fun result(status: String, value: Any?, attempts: Int, errors: List<Map<String, Any?>>): Map<String, Any?> =
    linkedMapOf("status" to status, "value" to value, "attempts" to attempts, "errors" to errors)

/** The quotes the document does not contain. */
private fun groundingErrors(value: Any?, document: String, evidenceFields: List<String>): List<Map<String, Any?>> {
    val errors = mutableListOf<Map<String, Any?>>()
    for (name in evidenceFields) {
        val quoted = (value as? Map<*, *>)?.get(name)
        if (quoted is String && !document.contains(quoted)) errors += problem("$.$name", "is not found in the document")
    }
    return errors
}

/** "refused" or "truncated" for a reply that must not be retried, else null. */
private fun earlyStatus(reply: Map<String, Any?>): String? = when (reply["stop_reason"]) {
    "refusal" -> "refused"
    "max_tokens" -> "truncated"
    else -> null
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
