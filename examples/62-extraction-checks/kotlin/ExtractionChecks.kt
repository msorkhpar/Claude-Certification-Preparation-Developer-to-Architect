import harness.Show.py

/**
 * What a schema does not give an extraction pipeline: a field the document may lack, checks of meaning, a retry that carries feedback, and an accuracy figure that does not hide the failures.
 *
 * The rules are the exam guide's for tasks 4.3 and 4.4 and the Claude documentation read on 2026-10-03 (structured outputs, "Define tools"): a schema guarantees syntax and not meaning; a field that may be missing from the source is
 * nullable so the model is not pushed to invent a value; a retry helps with format and structure and cannot supply what the source does not hold; a request that forces a tool is rejected by the current models, which use
 * `auto` with strict tool use. The "model" below is a script of fixed replies: it shows the pipeline's decisions, not what a real model would answer.
 */
const val DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 130.00 EUR"
val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

/** What a reply holds: the item amounts, the total and the quotation that shows where the total comes from. */
data class Invoice(val items: List<Double>, val total: Double, val evidence: String) {
    /** The reply as JSON text, in the spacing Python's json.dumps uses, so every language edition prints the same. */
    fun toJson(): String = "{\"items\": ${items.joinToString(", ", "[", "]")}, \"total\": $total, \"evidence\": \"${evidence.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\"}"
}

/** The end of an extraction: its status, the attempts made and the feedback messages sent. */
data class Extraction(val status: String, val attempts: Int, val feedback: List<String>)

/** What a model under pressure does with a purchase order number the document does not contain: a required string gets filled, a nullable one stays null. */
fun scriptedValue(document: String, nullable: Boolean): String? {
    val marker = "PO "
    if (marker in document) return document.split(marker)[1].trim().split(Regex("\\s+"))[0]
    return if (nullable) null else "PO-0000"
}

/** Checks a schema cannot make: the items add up to the total, and the quoted evidence is in the document. */
fun check(record: Invoice, document: String): List<String> {
    val problems = mutableListOf<String>()
    val sum = record.items.fold(0.0) { a, b -> a + b }
    if (Math.abs(sum - record.total) > 0.005) problems += "total: the items add up to $sum, not ${record.total}"
    if (record.evidence !in document) problems += "evidence: this quotation is not in the document"
    return problems
}

/** Ask, check, and ask again with the document, the failed answer and the problems; give up after maxRetries. */
fun extract(document: String, replies: List<Invoice>, maxRetries: Int = 1): Extraction {
    val messages = mutableListOf<String>()
    for ((index, reply) in replies.take(maxRetries + 1).withIndex()) {
        val problems = check(reply, document)
        if (problems.isEmpty()) return Extraction("valid", index + 1, messages)
        messages += "Document:\n$document\nYour answer:\n${reply.toJson()}\nProblems:\n" + problems.joinToString("\n") { "- $it" }
    }
    return Extraction("failed", minOf(replies.size, maxRetries + 1), messages)
}

/** outcomes: (status, correct) per document. The figure on validated records alone leaves out every document that failed. */
fun accuracy(outcomes: List<Pair<String, Boolean>>): Map<String, Double> {
    val valid = outcomes.filter { it.first == "valid" }.map { it.second }
    val right = valid.count { it }
    return linkedMapOf(
        "validated_only" to if (valid.isEmpty()) 0.0 else Math.round(right.toDouble() / valid.size * 100) / 100.0,
        "all_documents" to if (outcomes.isEmpty()) 0.0 else Math.round(right.toDouble() / outcomes.size * 100) / 100.0,
    )
}

/** The tool_choice of an extraction request: any when several schemas fit, the one tool otherwise, auto with strict tool use where forcing is rejected. */
fun requestChoice(model: String, tools: List<String>): Map<String, Any> =
    if (model in NO_FORCING) linkedMapOf("tool_choice" to "auto", "check_reply" to true)
    else linkedMapOf("tool_choice" to if (tools.size > 1) "any" else "tool:${tools[0]}", "check_reply" to false)

fun main() {
    val noPo = "Invoice from Acme Tools. Total due: 130.00 EUR"
    println("purchase order, document without one: required -> ${scriptedValue(noPo, false)} | nullable -> ${scriptedValue(noPo, true) ?: "None"}")
    val wrong = Invoice(listOf(100.0, 20.5), 130.0, "Total due: 130.00 EUR")
    val right = Invoice(listOf(100.0, 20.5, 9.5), 130.0, "Total due: 130.00 EUR")
    val fabricated = right.copy(evidence = "Total due: 130.00 USD")
    val result = extract(DOC, listOf(wrong, right))
    println("answer 1 wrong, answer 2 right: ${result.status} after ${result.attempts} attempts")
    println(result.feedback[0])
    println("two answers that stay wrong: ${extract(DOC, listOf(wrong, fabricated)).status}")
    val outcomes = List(5) { "valid" to true } + listOf("valid" to false) + List(4) { "failed" to false }
    println("accuracy of 10 documents (6 valid, 5 of them right): ${py(accuracy(outcomes))}")
    for (model in listOf("claude-haiku-4-5", "claude-sonnet-5-5")) println("$model, two extraction tools: ${py(requestChoice(model, listOf("extract_invoice", "extract_receipt")))}")
}
