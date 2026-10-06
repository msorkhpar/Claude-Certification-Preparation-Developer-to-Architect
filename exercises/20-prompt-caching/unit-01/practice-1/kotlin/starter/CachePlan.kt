import java.lang.System.Logger.Level

private val log = System.getLogger("cacheplan")

/** Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract. Blocks are JSON-like maps. */

/** The request cannot be cached as asked (the API would answer 400, or the plan can never hit). */
class PlanError(message: String) : RuntimeException(message)

private val SECTIONS = mapOf("tools" to 0, "system" to 1, "messages" to 2)
private const val MAX_BREAKPOINTS = 4

private fun checkTools(blocks: List<Map<String, Any?>>) {
    // TODO 1 of 6 (finish this to pass e5): refuse a volatile tool definition.
    // Receives the blocks. Throws PlanError when a block in the "tools" section is volatile (tools come first, nothing volatile may sit
    // in the prefix); returns nothing otherwise. Example: a tools block with "volatile" true makes it throw
}

private fun ordered(blocks: List<Map<String, Any?>>): List<Map<String, Any?>> {
    // TODO 2 of 6 (finish this to pass m1, e1 and e6): the blocks in cache-friendly order, as a new list.
    // Receives the blocks. Returns a new list: sections in the order of SECTIONS (tools, system, messages), blocks of one section in the
    // order given, then every volatile block moved to the very end in its given order. The input list is left unchanged.
    // Example: ids of [m (messages), s (system, volatile), t (tools)] come out as t, m, s
    return emptyList()
}

private fun wantsBreakpoint(block: Map<String, Any?>, total: Long, minTokens: Int): Boolean {
    // TODO 3 of 6 (finish this to pass e2 and e5): does this block carry a breakpoint?
    // Receives the block, total (the stable tokens from the start up to and including this block) and minTokens. Returns true when the
    // block asks for a breakpoint, is not volatile and total is at least minTokens. Example: a breakpoint block at 500 of 1024 -> false
    return false
}

private fun marker(block: Map<String, Any?>): Any {
    // TODO 4 of 6 (finish this to pass m1): the cache lifetime a breakpoint carries.
    // Receives the block. Returns its "ttl" ("5m" or "1h"), "5m" when it has none. Example: a block with ttl "1h" -> "1h"
    return "5m"
}

private fun checkCount(marked: List<Any>) {
    // TODO 5 of 6 (finish this to pass e3): at most MAX_BREAKPOINTS breakpoints.
    // Receives the lifetimes of the blocks that kept a breakpoint. Throws PlanError when there are more than MAX_BREAKPOINTS.
    // Example: five "5m" entries make it throw
}

private fun checkLifetimes(marked: List<Any>) {
    // TODO 6 of 6 (finish this to pass e4): a 1h breakpoint must come before every 5m one.
    // Receives the lifetimes of the kept breakpoints in request order. Throws PlanError when a "1h" follows a "5m".
    // Example: ["5m", "1h"] throws, ["1h", "5m"] does not
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
