/** Assemble a streamed Messages reply from its events. See ../../statement.md for the contract. */
@Suppress("UNCHECKED_CAST")
fun assemble(events: List<Map<String, Any?>>): Map<String, Any?> {
    var message: MutableMap<String, Any?>? = null
    val blocks = sortedMapOf<Int, MutableMap<String, Any?>>()
    val fragments = HashMap<Int, StringBuilder>()
    var stopped = false
    for (event in events) {
        when (event["type"]) {
            "message_start" -> {
                val start = event["message"] as Map<String, Any?>
                message = start.toMutableMap()
                message["content"] = mutableListOf<Any?>()
                message["usage"] = ((start["usage"] as? Map<String, Any?>) ?: emptyMap()).toMutableMap()
            }
            "content_block_start" -> {
                val index = (event["index"] as Number).toInt()
                val block = (event["content_block"] as Map<String, Any?>).toMutableMap()
                blocks[index] = block
                if (block["type"] == "tool_use") fragments[index] = StringBuilder()
            }
            "content_block_delta" -> {
                val index = (event["index"] as Number).toInt()
                val block = blocks.getValue(index)
                val delta = event["delta"] as Map<String, Any?>
                when (delta["type"]) {
                    "text_delta" -> block["text"] = (block["text"] as? String ?: "") + delta["text"]
                    "input_json_delta" -> fragments.getValue(index).append(delta["partial_json"] as String)
                    "thinking_delta" -> block["thinking"] = (block["thinking"] as? String ?: "") + delta["thinking"]
                    "signature_delta" -> block["signature"] = delta["signature"]
                }
            }
            "content_block_stop" -> {
                val index = (event["index"] as Number).toInt()
                fragments[index]?.let { f ->
                    val joined = f.toString()
                    blocks.getValue(index)["input"] = if (joined.isBlank()) emptyMap<String, Any?>() else Json.parse(joined)
                }
            }
            "message_delta" -> {
                val delta = event["delta"] as Map<String, Any?>
                message!!["stop_reason"] = delta["stop_reason"]
                message["stop_sequence"] = delta["stop_sequence"]
                (event["usage"] as? Map<String, Any?>)?.let { (message["usage"] as MutableMap<String, Any?>).putAll(it) }
            }
            "message_stop" -> stopped = true
            "error" -> {
                val error = event["error"] as Map<String, Any?>
                throw StreamError(error["type"]?.toString() ?: "unknown", error["message"]?.toString() ?: "")
            }
            else -> {} // ping and event types this client does not know are skipped: new types may be added
        }
    }
    if (message == null || !stopped) throw StreamError("incomplete_stream", "the stream ended before message_stop")
    message["content"] = blocks.values.toList()
    return message
}
