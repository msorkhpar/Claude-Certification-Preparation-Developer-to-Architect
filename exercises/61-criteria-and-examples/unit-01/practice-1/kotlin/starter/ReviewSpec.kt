/** A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md. Results are JSON-like maps. */

/** Phrases that name no pattern. */
@Suppress("unused")
val VAGUE = listOf("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment")

fun buildReviewPrompt(spec: Map<String, Any?>, diff: String): String? {
    // TODO: refuse a specification that is vague or incomplete (IllegalArgumentException), then write the prompt: criteria, examples, diff last.
    return null
}

fun categoryReport(findings: List<Map<String, Any?>>, minReviewed: Int = 5, minPrecision: Double = 0.5): Map<String, Any?>? {
    // TODO: per category the number reviewed, the precision, whether to disable it and its most dismissed patterns.
    return null
}

fun nextStep(request: Map<String, Any?>, required: List<String>, defaults: Map<String, String>, attended: Boolean): Map<String, Any?>? {
    // TODO: proceed, ask or stop for a request with missing fields, and state the assumptions made.
    return null
}
