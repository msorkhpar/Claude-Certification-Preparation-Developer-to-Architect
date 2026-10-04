/** Human review without fooling yourself: accuracy by segment, the decision to automate, a calibrated confidence threshold, a stratified sample, review routing within capacity, and checkpoints for irreversible actions. See ../../statement.md. */

val IRREVERSIBLE = listOf("delete_records", "send_payment", "close_account")

data class Rec(val docType: String, val field: String, val correct: Boolean)

data class Seg(val segment: String, val correct: Int, val total: Int, val percent: Int)

data class Automation(val automate: Boolean, val failing: List<String>, val undersampled: List<String>)

data class Labeled(val confidence: Int, val correct: Boolean)

data class Item(val id: String, val stratum: String, val rank: Int)

data class Extraction(val id: String, val confidence: Int, val conflict: Boolean)

data class Routing(val review: List<String>, val backlog: List<String>, val auto: List<String>)

fun accuracyBy(records: List<Rec>): List<Seg>? {
    // TODO: the overall accuracy and the accuracy of every docType/field segment, the overall one first and then the segments in alphabetical order.
    return null
}

fun canAutomate(records: List<Rec>, threshold: Int, minN: Int): Automation? {
    // TODO: every segment must have enough records and reach the threshold.
    return null
}

fun calibrateThreshold(labeled: List<Labeled>, target: Int): Int? {
    // TODO: the lowest confidence whose auto-accepted items (confidence at or above it) reach the target precision, or null.
    return null
}

fun stratifiedSample(items: List<Item>, perStratum: Int): List<String>? {
    // TODO: the ids of the lowest-ranked items of every stratum.
    return null
}

fun route(extractions: List<Extraction>, threshold: Int, capacity: Int): Routing? {
    // TODO: low confidence and conflicts go to review, the weakest first, within the capacity.
    return null
}

fun checkpoint(action: String, amount: Int, limit: Int = 1000): String? {
    // TODO: "human" or "auto" for an action.
    return null
}
