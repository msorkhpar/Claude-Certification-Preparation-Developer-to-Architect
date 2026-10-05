/** When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md. */

private val log = System.getLogger("escalation")

data class Case(val askedForPerson: Boolean = false, val matches: Int = 1, val policyCovers: Boolean = true, val attemptsWithoutProgress: Int = 0, val sentiment: String = "calm", val confidence: Int = 50)

data class Decision(val action: String, val reason: String, val acknowledge: Boolean)

data class HandoffCase(val customerId: String?, val issue: String?, val rootCause: String? = null, val amount: String? = null, val actions: List<String>? = null, val recommended: String? = null, val transcript: String? = null)

fun decide(c: Case, maxAttempts: Int = 2): Decision = when {
    // TODO 1 of 7 (finish this to pass m1, e4): the first rule. When the customer asked for a person, return the
    //   decision escalate with the reason "customer asked for a person", before any other rule. Example: asked_for_person
    //   true and two matches -> escalate.
    // TODO 2 of 7 (finish this to pass e3): the ambiguity rule. When more than one customer record matches, return the
    //   decision clarify with the reason "ambiguous customer match". Example: matches 2 -> clarify.
    // TODO 3 of 7 (finish this to pass e2): the policy rule. When the policy does not cover the request, return the
    //   decision escalate with the reason "policy does not cover the request". Example: policy_covers false -> escalate.
    // TODO 4 of 7 (finish this to pass e5): the progress rule. When the attempts without progress are at or above
    //   max_attempts, return the decision escalate with the reason "no progress". Example: 2 attempts, limit 2 ->
    //   escalate; 1 attempt -> not.
    // TODO 5 of 7 (finish this to pass e1): the acknowledgement of a resolved case. Return resolve with the reason
    //   "within capability" and acknowledge true when the sentiment is anything but calm, false when it is calm. Example:
    //   sentiment frustrated -> resolve, acknowledge true.
    else -> Decision("resolve", "within capability", false)
}

fun clarifyingFields(matches: List<Map<String, String>>): List<String> {
    if (matches.size < 2) return emptyList()
    // TODO 6 of 7 (finish this to pass e7): the fields to ask about. Receives the matching records (maps). Return the
    //   names of the fields, other than id, whose values are not all the same across the matches; return none when there
    //   are fewer than two matches. Example: two records that differ only in city -> [city].
    return emptyList()
}

fun handoffText(c: HandoffCase): String {
    log.log(System.Logger.Level.DEBUG, "handoffText input: {0}", c)
    require(!c.customerId.isNullOrEmpty() && !c.issue.isNullOrEmpty()) { "a hand-off needs a customer id and an issue" }
    return listOf(
        "Customer: ${c.customerId}",
        "Issue: ${c.issue}",
        "Root cause: ${c.rootCause?.ifEmpty { null } ?: "unknown"}",
        "Amount: ${c.amount?.ifEmpty { null } ?: "unknown"}",
        // TODO 7 of 7 (finish this to pass e8): the last two lines of the hand-off. Write "Actions taken: " with the
        //   actions joined by "; " (or none when there are none) and "Recommended action: " with the recommendation (or
        //   review the case when it is empty). Never the transcript. Example: actions [refund, email] -> "Actions taken:
        //   refund; email".
        "Actions taken: none",
        "Recommended action: review the case",
    ).joinToString("\n")
}
