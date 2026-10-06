private val log = System.getLogger("toolloop")

/** A tool loop against a scripted model. See ../../statement.md. Messages and replies are JSON-like maps. */

private val FORCED_UNSUPPORTED = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")
private val CHOICE_TYPES = setOf("auto", "any", "tool", "none")

/** Refuse a tool_choice the API would reject. */
private fun checkChoice(tools: List<Tool>, model: String, choice: Map<String, Any?>) {
    val kind = choice["type"]
    if (kind !in CHOICE_TYPES) throw RequestError("tool_choice.type", "must be auto, any, tool or none")
    if (choice.containsKey("disable_parallel_tool_use") && choice["disable_parallel_tool_use"] !is Boolean) throw RequestError("tool_choice.disable_parallel_tool_use", "must be a boolean")
    if ((kind == "any" || kind == "tool") && model in FORCED_UNSUPPORTED) throw RequestError("tool_choice", "$model does not support forced tool use")
    if (kind == "tool" && tools.none { it.name == choice["name"] }) throw RequestError("tool_choice.name", "names no tool in the request")
}

/** The keys listed in the tool's required list that the input lacks. */
@Suppress("UNCHECKED_CAST")
private fun missingInputs(tool: Tool, input: Map<String, Any?>): List<Any?> =
    ((tool.inputSchema["required"] as? List<Any?>) ?: emptyList()).filter { !input.containsKey(it) }

/** The content of a tool result: a string as it is, any other value as JSON text. */
private fun resultContent(out: Any?): String = if (out is String) out else Json.stringify(out)

@Suppress("UNCHECKED_CAST")
private fun runOne(tools: List<Tool>, block: Map<String, Any?>): Map<String, Any?> {
    val result = linkedMapOf<String, Any?>("type" to "tool_result", "tool_use_id" to block["id"])
    val tool = tools.firstOrNull { it.name == block["name"] }
    if (tool == null) {
        result["content"] = "Unknown tool: ${block["name"]}"
        result["is_error"] = true
        return result
    }
    val input = block["input"] as Map<String, Any?>
    val missing = missingInputs(tool, input)
    if (missing.isNotEmpty()) {
        result["content"] = "Missing required input: ${missing.joinToString(", ")}"
        result["is_error"] = true
        return result
    }
    try {
        val out = tool.handler(input)
        result["content"] = resultContent(out)
    } catch (e: RuntimeException) { // a tool that fails must not end the loop
        result["content"] = e.message
        result["is_error"] = true
    }
    return result
}

private fun textOf(content: List<Map<String, Any?>>): String = content.filter { it["type"] == "text" }.joinToString("") { it["text"] as String }

/** One tool_result per tool_use block of the reply, in order. */
private fun toolResults(tools: List<Tool>, content: List<Map<String, Any?>>): List<Map<String, Any?>> =
    content.filter { it["type"] == "tool_use" }.map { runOne(tools, it) }

/** The status of a reply that ends the loop: done, refused or truncated. */
private fun finalStatus(stop: Any?): String = when (stop) {
    "end_turn", "stop_sequence" -> "done"
    "refusal" -> "refused"
    else -> "truncated" // max_tokens, model_context_window_exceeded: the answer is cut off
}

/** The last turn number the loop may use. */
private fun lastTurn(maxTurns: Int): Int = maxTurns

/** The tool_choice to send on this turn: a forced choice only on the first request. */
private fun sentChoice(toolChoice: Map<String, Any?>, turn: Int): Map<String, Any?> {
    val forced = toolChoice["type"] == "any" || toolChoice["type"] == "tool"
    return if (forced && turn > 1) mapOf("type" to "auto") else toolChoice
}

private fun outcome(status: String, text: String, turns: Int, messages: List<Map<String, Any?>>): Map<String, Any?> =
    linkedMapOf("status" to status, "text" to text, "turns" to turns, "messages" to messages)

/** Call the model, run every tool it asks for, send the results back, until it ends its turn or a limit is hit. */
@Suppress("UNCHECKED_CAST")
fun runAgent(ask: Ask, tools: List<Tool>, userText: String, model: String = "claude-sonnet-5-5", maxTurns: Int = 8, toolChoice: Map<String, Any?>? = null): Map<String, Any?> {
    log.log(System.Logger.Level.DEBUG, "runAgent input: {0}", userText)
    if (toolChoice != null) checkChoice(tools, model, toolChoice)
    val definitions = tools.map { mapOf("name" to it.name, "description" to it.description, "input_schema" to it.inputSchema) }
    val messages = mutableListOf<Map<String, Any?>>(mapOf("role" to "user", "content" to userText))
    var text = ""
    var calls = 0
    for (turn in 1..lastTurn(maxTurns)) {
        val request = linkedMapOf<String, Any?>("model" to model, "max_tokens" to 1024, "messages" to messages.toList(), "tools" to definitions)
        if (toolChoice != null) request["tool_choice"] = sentChoice(toolChoice, turn)
        val reply = ask(request)
        calls = turn
        val content = reply["content"] as List<Map<String, Any?>>
        messages += mapOf("role" to "assistant", "content" to content)
        text = textOf(content)
        when (reply["stop_reason"]) {
            "tool_use" -> messages += mapOf("role" to "user", "content" to toolResults(tools, content))
            "pause_turn" -> continue
            else -> return outcome(finalStatus(reply["stop_reason"]), text, calls, messages)
        }
    }
    return outcome("max_turns", text, calls, messages)
}
