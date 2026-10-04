/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. Requests, policies and rows are JSON-like maps. */

@Suppress("UNCHECKED_CAST")
private fun asMap(value: Any?): Map<String, Any?> = value as Map<String, Any?>

@Suppress("UNCHECKED_CAST")
private fun asList(value: Any?): List<String> = value as List<String>

fun route(request: Map<String, Any?>, policy: Map<String, Any?>, status: String): String? {
    if (status == "block") {
        return null
    }
    val wanted = request["model"] as String?
    var model = if (wanted != null && wanted in asList(policy["allowed"])) wanted else (asMap(policy["routes"])[request["task"] as String] ?: policy["default"]) as String
    if (status == "warn") {
        model = (asMap(policy["cheaper"])[model] ?: model) as String
    }
    return model
}

fun admit(spend: Long, budget: Long, estimate: Long): String? {
    if (budget <= 0) {
        return "block"
    }
    val after = spend + estimate
    if (after > budget) return "block"
    if (after * 100 >= budget * 80) return "warn"
    return "allow"
}

fun showback(rows: List<Map<String, Any?>>, prices: Map<String, Any?>): List<Map<String, Any?>>? {
    val totals = linkedMapOf<String, Long>()
    for (r in rows) {
        val name = r["model"] as String
        require(name in prices) { "unknown model: $name" }
        val p = asMap(prices[name])
        fun n(map: Map<String, Any?>, key: String) = (map[key] as Number).toLong()
        val value = n(r, "input") * n(p, "input") + n(r, "cache_read") * n(p, "cache_read") + n(r, "output") * n(p, "output")
        totals[r["team"] as String] = (totals[r["team"] as String] ?: 0L) + value
    }
    val result = totals.map { (team, value) -> linkedMapOf<String, Any?>("team" to team, "cents" to (value + 500_000) / 1_000_000) }
    return result.sortedWith(compareBy({ -(it["cents"] as Long) }, { it["team"] as String }))
}

fun delivery(p95Seconds: Long, timeoutSeconds: Long, marginPercent: Long): String? =
    if (p95Seconds * (100 + marginPercent) <= timeoutSeconds * 100) "sync" else "accept-and-poll"
