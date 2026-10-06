private val log = System.getLogger("assemble")

/** The message a message_start event begins: its fields except content, an empty content list and a copy of its usage. */
@Suppress("UNCHECKED_CAST")
private fun startMessage(start: Map<String, Any?>): MutableMap<String, Any?> {
    val message = start.toMutableMap()
    message["content"] = mutableListOf<Any?>()
    message["usage"] = ((start["usage"] as? Map<String, Any?>) ?: emptyMap()).toMutableMap()
    return message
}

/** Fold one content_block_delta into its block; `pieces` is the fragment builder of a tool_use block. */
private fun applyDelta(block: MutableMap<String, Any?>, pieces: StringBuilder?, delta: Map<String, Any?>) {
    when (delta["type"]) {
        "text_delta" -> block["text"] = (block["text"] as? String ?: "") + delta["text"]
        "input_json_delta" -> pieces!!.append(delta["partial_json"] as String)
        "thinking_delta" -> block["thinking"] = (block["thinking"] as? String ?: "") + delta["thinking"]
        "signature_delta" -> block["signature"] = delta["signature"]
    }
}

/** At content_block_stop: a tool_use block (it has `pieces`) gets its input, the fragments joined and parsed, an empty map when empty. */
private fun finishBlock(block: MutableMap<String, Any?>, pieces: StringBuilder?) {
    if (pieces == null) return
    val joined = pieces.toString()
    block["input"] = if (joined.isBlank()) emptyMap<String, Any?>() else Json.parse(joined)
}

/** Fold a message_delta event: the stop reason and sequence, and each usage key replaces the same key (the output count is cumulative). */
@Suppress("UNCHECKED_CAST")
private fun applyMessageDelta(message: MutableMap<String, Any?>, event: Map<String, Any?>) {
    val delta = event["delta"] as Map<String, Any?>
    message["stop_reason"] = delta["stop_reason"]
    message["stop_sequence"] = delta["stop_sequence"]
    (event["usage"] as? Map<String, Any?>)?.let { (message["usage"] as MutableMap<String, Any?>).putAll(it) }
}

/** An error event ends the assembly with a StreamError carrying the error's type and message. */
@Suppress("UNCHECKED_CAST")
private fun raiseError(event: Map<String, Any?>) {
    val error = event["error"] as Map<String, Any?>
    throw StreamError(error["type"]?.toString() ?: "unknown", error["message"]?.toString() ?: "")
}

/** A stream that never started or never reached message_stop is an error, not a short message. */
private fun checkComplete(message: Map<String, Any?>?, stopped: Boolean) {
    if (message == null || !stopped) throw StreamError("incomplete_stream", "the stream ended before message_stop")
}

/** Assemble a streamed Messages reply from its events. See ../../statement.md for the contract. */
@Suppress("UNCHECKED_CAST")
fun assemble(events: List<Map<String, Any?>>): Map<String, Any?> {
    log.log(System.Logger.Level.DEBUG, "assemble input: {0}", events)
    var message: MutableMap<String, Any?>? = null
    val blocks = sortedMapOf<Int, MutableMap<String, Any?>>()
    val fragments = HashMap<Int, StringBuilder>()
    var stopped = false
    for (event in events) {
        when (event["type"]) {
            "message_start" -> message = startMessage(event["message"] as Map<String, Any?>)
            "content_block_start" -> {
                val index = (event["index"] as Number).toInt()
                val block = (event["content_block"] as Map<String, Any?>).toMutableMap()
                blocks[index] = block
                if (block["type"] == "tool_use") fragments[index] = StringBuilder()
            }
            "content_block_delta" -> {
                val index = (event["index"] as Number).toInt()
                applyDelta(blocks.getValue(index), fragments[index], event["delta"] as Map<String, Any?>)
            }
            "content_block_stop" -> {
                val index = (event["index"] as Number).toInt()
                finishBlock(blocks.getValue(index), fragments[index])
            }
            "message_delta" -> applyMessageDelta(message!!, event)
            "message_stop" -> stopped = true
            "error" -> raiseError(event)
            else -> {} // ping and event types this client does not know are skipped: new types may be added
        }
    }
    checkComplete(message, stopped)
    message!!["content"] = blocks.values.toList()
    return message
}
