private val log = System.getLogger("assemble")

private fun startMessage(start: Map<String, Any?>): MutableMap<String, Any?> {
    // TODO 1 of 6 (finish this to pass m1 and e5): the message a message_start event begins.
    // Receives the `message` object of the event. Returns a new mutable map with its fields except "content", then "content" as an empty list and "usage" as a COPY of its usage.
    // Example: startMessage(mapOf("id" to "m", "content" to listOf<Any?>(), "usage" to mapOf("input_tokens" to 5))) -> id m, content [], usage {input_tokens=5}
    return mutableMapOf()
}

private fun applyDelta(block: MutableMap<String, Any?>, pieces: StringBuilder?, delta: Map<String, Any?>) {
    // TODO 2 of 6 (finish this to pass m1 and e6): fold one content_block_delta into its block.
    // Receives the block, `pieces` (the fragment builder of a tool_use block, else null) and the delta. "text_delta" appends "text" to the block's text,
    // "thinking_delta" appends "thinking", "signature_delta" sets "signature", "input_json_delta" appends "partial_json" to `pieces`. Returns nothing.
    // Example: block {text=He}, delta {type=text_delta, text=llo} -> block {text=Hello}
}

private fun finishBlock(block: MutableMap<String, Any?>, pieces: StringBuilder?) {
    // TODO 3 of 6 (finish this to pass e1): at content_block_stop, give a tool_use block its input.
    // Receives the block and its fragment builder `pieces` (null for a block that is not tool_use). When `pieces` is not null, sets block["input"]: the
    // fragments joined and parsed with Json.parse, or an empty map when the joined text is empty or only white space. Returns nothing.
    // Example: fragments {"ci + ty": "Pa + ris"} -> block["input"] is {city=Paris}; "" -> {}
}

private fun applyMessageDelta(message: MutableMap<String, Any?>, event: Map<String, Any?>) {
    // TODO 4 of 6 (finish this to pass m1 and e5): fold a message_delta event into the message.
    // Receives the message and the event. Sets "stop_reason" and "stop_sequence" from event["delta"], and each key of event["usage"] replaces the same key in
    // the message's "usage" map (the output count is cumulative: replace, do not add). Returns nothing.
    // Example: usage {input_tokens=52, output_tokens=1} and event usage {output_tokens=38} -> {input_tokens=52, output_tokens=38}
}

private fun raiseError(event: Map<String, Any?>) {
    // TODO 5 of 6 (finish this to pass e3): an error event ends the assembly.
    // Receives the event, whose "error" map holds a "type" and a "message". Throws StreamError(type, message).
    // Example: {error={type=overloaded_error, message=Overloaded}} throws StreamError("overloaded_error", "Overloaded")
}

private fun checkComplete(message: Map<String, Any?>?, stopped: Boolean) {
    // TODO 6 of 6 (finish this to pass e4): a stream that never started or never reached message_stop is an error.
    // Receives the message (null before message_start) and whether message_stop was seen. Throws StreamError("incomplete_stream", ...) unless both hold.
    // Example: checkComplete(message, false) throws a StreamError with errorType "incomplete_stream"
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
