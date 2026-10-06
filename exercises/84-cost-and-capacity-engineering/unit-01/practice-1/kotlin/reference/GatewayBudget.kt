private val log = System.getLogger("gateway_budget")

/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. Requests, policies and rows are JSON-like maps. */

@Suppress("UNCHECKED_CAST")
private fun asMap(value: Any?): Map<String, Any?> = value as Map<String, Any?>

@Suppress("UNCHECKED_CAST")
private fun asList(value: Any?): List<String> = value as List<String>

private fun n(map: Map<String, Any?>, key: String) = (map[key] as Number).toLong()

private fun chosenModel(request: Map<String, Any?>, policy: Map<String, Any?>): String? {
    val wanted = request["model"] as String?
    return if (wanted != null && wanted in asList(policy["allowed"])) wanted else (asMap(policy["routes"])[request["task"] as String] ?: policy["default"]) as String
}

fun route(request: Map<String, Any?>, policy: Map<String, Any?>, status: String): String? {
    log.log(System.Logger.Level.DEBUG, "route input: {0}", request)
    if (status == "block") return null
    val model = chosenModel(request, policy)!!
    return if (status == "warn") (asMap(policy["cheaper"])[model] ?: model) as String else model
}

fun admit(spend: Long, budget: Long, estimate: Long): String? {
    if (budget <= 0) return "block"
    val after = spend + estimate
    if (after > budget) return "block"
    if (after * 100 >= budget * 80) return "warn"
    return "allow"
}

private fun cost(row: Map<String, Any?>, prices: Map<String, Any?>): Long {
    val name = row["model"] as String
    require(name in prices) { "unknown model: $name" }
    val p = asMap(prices[name])
    return n(row, "input") * n(p, "input") + n(row, "cache_read") * n(p, "cache_read") + n(row, "output") * n(p, "output")
}

private fun toCents(value: Long): Long {
    return (value + 500_000) / 1_000_000
}

fun showback(rows: List<Map<String, Any?>>, prices: Map<String, Any?>): List<Map<String, Any?>>? {
    val totals = linkedMapOf<String, Long>()
    for (r in rows) totals[r["team"] as String] = (totals[r["team"] as String] ?: 0L) + cost(r, prices)
    val result = totals.map { (team, value) -> linkedMapOf<String, Any?>("team" to team, "cents" to toCents(value)) }
    return result.sortedWith(compareBy({ -(it["cents"] as Long) }, { it["team"] as String }))
}

fun delivery(p95Seconds: Long, timeoutSeconds: Long, marginPercent: Long): String? {
    return if (p95Seconds * (100 + marginPercent) <= timeoutSeconds * 100) "sync" else "accept-and-poll"
}
