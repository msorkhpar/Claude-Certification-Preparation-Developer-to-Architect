/** Order a request for cache hits and place its breakpoints. See ../../statement.md for the contract. Blocks are JSON-like maps. */

/** The request cannot be cached as asked. */
class PlanError(message: String) : RuntimeException(message)

fun planRequest(blocks: List<Map<String, Any?>>, minTokens: Int = 1024): List<Map<String, Any?>>? {
    // TODO: return the blocks in cache-friendly order, each as a map {"id": ..., "cache": null, "5m" or "1h"}.
    return null
}
