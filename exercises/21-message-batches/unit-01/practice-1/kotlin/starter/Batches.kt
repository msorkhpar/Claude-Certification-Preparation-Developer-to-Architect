/** Build, split and read Message Batches. See ../../statement.md for the contract. Requests and results are JSON-like maps. */

/** The batch would be refused. [field] names the offending part. */
class BatchError(val field: String, reason: String) : RuntimeException("$field: $reason")

fun buildRequests(items: List<Map<String, Any?>>): List<Map<String, Any?>>? {
    // TODO: turn [{id, params}] into [{custom_id, params}], refusing what a batch refuses.
    return null
}

fun splitBatches(requests: List<Map<String, Any?>>, maxRequests: Int = 100_000, maxBytes: Long = 256L * 1024 * 1024): List<List<Map<String, Any?>>>? {
    // TODO: cut the requests, in order, into batches that respect both limits.
    return null
}

fun collect(requests: List<Map<String, Any?>>, resultLines: List<String>): Map<String, Any?>? {
    // TODO: match the result lines to the requests by custom_id and sort out what to retry and what to fix.
    return null
}
