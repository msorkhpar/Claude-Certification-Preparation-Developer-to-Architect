/** Human review without fooling yourself: accuracy by segment, the decision to automate, a calibrated confidence threshold, a stratified sample, review routing within capacity, and checkpoints for irreversible actions. See ../../statement.md. */

private val log = System.getLogger("review_routing")

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
    log.log(System.Logger.Level.DEBUG, "accuracyBy input: {0}", records)
    val groups = records.groupBy { "${it.docType}/${it.field}" }.toSortedMap()
    val correct = records.count { it.correct }
    val total = records.size
    // TODO 1 of 7 (finish this to pass m1, e1): the segment rows. After the overall row, add one row for each segment
    //   ("doc_type/field"), sorted by name, with its correct count, its total and the rounded percent. Example:
    //   invoice/total 8 of 10 -> {segment: invoice/total, correct 8, total 10, percent 80}.
    return listOf(Seg("overall", correct, total, if (total > 0) percent(correct, total) else 0)) +
        groups.map { (name, rs) -> Seg(name, rs.count { it.correct }, rs.size, if (total > 0) percent(correct, total) else 0) }
}

fun canAutomate(records: List<Rec>, threshold: Int, minN: Int): Automation {
    val segments = accuracyBy(records).drop(1)
    // TODO 2 of 7 (finish this to pass e2): the sort of the segments. For each segment (not the overall row): when its
    //   total is below min_n it is undersampled; otherwise when its percent is below the threshold it is failing. Example:
    //   min_n 5, segment with 4 records -> undersampled; 5 records at 70 percent, threshold 90 -> failing.
    val undersampled = emptyList<String>()
    val failing = emptyList<String>()
    return Automation(segments.isNotEmpty() && failing.isEmpty() && undersampled.isEmpty(), failing, undersampled)
}

fun calibrateThreshold(labeled: List<Labeled>, target: Int): Int? {
    // TODO 3 of 7 (finish this to pass e3, e4): the threshold. Receives the labelled items (confidence, correct) and the
    //   target precision in percent. Try each distinct confidence from the lowest; return the first for which the items at
    //   or above it are right at least target percent of the time; return none when no level does. Example: target 90 and
    //   no level reaches it -> none.
    return 0
}

// TODO 4 of 7 (finish this to pass e5): the sample of one stratum. `members` are the stratum's items sorted by rank then
//   id. Take only the first per_stratum of them and add their ids. Example: 5 items, per_stratum 2 -> the 2 best ranked.
fun stratifiedSample(items: List<Item>, perStratum: Int): List<String> =
    items.map { it.stratum }.distinct().flatMap { stratum -> items.filter { it.stratum == stratum }.sortedWith(compareBy({ it.rank }, { it.id })).map { it.id } }

fun route(extractions: List<Extraction>, threshold: Int, capacity: Int): Routing {
    // TODO 5 of 7 (finish this to pass e6): the review queue. Keep the extractions that have a conflict or a confidence
    //   below the threshold, ordered with conflicts first, then by confidence (lowest first), then by id. Example:
    //   threshold 80, a conflict, a 60 and a 90 -> the conflict, then the 60.
    val queue = extractions.map { it.id }
    val flagged = queue.toSet()
    // TODO 6 of 7 (finish this to pass e7): the capacity cut. Split the queue: the first `capacity` ids go to review,
    //   the others to the backlog, both in order. Example: queue [a, b, c], capacity 2 -> review [a, b], backlog [c].
    return Routing(queue, emptyList(), extractions.filter { it.id !in flagged }.map { it.id })
}

// TODO 7 of 7 (finish this to pass e8): the checkpoint. Receives the action, the amount and the limit. Return human when
//   the action is in IRREVERSIBLE or the amount is above the limit, otherwise auto. Example: close_account for 10 ->
//   human.
fun checkpoint(action: String, amount: Int, limit: Int = 1000): String = "auto"
