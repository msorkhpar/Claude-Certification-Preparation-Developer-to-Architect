/** Hand-written, illustrative scripted model: replies in the Messages API shape (maps and lists). */

fun text(t: String): Msg = mapOf("type" to "text", "text" to t)
fun toolUse(id: String, name: String, input: Map<String, Any?> = emptyMap()): Msg =
    mapOf("type" to "tool_use", "id" to id, "name" to name, "input" to input)
fun reply(content: List<Msg>, stopReason: String): Msg =
    mapOf("type" to "message", "role" to "assistant", "content" to content, "stop_reason" to stopReason)

class ScriptedModel(vararg replies: Msg) : (List<Msg>) -> Msg {
    private val queue = ArrayDeque(replies.toList())
    val seen = mutableListOf<List<Msg>>()
    override fun invoke(messages: List<Msg>): Msg {
        seen += messages.toList()
        return queue.removeFirstOrNull() ?: throw AssertionError("scripted model ran out of replies")
    }
}
