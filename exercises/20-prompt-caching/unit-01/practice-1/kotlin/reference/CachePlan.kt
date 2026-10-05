import java.lang.System.Logger.Level

private val log = System.getLogger("cacheplan")

/** Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract. Blocks are JSON-like maps. */

/** The request cannot be cached as asked (the API would answer 400, or the plan can never hit). */
class PlanError(message: String) : RuntimeException(message)

private val SECTIONS = mapOf("tools" to 0, "system" to 1, "messages" to 2)
private const val MAX_BREAKPOINTS = 4

private fun checkTools(blocks: List<Map<String, Any?>>) {
    for (b in blocks) {
        if (b["volatile"] == true && b["section"] == "tools") throw PlanError("tool definition ${b["id"]} cannot be volatile: tools come first")
    }
}

private fun ordered(blocks: List<Map<String, Any?>>): List<Map<String, Any?>> {
    val bySection = blocks.sortedBy { SECTIONS.getValue(it["section"] as String) } // sortedBy is stable
    return bySection.filter { it["volatile"] != true } + bySection.filter { it["volatile"] == true }
}

private fun wantsBreakpoint(block: Map<String, Any?>, total: Long, minTokens: Int): Boolean =
    block["breakpoint"] == true && block["volatile"] != true && total >= minTokens

private fun marker(block: Map<String, Any?>): Any = block["ttl"] ?: "5m"

private fun checkCount(marked: List<Any>) {
    if (marked.size > MAX_BREAKPOINTS) throw PlanError("${marked.size} breakpoints: at most $MAX_BREAKPOINTS")
}

private fun checkLifetimes(marked: List<Any>) {
    var seenFive = false
    for (cache in marked) {
        if (cache == "5m") seenFive = true
        else if (seenFive) throw PlanError("a 1h breakpoint must come before every 5m breakpoint")
    }
}

fun planRequest(blocks: List<Map<String, Any?>>, minTokens: Int = 1024): List<Map<String, Any?>> {
    log.log(Level.DEBUG, "planRequest input: {0}", blocks)
    checkTools(blocks)
    val plan = mutableListOf<Map<String, Any?>>()
    val marked = mutableListOf<Any>()
    var total = 0L
    for (b in ordered(blocks)) {
        if (b["volatile"] != true) total += (b["tokens"] as Number).toLong()
        val cache = if (wantsBreakpoint(b, total, minTokens)) marker(b) else null
        plan.add(mapOf("id" to b["id"], "cache" to cache))
        if (cache != null) marked.add(cache)
    }
    checkCount(marked)
    checkLifetimes(marked)
    return plan
}
