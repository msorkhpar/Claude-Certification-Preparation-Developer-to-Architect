/** An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md. Results are JSON-like maps. */

val CURRENCIES = listOf("USD", "EUR", "GBP", "other", "unclear")
private val KEYS = listOf("vendor", "currency", "currency_detail", "line_items", "stated_total", "calculated_total", "conflict_detected", "provenance")
private val RETRYABLE = listOf("syntax", "semantic", "ungrounded")

/** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
private val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

private fun round2(x: Double): Double = Math.round(x * 100) / 100.0

private fun err(kind: String, field: String, message: String): Map<String, Any?> = mapOf("kind" to kind, "field" to field, "message" to message)

private fun quoteFound(quote: Any?, document: String): Boolean {
    return quote is String && quote.isNotEmpty() && document.contains(quote)
}

private fun currencyOk(currency: Any?): Boolean {
    return currency in CURRENCIES
}

private fun detailMissing(currency: Any?, detail: Any?): Boolean {
    return currency == "other" && !(detail is String && detail.isNotBlank())
}

@Suppress("UNCHECKED_CAST")
private fun checkSemantics(record: Map<String, Any?>, errors: MutableList<Map<String, Any?>>) {
    val items = record["line_items"]
    val sum = (items as List<Number>).sumOf { it.toDouble() }
    val calculated = (record["calculated_total"] as Number).toDouble()
    if (Math.abs(sum - calculated) > 0.005) errors += err("semantic", "calculated_total", "$calculated is not the sum of the line items, $sum")
    val stated = (record["stated_total"] as Number?)?.toDouble()
    if (stated != null && Math.abs(stated - calculated) > 0.005 && record["conflict_detected"] != true) {
        errors += err("semantic", "stated_total", "the stated total $stated differs from the calculated total $calculated but conflict_detected is false")
    }
}

@Suppress("UNCHECKED_CAST")
fun validate(record: Map<String, Any?>, document: String, required: List<String> = emptyList()): List<Map<String, Any?>>? {
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
    return errors.filter { it["kind"] in RETRYABLE }
}

private fun status(errors: List<Map<String, Any?>>, record: Map<String, Any?>): String {
    return when {
        errors.isNotEmpty() -> if (errors.all { it["kind"] == "absent" }) "needs_review" else "failed"
        record["conflict_detected"] == true -> "needs_review"
        else -> "valid"
    }
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
    return value == null || value == "unclear"
}

private fun isConflict(current: Any?, value: Any?, field: String, conflicts: List<Any?>): Boolean {
    return current != value && field !in conflicts
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
    return mapOf("all_documents" to if (total > 0) round2(correct.toDouble() / total) else 0.0, "validated_only" to if (valid > 0) round2(correct.toDouble() / valid) else 0.0, "validated" to valid, "total" to total)
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
    return when {
        forced != null -> mapOf("tool_choice" to mapOf("type" to "tool", "name" to forced), "strict" to true, "verify_reply" to false)
        tools.size > 1 -> mapOf("tool_choice" to mapOf("type" to "any"), "strict" to true, "verify_reply" to false)
        else -> mapOf("tool_choice" to mapOf("type" to "tool", "name" to tools[0]), "strict" to true, "verify_reply" to false)
    }
}

fun requestChoice(model: String, tools: List<String>, forced: String? = null): Map<String, Any?>? {
    if (model in NO_FORCING) return mapOf("tool_choice" to mapOf("type" to "auto"), "strict" to true, "verify_reply" to true)
    return forcedChoice(tools, forced)
}
