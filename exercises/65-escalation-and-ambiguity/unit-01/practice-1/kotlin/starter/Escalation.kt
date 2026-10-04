/** When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md. */

data class Case(val askedForPerson: Boolean = false, val matches: Int = 1, val policyCovers: Boolean = true, val attemptsWithoutProgress: Int = 0, val sentiment: String = "calm", val confidence: Int = 50)

data class Decision(val action: String, val reason: String, val acknowledge: Boolean)

data class HandoffCase(val customerId: String?, val issue: String?, val rootCause: String? = null, val amount: String? = null, val actions: List<String>? = null, val recommended: String? = null, val transcript: String? = null)

fun decide(c: Case, maxAttempts: Int = 2): Decision? {
    // TODO: resolve, clarify or escalate, with the reason and whether the reply acknowledges the customer's frustration.
    return null
}

fun clarifyingFields(matches: List<Map<String, String>>): List<String>? {
    // TODO: the fields (never "id") on which the matching records differ, in the order of the first record.
    return null
}

fun handoffText(c: HandoffCase): String? {
    // TODO: the text of a hand-off: six labelled lines, no transcript; an error when the customer id or the issue is missing.
    return null
}
