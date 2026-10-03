/** Build, split and read Message Batches. See ../../statement.md for the contract. Requests and results are JSON-like maps. */

/** The batch would be refused. [field] names the offending part. */
class BatchError(val field: String, reason: String) : RuntimeException("$field: $reason")

private val CUSTOM_ID = Regex("^[a-zA-Z0-9_-]{1,64}$")
private const val MAX_REQUESTS = 100_000
private const val MAX_BYTES = 256L * 1024 * 1024
private val USAGE_KEYS = listOf("input_tokens", "output_tokens", "cache_creation_input_tokens", "cache_read_input_tokens")

@Suppress("UNCHECKED_CAST")
fun buildRequests(items: List<Map<String, Any?>>): List<Map<String, Any?>> {
    val seen = mutableSetOf<String>()
    val requests = mutableListOf<Map<String, Any?>>()
    for (item in items) {
        val id = item["id"]
        val params = item["params"] as Map<String, Any?>
        if (id !is String || !CUSTOM_ID.matches(id)) throw BatchError("custom_id", "$id is not 1 to 64 letters, digits, hyphens or underscores")
        if (!seen.add(id)) throw BatchError("custom_id", "$id is used twice")
        val maxTokens = (params["max_tokens"] as Number?)?.toLong() ?: 0L
        if (maxTokens < 1) throw BatchError("params.max_tokens", "must be at least 1 inside a batch")
        if (params["stream"] == true) throw BatchError("params.stream", "batch results are a file, not a stream")
        if (params.containsKey("speed")) throw BatchError("params.speed", "fast mode is not available in a batch")
        requests.add(linkedMapOf("custom_id" to id, "params" to params))
    }
    return requests
}

private fun size(request: Map<String, Any?>): Long = Json.stringify(request).toByteArray(Charsets.UTF_8).size.toLong()

fun splitBatches(requests: List<Map<String, Any?>>, maxRequests: Int = MAX_REQUESTS, maxBytes: Long = MAX_BYTES): List<List<Map<String, Any?>>> {
    val batches = mutableListOf<List<Map<String, Any?>>>()
    var current = mutableListOf<Map<String, Any?>>()
    var used = 0L
    for (request in requests) {
        val bytes = size(request)
        if (bytes > maxBytes) throw BatchError("size", "${request["custom_id"]} alone is larger than a batch may be")
        if (current.isNotEmpty() && (current.size >= maxRequests || used + bytes > maxBytes)) {
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
fun collect(requests: List<Map<String, Any?>>, resultLines: List<String>): Map<String, Any?> {
    val wanted = requests.map { it["custom_id"] as String }
    val byId = linkedMapOf<String, Map<String, Any?>>()
    val unknown = mutableListOf<String>()
    for (line in resultLines) {
        if (line.isBlank()) continue
        val record = Json.parse(line) as Map<String, Any?>
        val cid = record["custom_id"] as String
        if (cid !in wanted) unknown.add(cid) else byId.putIfAbsent(cid, record["result"] as Map<String, Any?>)
    }
    val outcomes = mutableListOf<Map<String, Any?>>()
    val retry = mutableListOf<String>()
    val fix = mutableListOf<String>()
    val usage = linkedMapOf<String, Any?>().also { u -> USAGE_KEYS.forEach { u[it] = 0L } }
    for (cid in wanted) {
        val result = byId[cid]
        when {
            result == null -> {
                continue
            }
            result["type"] == "succeeded" -> {
                val message = result["message"] as Map<String, Any?>
                val text = (message["content"] as List<Map<String, Any?>>).filter { it["type"] == "text" }.joinToString("") { (it["text"] ?: "").toString() }
                val used = message["usage"] as Map<String, Any?>
                outcomes.add(linkedMapOf("custom_id" to cid, "status" to "succeeded", "text" to text, "usage" to used))
                for (key in USAGE_KEYS) usage[key] = (usage[key] as Long) + ((used[key] as Number?)?.toLong() ?: 0L)
            }
            result["type"] == "errored" -> {
                val kind = ((result["error"] as Map<String, Any?>)["error"] as Map<String, Any?>)["type"] as String
                outcomes.add(linkedMapOf("custom_id" to cid, "status" to "errored", "error_type" to kind))
                (if (kind == "invalid_request_error") fix else retry).add(cid)
            }
            else -> { // canceled or expired: the request never reached the model
                outcomes.add(linkedMapOf("custom_id" to cid, "status" to result["type"]))
                retry.add(cid)
            }
        }
    }
    return linkedMapOf("outcomes" to outcomes, "retry" to retry, "fix" to fix, "unknown" to unknown, "usage" to usage)
}
