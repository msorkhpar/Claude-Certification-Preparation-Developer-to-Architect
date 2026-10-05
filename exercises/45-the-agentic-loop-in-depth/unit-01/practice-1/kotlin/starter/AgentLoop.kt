private val log = System.getLogger("agent")

/** The agent loop, driven by the stop reason. See ../../statement.md. Messages and replies are JSON-like maps. */

typealias Model = (List<Map<String, Any?>>) -> Map<String, Any?>
typealias Tools = Map<String, (Map<String, Any?>) -> String>

/**
 * TODO 1 of 6 (unlocks m1 and e5): the text of a reply.
 * Receives a reply's content list of blocks. Returns the "text" of every block whose type is "text", joined with nothing between.
 * Example: [text "Hi ", tool_use, text "there"] -> "Hi there"
 */
private fun textOf(content: List<Map<String, Any?>>): String = ""

/**
 * TODO 2 of 6 (unlocks e2 and e6): the tool calls of a reply.
 * Receives a reply's content list. Returns its tool_use blocks, in order; an empty list when there are none.
 * Example: [text "x", tool_use a] -> [tool_use a]
 */
private fun callsOf(content: List<Map<String, Any?>>): List<Map<String, Any?>> = emptyList()

/**
 * TODO 3 of 6 (unlocks m1, e2 and e3): one tool_result block.
 * Receives the tool_use block, the result text and a flag. Returns {type: tool_result, tool_use_id: the block's id, content: the text},
 * with is_error true added only when the flag is set (a good result has no is_error key).
 * Example: (block with id t1, "boom", true) -> {type: tool_result, tool_use_id: t1, content: boom, is_error: true}
 */
private fun toolResult(block: Map<String, Any?>, content: String, isError: Boolean = false): Map<String, Any?> = linkedMapOf()

/**
 * TODO 4 of 6 (unlocks m1, e5 and e6): the status a stop reason ends the run with (the loop only gets here when it cannot go on).
 * Receives the stop reason and the tool calls of the reply. Returns "malformed" for tool_use with no calls, "done" for end_turn and
 * stop_sequence, "truncated" for max_tokens, "refused" for refusal and "unexpected" for anything else.
 * Example: ("max_tokens", no calls) -> "truncated", ("tool_use", no calls) -> "malformed", ("brand_new", no calls) -> "unexpected"
 */
private fun statusFor(reason: Any?, calls: List<Map<String, Any?>>): String = ""

/**
 * TODO 5 of 6 (unlocks e4): has the turn limit been reached before the next model call?
 * Receives the model calls made so far and the limit. Returns true when no call is left. Example: (3, 3) -> true, (2, 3) -> false
 */
private fun atLimit(turns: Int, maxTurns: Int): Boolean = false

/**
 * TODO 6 of 6 (unlocks m1): the copy of the messages that the model is handed.
 * Receives the list of messages so far. Returns a new list with the same items, so later changes do not rewrite what the model saw.
 * Example: [a, b] -> a new list [a, b]
 */
private fun snapshot(messages: List<Map<String, Any?>>): List<Map<String, Any?>> = messages

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
