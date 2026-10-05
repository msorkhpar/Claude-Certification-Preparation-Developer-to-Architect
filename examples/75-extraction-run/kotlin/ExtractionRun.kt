/**
 * An extraction run in miniature: a scripted model reads six documents, every record is validated for what a schema cannot check, only the errors a second look can fix
 * are retried, what the document does not hold goes to a person, and the accuracy is reported on every document and not only on the validated ones.
 *
 * The model is a table of made-up replies: this example is about what the pipeline does with a record, not about what a model writes. Amounts are in cents. The shapes
 * (a record, an error with a kind and a field, the status of a document) are this course's design, not an Anthropic interface.
 */
data class Doc(val kind: String, val text: String)

data class Rec(val vendor: String?, val lines: List<Int>, val total: Int?, val conflict: Boolean = false)

data class Err(val kind: String, val field: String)

data class Extracted(val id: String, val attempts: Int, val record: Rec, val errors: List<Err>, val retried: List<String>, val status: String)

val DOCS = linkedMapOf(
    "d1" to Doc("typed", "Vendor: Acme Ltd. Lines: 10.00 20.00. Total: 30.00"),
    "d2" to Doc("typed", "Vendor: Borealis Co. Lines: 40.00 5.00. Total: 45.00"),
    "d3" to Doc("scanned", "Lines: 8.00 2.00. Total: 10.00"),
    "d4" to Doc("scanned", "Vendor: Corvid Inc. Lines: 12.00 8.00."),
    "d5" to Doc("handwritten", "Vendor: Dunmore. Lines: 6.00 6.00. Total: 12.50"),
    "d6" to Doc("handwritten", "Lines: 3.00. Total: 3.00"),
)
val LABELS = mapOf("d1" to ("Acme Ltd" to 3000), "d2" to ("Borealis Co" to 4500), "d3" to (null to 1000), "d4" to ("Corvid Inc" to 2000), "d5" to ("Dunmore" to 1200), "d6" to (null to 300))

// what the scripted model returns on the first and on the second attempt (a missing second reply repeats the first)
val REPLIES = mapOf(
    "d1" to listOf(Rec("Acme Ltd", listOf(1000, 2000), 3000)),
    "d2" to listOf(Rec("Borealis Co", listOf(4000, 500), 5400), Rec("Borealis Co", listOf(4000, 500), 4500)),
    "d3" to listOf(Rec("Globex", listOf(800, 200), 1000), Rec(null, listOf(800, 200), 1000)),
    "d4" to listOf(Rec("Corvid Inc", listOf(1200, 800), null)),
    "d5" to listOf(Rec("Dunmore", listOf(600, 600), 1250, conflict = true)),
    "d6" to listOf(Rec("Hollis", listOf(300), 300)),
)

/** What a schema cannot check: a vendor the document never names, totals that disagree, a required total that is missing. */
fun validate(record: Rec, text: String): List<Err> {
    val errors = mutableListOf<Err>()
    if (record.vendor != null && record.vendor !in text) errors += Err("ungrounded", "vendor")
    if (record.total == null) errors += Err("absent", "total")
    else if (record.total != record.lines.sum() && !record.conflict) errors += Err("semantic", "total")
    return errors
}

/** One attempt, then one retry that carries the errors, and only when a second look can fix one. An absent value is never retried. */
fun extract(docId: String, text: String): Extracted {
    var retried = listOf<String>()
    val replies = REPLIES.getValue(docId)
    var record = replies[0]
    var errors = listOf<Err>()
    for (attempt in 1..2) {
        record = replies[minOf(attempt, replies.size) - 1]
        errors = validate(record, text)
        val fixable = errors.filter { it.kind != "absent" }
        if (fixable.isEmpty()) return Extracted(docId, attempt, record, errors, retried, if (errors.isNotEmpty() || record.conflict) "needs_review" else "valid")
        retried = fixable.map { it.kind }.toSortedSet().toList()
    }
    return Extracted(docId, 2, record, errors, retried, "failed")
}

fun percent(correct: Int, total: Int): Int = if (total > 0) (200 * correct + total) / (2 * total) else 0

fun main() {
    val results = DOCS.map { (docId, doc) ->
        val r = extract(docId, doc.text)
        val label = LABELS.getValue(docId)
        Triple(r, doc.kind, r.status == "valid" && r.record.vendor == label.first && r.record.total == label.second)
    }
    for ((r, kind, _) in results) {
        var note = if (r.status == "valid" && r.retried.isNotEmpty()) " (retried: ${r.retried.joinToString(", ")})" else ""
        if (r.status == "needs_review") note = if (r.record.conflict) " (conflict flagged)" else " (${r.errors[0].kind}: ${r.errors[0].field}, not retried)"
        if (r.status == "failed") note = " (${r.retried.joinToString(", ")})"
        println("${r.id} $kind: ${r.status} after ${r.attempts} attempt${if (r.attempts > 1) "s" else ""}$note")
    }
    val valid = results.filter { it.first.status == "valid" }
    val rightValid = valid.count { it.third }
    val right = results.count { it.third }
    println("accuracy: validated only $rightValid of ${valid.size} (${percent(rightValid, valid.size)}%), all documents $right of ${results.size} (${percent(right, results.size)}%)")
    val kinds = results.map { it.second }.distinct()
    println("by kind: " + kinds.joinToString(", ") { k -> "$k ${results.count { it.second == k && it.third }}/${results.count { it.second == k }}" })
    val ready = kinds.filter { k -> results.count { it.second == k } >= 2 && results.filter { it.second == k }.all { it.third } }
    println("automate: " + ready.joinToString(", ").ifEmpty { "none" })
}
