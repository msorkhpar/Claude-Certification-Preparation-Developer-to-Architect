private val log = System.getLogger("gateway_budget")

/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. Requests, policies and rows are JSON-like maps. */

@Suppress("UNCHECKED_CAST")
private fun asMap(value: Any?): Map<String, Any?> = value as Map<String, Any?>

@Suppress("UNCHECKED_CAST")
private fun asList(value: Any?): List<String> = value as List<String>

private fun n(map: Map<String, Any?>, key: String) = (map[key] as Number).toLong()

private fun chosenModel(request: Map<String, Any?>, policy: Map<String, Any?>): String? {
    // TODO 1 of 6 (unlocks m1 and e6): the model the policy picks for a request.
    // Receives the request (`task`, and perhaps `model`, a pin) and the policy (`allowed`, `routes`, `default`; use `asList` and `asMap`). Returns the pinned model
    // when it is in the `allowed` list; otherwise the model `routes` names for the task, or `default` for a task that is not listed.
    // Example: task "review" with no pin -> "opus"; task "classify" pinned to "opus" (not allowed) -> "haiku"
    return null
}

fun route(request: Map<String, Any?>, policy: Map<String, Any?>, status: String): String? {
    log.log(System.Logger.Level.DEBUG, "route input: {0}", request)
    // TODO 2 of 6 (unlocks e1): the model for the request once the budget status is known.
    // Receives the request, the policy and the budget status ("allow", "warn" or "block"). Returns null when the status is "block"; the model from
    // `chosenModel` when it is "allow"; for "warn" the cheaper model that the policy's `cheaper` map names for it (the model itself when none is named).
    // Example: status "warn", chosen model "opus", cheaper {opus: sonnet} -> "sonnet"
    return null
}

fun admit(spend: Long, budget: Long, estimate: Long): String? {
    // TODO 3 of 6 (unlocks e2): admit one more request against the budget.
    // Receives what the team has spent, its budget and the estimated cost of the request, all in cents. Returns "block" when the budget is not positive or
    // `spend + estimate` is more than the budget; "warn" when it reaches 80 percent of the budget (80 percent itself warns) and is not over; else "allow".
    // Example: admit(700, 1000, 100) -> "warn", admit(900, 1000, 101) -> "block"
    return null
}

private fun cost(row: Map<String, Any?>, prices: Map<String, Any?>): Long {
    // TODO 4 of 6 (unlocks e3): what one row of tokens costs.
    // Receives a row (`model`, `input`, `cache_read`, `output`: token counts) and `prices`, which maps a model to cents per million tokens for the same three
    // kinds (`n(map, key)` reads a number). Returns tokens times price, summed over the three kinds (not yet divided by a million). Fails with
    // `require(...) { "unknown model: $name" }` for a model without a price.
    // Example: input 1_000_000 at price 200 and nothing else -> 200_000_000
    return 0L
}

private fun toCents(value: Long): Long {
    // TODO 5 of 6 (unlocks e4): a team's total in whole cents.
    // Receives a total in cents times a million. Returns it divided by one million and rounded to the nearest cent, halves up, with integer arithmetic.
    // Example: toCents(500_000) -> 1, toCents(499_999) -> 0
    return 0L
}

fun showback(rows: List<Map<String, Any?>>, prices: Map<String, Any?>): List<Map<String, Any?>>? {
    val totals = linkedMapOf<String, Long>()
    for (r in rows) totals[r["team"] as String] = (totals[r["team"] as String] ?: 0L) + cost(r, prices)
    val result = totals.map { (team, value) -> linkedMapOf<String, Any?>("team" to team, "cents" to toCents(value)) }
    return result.sortedWith(compareBy({ -(it["cents"] as Long) }, { it["team"] as String }))
}

fun delivery(p95Seconds: Long, timeoutSeconds: Long, marginPercent: Long): String? {
    // TODO 6 of 6 (unlocks e5): sync or accept-and-poll.
    // Receives the p95 latency and the caller's timeout in seconds and a safety margin in percent. Returns "sync" when the p95 plus the margin fits within the
    // timeout (an exact fit counts), otherwise "accept-and-poll". Compare p95 * (100 + margin) with timeout * 100.
    // Example: delivery(8, 10, 25) -> "sync", delivery(8, 10, 26) -> "accept-and-poll"
    return null
}
