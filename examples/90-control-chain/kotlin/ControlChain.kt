/**
 * Governing a model call: a control that fails closed where the cost of an error is high, an independent check against the source that a confident answer must pass, and an audit record that holds no content.
 *
 * The requests, answers and thresholds are invented; the confidence threshold of 95 is a value to tune to your own error costs. Nothing here calls a model.
 */

private val log = System.getLogger("control_chain")

data class Action(val name: String, val consequence: String)

data class Answer(val text: String, val confidence: Int, val quote: String)

data class Case(val label: String, val action: Action, val answer: Answer, val screenUp: Boolean)

data class Erased(val kept: Map<String, String>, val removed: Int)

const val AUTO_CONFIDENCE = 95

/** Decide what happens to an answer. A down screen holds a high-consequence action, an unsupported answer is held whatever its confidence, and only a confident, supported, low-consequence answer goes out unreviewed. */
fun route(action: Action, answer: Answer, source: String, screenUp: Boolean, confidenceMin: Int = AUTO_CONFIDENCE): String {
    log.log(System.Logger.Level.DEBUG, "route input: {0}", action)
    if (!screenUp && action.consequence == "high") return "hold: screen down"
    val flag = if (screenUp) "" else " (unscreened)"
    if (answer.quote !in source) return "hold: unsupported$flag"
    if (action.consequence == "high") return "human$flag"
    return (if (answer.confidence >= confidenceMin) "auto" else "review") + flag
}

/** Proof of what happened without a copy of the data: who, what, how big and the outcome, and never the text. */
fun auditRecord(requestId: String, action: Action, outcome: String, text: String): Map<String, Any> =
    linkedMapOf("request" to requestId, "action" to action.name, "consequence" to action.consequence, "outcome" to outcome, "chars" to text.length, "content_stored" to false)

/** Erasure removes the map from a token to a person, so the audit entries that carry only tokens can no longer be linked to anyone. */
fun erase(vault: Map<String, String>, subject: String): Erased {
    val kept = vault.filterValues { it != subject }
    return Erased(kept, vault.size - kept.size)
}

fun flag(value: Any): String = if (value is Boolean) (if (value) "True" else "False") else value.toString()

fun main() {
    val source = "Water damage is covered up to 5,000 per claim. Flood damage is excluded."
    val reply = Action("draft_reply", "low")
    val refund = Action("issue_refund", "high")
    val supported = "Water damage is covered up to 5,000 per claim."
    val good = Answer("Water damage is covered up to 5,000.", 99, supported)
    val edge = Answer("Water damage is covered up to 5,000.", 95, supported)
    val unsure = Answer("Water damage is covered up to 5,000.", 94, supported)
    val wrong = Answer("Water damage is covered up to 8,000.", 99, "Water damage is covered up to 8,000 per claim.")
    val cases = listOf(
        Case("screen up, refund, supported", refund, good, true),
        Case("screen up, reply, confidence 99", reply, good, true),
        Case("screen up, reply, confidence 95", reply, edge, true),
        Case("screen up, reply, confidence 94", reply, unsure, true),
        Case("screen up, reply, confident but unsupported", reply, wrong, true),
        Case("screen down, refund", refund, good, false),
        Case("screen down, reply", reply, good, false),
    )
    for (c in cases) println("${c.label}: ${route(c.action, c.answer, source, c.screenUp)}")
    val record = auditRecord("r-1001", refund, "human", good.text)
    println("audit record: " + record.entries.joinToString(", ") { "${it.key}=${flag(it.value)}" })
    val vault = linkedMapOf("<EMAIL_1>" to "person-a", "<EMAIL_2>" to "person-b", "<MEMBER_1>" to "person-a")
    val erased = erase(vault, "person-a")
    println("erasure removed ${erased.removed} of ${vault.size} mappings; the audit entries stay, with ${erased.kept.size} token still linkable")
}
