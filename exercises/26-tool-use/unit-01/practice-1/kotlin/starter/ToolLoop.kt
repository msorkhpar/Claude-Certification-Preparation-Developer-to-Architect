private val log = System.getLogger("toolloop")

/** A tool loop against a scripted model. See ../../statement.md. Messages and replies are JSON-like maps. */

private val FORCED_UNSUPPORTED = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")
private val CHOICE_TYPES = setOf("auto", "any", "tool", "none")

/** Refuse a tool_choice the API would reject. */
private fun checkChoice(tools: List<Tool>, model: String, choice: Map<String, Any?>) {
    // TODO 1 of 7 (finish this to pass e5): refuse a tool_choice the API would reject.
    // Receives the tools, the model id and the tool_choice map. Throws RequestError, in this order: field "tool_choice.type" unless the type is in
    // CHOICE_TYPES; "tool_choice.disable_parallel_tool_use" when that key is present and not a Boolean; "tool_choice" for type "any" or "tool" on a
    // model in FORCED_UNSUPPORTED; "tool_choice.name" for type "tool" when no tool has that name. Returns normally otherwise.
    // Example: checkChoice(emptyList(), "claude-opus-5-5", mapOf("type" to "any")) throws a RequestError with field "tool_choice"
}

/** The keys listed in the tool's required list that the input lacks. */
@Suppress("UNCHECKED_CAST")
private fun missingInputs(tool: Tool, input: Map<String, Any?>): List<Any?> {
    // TODO 2 of 7 (finish this to pass e2): the required keys a call leaves out.
    // Receives a tool and the input map of the call. Returns the keys in the tool's inputSchema "required" list that the input lacks, in the
    // order of that list; an empty list when none is missing.
    // Example: a tool whose inputSchema requires "city", with an empty input -> [city]
    return emptyList()
}

/** The content of a tool result: a string as it is, any other value as JSON text. */
private fun resultContent(out: Any?): String {
    // TODO 3 of 7 (finish this to pass e6): the content of a tool_result.
    // Receives what a handler returned. Returns a String as it is and any other value as JSON text (use Json.stringify).
    // Example: resultContent(mapOf("city" to "Oslo")) -> {"city":"Oslo"}
    return out.toString()
}

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
private fun toolResults(tools: List<Tool>, content: List<Map<String, Any?>>): List<Map<String, Any?>> {
    // TODO 4 of 7 (finish this to pass m1 and e1): run every tool call of a reply.
    // Receives the tools and the content blocks of the reply. Returns one tool_result (from runOne) per block of type "tool_use", in the
    // order of the blocks; blocks of any other type, such as "server_tool_use", get no result.
    // Example: [a text block, a tool_use block] -> a list with one tool_result
    return emptyList()
}

/** The status of a reply that ends the loop: done, refused or truncated. */
private fun finalStatus(stop: Any?): String {
    // TODO 5 of 7 (finish this to pass e4): the status of a reply that ends the loop.
    // Receives the stop_reason. Returns "done" for "end_turn" and "stop_sequence", "refused" for "refusal" and "truncated" for any other.
    // Example: finalStatus("max_tokens") -> "truncated"
    return "done"
}

/** The last turn number the loop may use. */
private fun lastTurn(maxTurns: Int): Int {
    // TODO 6 of 7 (finish this to pass e3): the last turn number the loop may use.
    // Receives maxTurns. Returns the highest turn number, so that at most maxTurns calls are made.
    // Example: lastTurn(3) -> 3
    return 1
}

/** The tool_choice to send on this turn: a forced choice only on the first request. */
private fun sentChoice(toolChoice: Map<String, Any?>, turn: Int): Map<String, Any?> {
    // TODO 7 of 7 (finish this to pass e5): the tool_choice to send on a turn.
    // Receives the tool_choice map and the turn number (1 for the first request). A forced choice (type "any" or "tool") is sent as given on
    // turn 1 and as {type=auto} from turn 2 on; "auto" and "none" are sent unchanged every turn.
    // Example: sentChoice(mapOf("type" to "any"), 2) -> {type=auto}
    return toolChoice
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
