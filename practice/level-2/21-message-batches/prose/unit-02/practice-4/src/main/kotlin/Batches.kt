import java.lang.System.Logger.Level

private val log = System.getLogger("batches")

/** Build, split and read Message Batches. See ../../statement.md for the contract. Requests and results are JSON-like maps. */

/** The batch would be refused. [field] names the offending part. */
class BatchError(val field: String, reason: String) : RuntimeException("$field: $reason")

private val CUSTOM_ID = Regex("^[a-zA-Z0-9_-]{1,64}$")
private const val MAX_REQUESTS = 100_000
private const val MAX_BYTES = 256L * 1024 * 1024
private val USAGE_KEYS = listOf("input_tokens", "output_tokens", "cache_creation_input_tokens", "cache_read_input_tokens")

private fun checkCustomId(id: Any?, seen: MutableSet<String>) {
    // TODO 1 of 6 (finish this to pass e1): refuse a bad or repeated custom id, and remember a good one.
    // Receives the id and the set `seen` of ids already used. Throws BatchError("custom_id", reason) when the id is not a String matching
    // CUSTOM_ID (1 to 64 letters, digits, hyphens or underscores) or is already in `seen`; otherwise adds it to `seen`.
    // Example: "has space" throws; passing "a" twice with the same set throws the second time
}

private fun checkParams(params: Map<String, Any?>) {
    // TODO 2 of 6 (finish this to pass e2): refuse the parameters a batch cannot take.
    // Receives a request body. Throws BatchError("params.max_tokens", reason) when max_tokens is missing or below 1,
    // BatchError("params.stream", reason) when stream is true, and BatchError("params.speed", reason) when speed is present at all.
    // Example: a body with max_tokens 0 throws with field "params.max_tokens"
}

@Suppress("UNCHECKED_CAST")
fun buildRequests(items: List<Map<String, Any?>>): List<Map<String, Any?>> {
    log.log(Level.DEBUG, "buildRequests input: {0}", items)
    val seen = mutableSetOf<String>()
    val requests = mutableListOf<Map<String, Any?>>()
    for (item in items) {
        val id = item["id"]
        val params = item["params"] as Map<String, Any?>
        checkCustomId(id, seen)
        checkParams(params)
        requests.add(linkedMapOf("custom_id" to id, "params" to params))
    }
    return requests
}

private fun size(request: Map<String, Any?>): Long = Json.stringify(request).toByteArray(Charsets.UTF_8).size.toLong()

private fun mustStartNew(count: Int, used: Long, bytes: Long, maxRequests: Int, maxBytes: Long): Boolean {
    // TODO 3 of 6 (finish this to pass e3): must this request go into a new batch?
    // Receives the number of requests in the current batch, their total bytes `used`, the size of the next request and both limits.
    // Returns true when the current batch is not empty and adding the request would pass either limit (count already at maxRequests, or
    // used + bytes above maxBytes). Example: (3, 10, 5, 3, 1000) -> true, (0, 0, 5, 3, 1000) -> false
    return false
}

fun splitBatches(requests: List<Map<String, Any?>>, maxRequests: Int = MAX_REQUESTS, maxBytes: Long = MAX_BYTES): List<List<Map<String, Any?>>> {
    val batches = mutableListOf<List<Map<String, Any?>>>()
    var current = mutableListOf<Map<String, Any?>>()
    var used = 0L
    for (request in requests) {
        val bytes = size(request)
        if (bytes > maxBytes) throw BatchError("size", "${request["custom_id"]} alone is larger than a batch may be")
        if (mustStartNew(current.size, used, bytes, maxRequests, maxBytes)) {
            batches.add(current)
            current = mutableListOf()
            used = 0
        }
        current.add(request)
        used += bytes
    }
    if (current.isNotEmpty()) batches.add(current)
    return batches
}

@Suppress("UNCHECKED_CAST")
private fun keepResult(wanted: List<String>, byId: MutableMap<String, Map<String, Any?>>, unknown: MutableList<String>, record: Map<String, Any?>) {
    // TODO 4 of 6 (finish this to pass m1 and e5): file one parsed result line by its custom id.
    // Receives the list `wanted` of request ids, the map `byId`, the list `unknown` and the parsed `record` (custom_id and result).
    // Adds the id to `unknown` when no request carries it; otherwise stores the record's result (cast it to Map<String, Any?>) in
    // `byId` unless the id is already there (the first result wins). Example: a record for "zzz" when wanted is ["a"] -> unknown
}

private fun needsFix(errorType: String): Boolean {
    // TODO 5 of 6 (finish this to pass e4): does an errored result need a corrected request?
    // Receives the error type of an errored result. Returns true for "invalid_request_error" (it fails again until fixed), false for any
    // other type, which is worth sending again unchanged. Example: "overloaded_error" -> false
    return false
}

private fun addUsage(usage: MutableMap<String, Any?>, used: Map<String, Any?>) {
    // TODO 6 of 6 (finish this to pass e6): add one succeeded reply's usage to the totals.
    // Receives the map `usage` of totals (every key of USAGE_KEYS holds a Long, starting at 0L) and the reply's usage map, which may
    // lack a key. Adds each key of USAGE_KEYS from `used` to `usage`, a missing key counting as 0. Store Long values.
    // Example: adding {input_tokens: 5} raises usage input_tokens by 5 and leaves the other three alone
}

@Suppress("UNCHECKED_CAST")
fun collect(requests: List<Map<String, Any?>>, resultLines: List<String>): Map<String, Any?> {
    val wanted = requests.map { it["custom_id"] as String }
    val byId = linkedMapOf<String, Map<String, Any?>>()
    val unknown = mutableListOf<String>()
    for (line in resultLines) {
        if (line.isBlank()) continue
        keepResult(wanted, byId, unknown, Json.parse(line) as Map<String, Any?>)
    }
    val outcomes = mutableListOf<Map<String, Any?>>()
    val retry = mutableListOf<String>()
    val fix = mutableListOf<String>()
    val usage = linkedMapOf<String, Any?>().also { u -> USAGE_KEYS.forEach { u[it] = 0L } }
    for (cid in wanted) {
        val result = byId[cid]
        when {
            result == null -> {
                outcomes.add(linkedMapOf("custom_id" to cid, "status" to "missing"))
                retry.add(cid)
            }
            result["type"] == "succeeded" -> {
                val message = result["message"] as Map<String, Any?>
                val text = (message["content"] as List<Map<String, Any?>>).filter { it["type"] == "text" }.joinToString("") { (it["text"] ?: "").toString() }
                val used = message["usage"] as Map<String, Any?>
                outcomes.add(linkedMapOf("custom_id" to cid, "status" to "succeeded", "text" to text, "usage" to used))
                addUsage(usage, used)
            }
            result["type"] == "errored" -> {
                val kind = ((result["error"] as Map<String, Any?>)["error"] as Map<String, Any?>)["type"] as String
                outcomes.add(linkedMapOf("custom_id" to cid, "status" to "errored", "error_type" to kind))
                (if (needsFix(kind)) fix else retry).add(cid)
            }
            else -> { // canceled or expired: the request never reached the model
                outcomes.add(linkedMapOf("custom_id" to cid, "status" to result["type"]))
                retry.add(cid)
            }
        }
    }
    return linkedMapOf("outcomes" to outcomes, "retry" to retry, "fix" to fix, "unknown" to unknown, "usage" to usage)
}
