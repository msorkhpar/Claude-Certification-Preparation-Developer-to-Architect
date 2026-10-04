/** Human review without fooling yourself: accuracy by segment, the decision to automate, a calibrated confidence threshold, a stratified sample, review routing within capacity, and checkpoints for irreversible actions. See ../../statement.md. */

val IRREVERSIBLE = listOf("delete_records", "send_payment", "close_account")

data class Rec(val docType: String, val field: String, val correct: Boolean)

data class Seg(val segment: String, val correct: Int, val total: Int, val percent: Int)

data class Automation(val automate: Boolean, val failing: List<String>, val undersampled: List<String>)

data class Labeled(val confidence: Int, val correct: Boolean)

data class Item(val id: String, val stratum: String, val rank: Int)

data class Extraction(val id: String, val confidence: Int, val conflict: Boolean)

data class Routing(val review: List<String>, val backlog: List<String>, val auto: List<String>)

private fun percent(correct: Int, total: Int): Int = (200 * correct + total) / (2 * total)

fun accuracyBy(records: List<Rec>): List<Seg> {
    val groups = records.groupBy { "${it.docType}/${it.field}" }.toSortedMap()
    val correct = records.count { it.correct }
    val total = records.size
    return listOf(Seg("overall", correct, total, if (total > 0) percent(correct, total) else 0)) +
        groups.map { (name, rs) -> Seg(name, rs.count { it.correct }, rs.size, percent(rs.count { it.correct }, rs.size)) }
}

fun canAutomate(records: List<Rec>, threshold: Int, minN: Int): Automation {
    val segments = accuracyBy(records).drop(1)
    val undersampled = segments.filter { it.total < minN }.map { it.segment }
    val failing = segments.filter { it.total >= minN && it.percent < threshold }.map { it.segment }
    return Automation(segments.isNotEmpty() && failing.isEmpty() && undersampled.isEmpty(), failing, undersampled)
}

fun calibrateThreshold(labeled: List<Labeled>, target: Int): Int? {
    for (t in labeled.map { it.confidence }.toSortedSet()) {
        val kept = labeled.filter { it.confidence >= t }
        if (100 * kept.count { it.correct } >= target * kept.size) return t
    }
    return null
}

fun stratifiedSample(items: List<Item>, perStratum: Int): List<String> =
    items.map { it.stratum }.distinct().flatMap { stratum -> items.filter { it.stratum == stratum }.sortedWith(compareBy({ it.rank }, { it.id })).take(perStratum).map { it.id } }

fun route(extractions: List<Extraction>, threshold: Int, capacity: Int): Routing {
    val queue = extractions.filter { it.conflict || it.confidence < threshold }.sortedWith(compareBy({ if (it.conflict) 0 else it.confidence }, { it.id })).map { it.id }
    val flagged = queue.toSet()
    return Routing(queue.take(capacity), queue.drop(capacity), extractions.filter { it.id !in flagged }.map { it.id })
}

fun checkpoint(action: String, amount: Int, limit: Int = 1000): String = if (action in IRREVERSIBLE || amount > limit) "human" else "auto"
