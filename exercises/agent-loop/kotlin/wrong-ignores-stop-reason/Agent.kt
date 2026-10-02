typealias Msg = Map<String, Any?>

@Suppress("UNCHECKED_CAST")
private fun textOf(content: Any?): String =
    (content as List<Msg>).filter { it["type"] == "text" }.joinToString("") { it["text"] as String }

@Suppress("UNCHECKED_CAST")
fun runAgent(model: (List<Msg>) -> Msg, tools: Map<String, (Map<String, Any?>) -> Any?>, userText: String, maxTurns: Int = 5): String {
    val messages = mutableListOf<Msg>(mapOf("role" to "user", "content" to userText))
    repeat(maxTurns) {
        val resp = model(messages)
        messages += mapOf("role" to "assistant", "content" to resp["content"])
        if (textOf(resp["content"]).isNotEmpty()) return textOf(resp["content"])
        val results = mutableListOf<Msg>()
        for (block in resp["content"] as List<Msg>) {
            if (block["type"] != "tool_use") continue
            val id = block["id"]
            results += try {
                val tool = tools[block["name"]] ?: throw IllegalArgumentException("unknown tool ${block["name"]}")
                mapOf("type" to "tool_result", "tool_use_id" to id, "content" to tool(block["input"] as Map<String, Any?>).toString())
            } catch (e: RuntimeException) {
                mapOf("type" to "tool_result", "tool_use_id" to id, "content" to (e.message ?: ""), "is_error" to true)
            }
        }
        messages += mapOf("role" to "user", "content" to results)
    }
    throw IllegalStateException("max_turns exceeded")
}
