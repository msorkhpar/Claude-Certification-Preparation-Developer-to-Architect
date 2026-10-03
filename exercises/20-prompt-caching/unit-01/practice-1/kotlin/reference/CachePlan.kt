/** Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract. Blocks are JSON-like maps. */

/** The request cannot be cached as asked (the API would answer 400, or the plan can never hit). */
class PlanError(message: String) : RuntimeException(message)

private val SECTIONS = mapOf("tools" to 0, "system" to 1, "messages" to 2)
private const val MAX_BREAKPOINTS = 4

fun planRequest(blocks: List<Map<String, Any?>>, minTokens: Int = 1024): List<Map<String, Any?>> {
    for (b in blocks) {
        if (b["volatile"] == true && b["section"] == "tools") throw PlanError("tool definition ${b["id"]} cannot be volatile: tools come first")
    }
    val bySection = blocks.sortedBy { SECTIONS.getValue(it["section"] as String) } // sortedBy is stable
    val ordered = bySection.filter { it["volatile"] != true } + bySection.filter { it["volatile"] == true }
    val plan = mutableListOf<Map<String, Any?>>()
    var total = 0L
    for (b in ordered) {
        val isVolatile = b["volatile"] == true
        if (!isVolatile) total += (b["tokens"] as Number).toLong()
        val wanted = b["breakpoint"] == true && !isVolatile && total >= minTokens
        plan.add(mapOf("id" to b["id"], "cache" to (if (wanted) (b["ttl"] ?: "5m") else null)))
    }
    val marked = plan.filter { it["cache"] != null }
    if (marked.size > MAX_BREAKPOINTS) throw PlanError("${marked.size} breakpoints: at most $MAX_BREAKPOINTS")
    var seenFive = false
    for (p in marked) {
        if (p["cache"] == "5m") seenFive = true
        else if (seenFive) throw PlanError("a 1h breakpoint must come before every 5m breakpoint")
    }
    return plan
}
