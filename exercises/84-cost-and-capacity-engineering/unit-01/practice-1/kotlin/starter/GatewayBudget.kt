/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. Requests, policies and rows are JSON-like maps. */

fun route(request: Map<String, Any?>, policy: Map<String, Any?>, status: String): String? {
    // TODO: the model for the request, or null when the team is blocked.
    return null
}

fun admit(spend: Long, budget: Long, estimate: Long): String? {
    // TODO: "allow", "warn" or "block" for one more request against the budget.
    return null
}

fun showback(rows: List<Map<String, Any?>>, prices: Map<String, Any?>): List<Map<String, Any?>>? {
    // TODO: a list of {team, cents} for each team, most expensive first, ties by team name (cents is a Long).
    return null
}

fun delivery(p95Seconds: Long, timeoutSeconds: Long, marginPercent: Long): String? {
    // TODO: "sync" or "accept-and-poll".
    return null
}
