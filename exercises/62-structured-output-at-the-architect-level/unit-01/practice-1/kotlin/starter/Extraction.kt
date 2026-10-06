private val log = System.getLogger("extraction")
/** An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md. Results are JSON-like maps. */

val CURRENCIES = listOf("USD", "EUR", "GBP", "other", "unclear")
private val KEYS = listOf("vendor", "currency", "currency_detail", "line_items", "stated_total", "calculated_total", "conflict_detected", "provenance")
private val RETRYABLE = listOf("syntax", "semantic", "ungrounded")

/** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
private val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

private fun round2(x: Double): Double = Math.round(x * 100) / 100.0

private fun err(kind: String, field: String, message: String): Map<String, Any?> = mapOf("kind" to kind, "field" to field, "message" to message)

private fun quoteFound(quote: Any?, document: String): Boolean {
    // TODO 1 of 10 (finish this to pass e1): is a provenance quote real?
    // Receives the quote the model gave for a field and the document text. Returns true only when the quote is a non-empty String that
    // appears in the document, so an invented value cannot be supported by an invented quote.
    // Example: quoteFound("Acme Ltd", "Invoice from Acme Ltd") -> true, quoteFound("Zed Corp", "Invoice from Acme Ltd") -> false
    return true
}

private fun currencyOk(currency: Any?): Boolean {
    // TODO 2 of 10 (finish this to pass e6): is this a currency the schema allows?
    // Receives the value of "currency". Returns true when it is one of CURRENCIES ("unclear" and "other" count, anything else does not).
    // Example: currencyOk("unclear") -> true, currencyOk("dollars") -> false
    return true
}

private fun detailMissing(currency: Any?, detail: Any?): Boolean {
    // TODO 3 of 10 (finish this to pass e6): is a required currency detail missing?
    // Receives "currency" and "currency_detail". Returns true when the currency is "other" and the detail is not a non-blank String.
    // Example: detailMissing("other", "  ") -> true, detailMissing("other", "CHF") -> false, detailMissing("USD", null) -> false
    return false
}

private fun checkSemantics(record: Map<String, Any?>, errors: MutableList<Map<String, Any?>>) {
    // TODO 10 of 10 (finish this to pass e5): report what a schema cannot check about the numbers.
    // Receives a record whose types are already valid and the `errors` list; add errors with errors += err("semantic", field, message).
    // Add one on "calculated_total" when it is not the sum of "line_items" (to half a cent), and one on "stated_total" when it is not null,
    // differs from calculated_total (by more than half a cent) and "conflict_detected" is false; a conflict the model flagged is
    // information, not an error. It returns nothing.
    // Example: line_items [10, 5], calculated_total 15, stated_total 20, conflict_detected false -> one semantic error on stated_total
}

@Suppress("UNCHECKED_CAST")
fun validate(record: Map<String, Any?>, document: String, required: List<String> = emptyList()): List<Map<String, Any?>>? {
    log.log(System.Logger.Level.DEBUG, "validate input: {0}", record)
    val errors = mutableListOf<Map<String, Any?>>()
    for (key in KEYS) if (key !in record) errors += err("syntax", key, "is missing")
    if (errors.isNotEmpty()) return errors
    if (record["vendor"] != null && record["vendor"] !is String) errors += err("syntax", "vendor", "must be a string or null")
    if (!currencyOk(record["currency"])) errors += err("syntax", "currency", "'${record["currency"]}' is not one of $CURRENCIES")
    val detail = record["currency_detail"]
    if (detailMissing(record["currency"], detail)) errors += err("syntax", "currency_detail", "is required when the currency is other")
    val items = record["line_items"]
    if (!(items is List<*> && items.all { it is Number })) errors += err("syntax", "line_items", "must be a list of numbers")
    if (record["stated_total"] != null && record["stated_total"] !is Number) errors += err("syntax", "stated_total", "must be a number or null")
    if (record["calculated_total"] !is Number) errors += err("syntax", "calculated_total", "must be a number")
    if (record["conflict_detected"] !is Boolean) errors += err("syntax", "conflict_detected", "must be true or false")
    if (record["provenance"] !is Map<*, *>) errors += err("syntax", "provenance", "must be an object")
    if (errors.isNotEmpty()) return errors
    checkSemantics(record, errors)
    val provenance = record["provenance"] as Map<String, Any?>
    for (field in listOf("vendor", "currency", "stated_total")) {
        val value = record[field]
        if (value == null || value == "unclear") continue
        val quote = provenance[field]
        if (!quoteFound(quote, document)) errors += err("ungrounded", field, "$field has no quote that appears in the document")
    }
    for (field in required) {
        val value = record[field]
        if (value == null || value == "unclear") errors += err("absent", field, "the document gave no value")
    }
    return errors
}

private fun retryableErrors(errors: List<Map<String, Any?>>): List<Map<String, Any?>> {
    // TODO 4 of 10 (finish this to pass e2, e3 and e4): keep the errors a second look can fix.
    // Receives a list of {kind, field, message} errors. Returns those whose kind is in RETRYABLE (syntax, semantic, ungrounded), in order;
    // an "absent" error is never retried. Example: [{kind=absent}, {kind=syntax}] -> [{kind=syntax}]
    return emptyList()
}

private fun status(errors: List<Map<String, Any?>>, record: Map<String, Any?>): String {
    // TODO 5 of 10 (finish this to pass m1, e3 and e5): the status of a finished extraction.
    // Receives the errors left after the last attempt and the last record. Returns "needs_review" when the only errors are "absent" ones, or
    // when there are no errors and the model flagged a conflict ("conflict_detected"); "failed" for any other error; "valid" otherwise.
    // Example: no errors and conflict_detected false -> "valid"; one absent error -> "needs_review"; one syntax error -> "failed"
    return "failed"
}

fun extractDocument(document: String, callModel: (String, Map<String, Any?>?) -> Map<String, Any?>, required: List<String> = emptyList(), maxRetries: Int = 2): Map<String, Any?>? {
    var feedback: Map<String, Any?>? = null
    var attempts = 0
    var record: Map<String, Any?>
    var errors: List<Map<String, Any?>>
    while (true) {
        attempts++
        record = callModel(document, feedback)
        errors = validate(record, document, required)!!
        if (errors.isEmpty()) break
        val retryable = retryableErrors(errors)
        if (retryable.isEmpty()) break
        if (attempts > maxRetries) break
        feedback = mapOf("previous" to record, "errors" to retryable)
    }
    val status = status(errors, record)
    return mapOf("status" to status, "record" to record, "attempts" to attempts, "errors" to errors)
}

private fun isUnset(value: Any?): Boolean {
    // TODO 6 of 10 (finish this to pass e7): has a merged field no real value yet?
    // Receives a value. Returns true for null and for "unclear", so the first real value from a later chunk is kept.
    // Example: isUnset(null) -> true, isUnset("unclear") -> true, isUnset("Acme Ltd") -> false
    return false
}

private fun isConflict(current: Any?, value: Any?, field: String, conflicts: List<Any?>): Boolean {
    // TODO 7 of 10 (finish this to pass e7): do two chunks disagree about a field, not yet recorded?
    // Receives the value kept so far, a later chunk's value, the field name and the fields already in `conflicts`. Returns true when the
    // values differ and the field is not yet in that list, so a conflict is recorded once.
    // Example: isConflict("A", "B", "vendor", emptyList()) -> true
    return false
}

@Suppress("UNCHECKED_CAST")
fun mergeChunks(records: List<Map<String, Any?>>): Map<String, Any?>? {
    val provenance = LinkedHashMap<String, Any?>()
    val lineItems = mutableListOf<Any?>()
    val conflicts = mutableListOf<Any?>()
    val merged = linkedMapOf<String, Any?>("vendor" to null, "currency" to "unclear", "currency_detail" to null, "line_items" to lineItems, "stated_total" to null, "calculated_total" to 0.0,
        "conflict_detected" to false, "provenance" to provenance, "conflicts" to conflicts)
    for (record in records) {
        for (field in listOf("vendor", "currency", "stated_total")) {
            val value = record[field]
            if (value == null || value == "unclear") continue
            val current = merged[field]
            if (isUnset(current)) {
                merged[field] = value
                provenance[field] = (record["provenance"] as Map<String, Any?>)[field]
                if (field == "currency") merged["currency_detail"] = record["currency_detail"]
            } else if (isConflict(current, value, field, conflicts)) {
                conflicts.add(field)
            }
        }
        (record["line_items"] as List<Any?>?)?.let { lineItems.addAll(it) }
        if (record["conflict_detected"] == true) merged["conflict_detected"] = true
    }
    merged["calculated_total"] = round2(lineItems.sumOf { (it as Number).toDouble() })
    if (conflicts.isNotEmpty()) merged["conflict_detected"] = true
    return merged
}

private fun report(correct: Int, valid: Int, total: Int): Map<String, Any?> {
    // TODO 8 of 10 (finish this to pass e8): the accuracy report, on every document and on the validated ones.
    // Receives the number of correct documents, the number of valid documents and the number of labelled documents. Returns a map with
    // all_documents (correct over total), validated_only (correct over valid), validated (= valid) and total; the two rates are rounded
    // with round2 and are 0.0 when the denominator is zero. Example: report(3, 6, 6) -> all_documents 0.5, validated_only 0.5
    return mapOf("all_documents" to 0.0, "validated_only" to 0.0, "validated" to valid, "total" to total)
}

@Suppress("UNCHECKED_CAST")
fun accuracy(results: Map<String, Map<String, Any?>>, labels: Map<String, Map<String, Any?>>): Map<String, Any?>? {
    var valid = 0
    var correct = 0
    for ((id, label) in labels) {
        val result = results[id]
        val isValid = result != null && result["status"] == "valid"
        if (isValid) valid++
        if (isValid) {
            val record = result!!["record"] as Map<String, Any?>
            if (record["vendor"] == label["vendor"] && (record["stated_total"] as Number).toDouble() == (label["stated_total"] as Number).toDouble()) correct++
        }
    }
    val total = labels.size
    return report(correct, valid, total)
}

private fun forcedChoice(tools: List<String>, forced: String?): Map<String, Any?> {
    // TODO 9 of 10 (finish this to pass e9): the tool_choice for a model that accepts a forced choice.
    // Receives the list of tool names and the name to force, or null. Returns a map with tool_choice, strict true and verify_reply false:
    // the forced tool when `forced` is given (type tool, name forced), type any when there are several tools, otherwise the one tool by name.
    // Example: (listOf("a", "b"), null) -> {tool_choice={type=any}, strict=true, verify_reply=false}
    return mapOf("tool_choice" to mapOf("type" to "auto"), "strict" to true, "verify_reply" to false)
}

fun requestChoice(model: String, tools: List<String>, forced: String? = null): Map<String, Any?>? {
    if (model in NO_FORCING) return mapOf("tool_choice" to mapOf("type" to "auto"), "strict" to true, "verify_reply" to true)
    return forcedChoice(tools, forced)
}
