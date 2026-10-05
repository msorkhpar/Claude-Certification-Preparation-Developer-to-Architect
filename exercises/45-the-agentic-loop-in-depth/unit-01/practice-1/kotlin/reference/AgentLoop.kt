private val log = System.getLogger("agent")

/** The agent loop, driven by the stop reason. See ../../statement.md. Messages and replies are JSON-like maps. */

typealias Model = (List<Map<String, Any?>>) -> Map<String, Any?>
typealias Tools = Map<String, (Map<String, Any?>) -> String>

private fun textOf(content: List<Map<String, Any?>>): String = content.filter { it["type"] == "text" }.joinToString("") { (it["text"] as String?) ?: "" }

private fun callsOf(content: List<Map<String, Any?>>): List<Map<String, Any?>> = content.filter { it["type"] == "tool_use" }

private fun toolResult(block: Map<String, Any?>, content: String, isError: Boolean = false): Map<String, Any?> {
    val result = linkedMapOf<String, Any?>("type" to "tool_result", "tool_use_id" to block["id"], "content" to content)
    if (isError) result["is_error"] = true
    return result
}

private fun statusFor(reason: Any?, calls: List<Map<String, Any?>>): String = when {
    reason == "tool_use" && calls.isEmpty() -> "malformed"
    reason == "end_turn" || reason == "stop_sequence" -> "done"
    reason == "max_tokens" -> "truncated"
    reason == "refusal" -> "refused"
    else -> "unexpected"
}

private fun atLimit(turns: Int, maxTurns: Int): Boolean = turns >= maxTurns

private fun snapshot(messages: List<Map<String, Any?>>): List<Map<String, Any?>> = messages.toList()

@Suppress("UNCHECKED_CAST")
private fun runTool(block: Map<String, Any?>, tools: Tools): Map<String, Any?> {
    val name = block["name"] as String
    val handler = tools[name]
        ?: return toolResult(block, "Unknown tool: $name", true)
    return try {
        toolResult(block, handler((block["input"] as Map<String, Any?>?) ?: emptyMap()))
    } catch (error: RuntimeException) { // a failing tool is a result for the model, not a crash of the loop
        toolResult(block, error.message.toString(), true)
    }
}

private fun outcome(status: String, text: String, turns: Int, messages: List<Map<String, Any?>>): Map<String, Any?> =
    linkedMapOf("status" to status, "text" to text, "turns" to turns, "messages" to messages)

@Suppress("UNCHECKED_CAST")
fun runAgent(model: Model, tools: Tools, task: String, maxTurns: Int = 8): Map<String, Any?>? {
    log.log(System.Logger.Level.DEBUG, "runAgent input: {0}", task)
    val messages = mutableListOf<Map<String, Any?>>(linkedMapOf("role" to "user", "content" to task))
    var turns = 0
    var lastText = ""
    while (true) {
        if (atLimit(turns, maxTurns)) return outcome("max_turns", lastText, turns, messages) // the count is a backstop: it only ends a run the model has not ended itself
        turns++
        val reply = model(snapshot(messages))
        val content = reply["content"] as List<Map<String, Any?>>
        lastText = textOf(content)
        messages.add(linkedMapOf("role" to "assistant", "content" to content))
        val reason = reply["stop_reason"]
        val calls = callsOf(content)
        if (reason != "tool_use" || calls.isEmpty()) return outcome(statusFor(reason, calls), lastText, turns, messages)
        messages.add(linkedMapOf("role" to "user", "content" to calls.map { runTool(it, tools) }))
    }
}
