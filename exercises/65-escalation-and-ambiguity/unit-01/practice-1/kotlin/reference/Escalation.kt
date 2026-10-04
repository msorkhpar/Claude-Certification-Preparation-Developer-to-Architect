/** When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md. */

data class Case(val askedForPerson: Boolean = false, val matches: Int = 1, val policyCovers: Boolean = true, val attemptsWithoutProgress: Int = 0, val sentiment: String = "calm", val confidence: Int = 50)

data class Decision(val action: String, val reason: String, val acknowledge: Boolean)

data class HandoffCase(val customerId: String?, val issue: String?, val rootCause: String? = null, val amount: String? = null, val actions: List<String>? = null, val recommended: String? = null, val transcript: String? = null)

fun decide(c: Case, maxAttempts: Int = 2): Decision = when {
    c.askedForPerson -> Decision("escalate", "customer asked for a person", false)
    c.matches > 1 -> Decision("clarify", "ambiguous customer match", false)
    !c.policyCovers -> Decision("escalate", "policy does not cover the request", false)
    c.attemptsWithoutProgress >= maxAttempts -> Decision("escalate", "no progress", false)
    else -> Decision("resolve", "within capability", c.sentiment != "calm")
}

fun clarifyingFields(matches: List<Map<String, String>>): List<String> {
    if (matches.size < 2) return emptyList()
    return matches[0].keys.filter { field -> field != "id" && matches.map { it[field] }.toSet().size > 1 }
}

fun handoffText(c: HandoffCase): String {
    require(!c.customerId.isNullOrEmpty() && !c.issue.isNullOrEmpty()) { "a hand-off needs a customer id and an issue" }
    return listOf(
        "Customer: ${c.customerId}",
        "Issue: ${c.issue}",
        "Root cause: ${c.rootCause?.ifEmpty { null } ?: "unknown"}",
        "Amount: ${c.amount?.ifEmpty { null } ?: "unknown"}",
        "Actions taken: ${c.actions?.joinToString("; ")?.ifEmpty { null } ?: "none"}",
        "Recommended action: ${c.recommended?.ifEmpty { null } ?: "review the case"}",
    ).joinToString("\n")
}
