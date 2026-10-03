/** The agent loop, driven by the stop reason. See ../../statement.md. Messages and replies are JSON-like maps. */

typealias Model = (List<Map<String, Any?>>) -> Map<String, Any?>
typealias Tools = Map<String, (Map<String, Any?>) -> String>

private fun textOf(content: List<Map<String, Any?>>): String = content.filter { it["type"] == "text" }.joinToString("") { (it["text"] as String?) ?: "" }

@Suppress("UNCHECKED_CAST")
private fun runTool(block: Map<String, Any?>, tools: Tools): Map<String, Any?> {
    val name = block["name"] as String
    val handler = tools[name]
        ?: return linkedMapOf("type" to "tool_result", "tool_use_id" to block["id"], "content" to "Unknown tool: $name", "is_error" to true)
    return try {
        linkedMapOf("type" to "tool_result", "tool_use_id" to block["id"], "content" to handler((block["input"] as Map<String, Any?>?) ?: emptyMap()))
    } catch (error: RuntimeException) { // a failing tool is a result for the model, not a crash of the loop
        linkedMapOf("type" to "tool_result", "tool_use_id" to block["id"], "content" to error.message.toString(), "is_error" to true)
    }
}

private fun outcome(status: String, text: String, turns: Int, messages: List<Map<String, Any?>>): Map<String, Any?> =
    linkedMapOf("status" to status, "text" to text, "turns" to turns, "messages" to messages)

@Suppress("UNCHECKED_CAST")
fun runAgent(model: Model, tools: Tools, task: String, maxTurns: Int = 8): Map<String, Any?>? {
    val messages = mutableListOf<Map<String, Any?>>(linkedMapOf("role" to "user", "content" to task))
    var turns = 0
    var lastText = ""
    while (true) {
        if (turns >= maxTurns) return outcome("max_turns", lastText, turns, messages) // the count is a backstop: it only ends a run the model has not ended itself
        turns++
        val reply = model(messages.toList())
        val content = reply["content"] as List<Map<String, Any?>>
        lastText = textOf(content)
        messages.add(linkedMapOf("role" to "assistant", "content" to content))
        if (lastText.lowercase().contains("done")) return outcome("done", lastText, turns, messages)
        when (reply["stop_reason"]) {
            "tool_use" -> {
                val calls = content.filter { it["type"] == "tool_use" }
                if (calls.isEmpty()) return outcome("malformed", lastText, turns, messages)
                messages.add(linkedMapOf("role" to "user", "content" to calls.map { runTool(it, tools) }))
            }
            "end_turn", "stop_sequence" -> return outcome("done", lastText, turns, messages)
            "max_tokens" -> return outcome("truncated", lastText, turns, messages)
            "refusal" -> return outcome("refused", lastText, turns, messages)
            else -> return outcome("unexpected", lastText, turns, messages)
        }
    }
}
