/** A conversation client that keeps the state the API does not. See ../../statement.md for the contract. */
class Conversation(
    private val send: Send,
    private val model: String,
    private val maxTokens: Int,
    private val system: String? = null,
    private val stopSequences: List<String>? = null,
) {
    private var history = mutableListOf<MutableMap<String, Any?>>()
    private var inputTokens = 0L
    private var outputTokens = 0L

    @Suppress("UNCHECKED_CAST")
    private fun copy(turns: List<Map<String, Any?>>): MutableList<MutableMap<String, Any?>> =
        (Json.parse(Json.stringify(turns)) as List<Map<String, Any?>>).map { it.toMutableMap() }.toMutableList()

    fun say(text: String): Reply {
        require(text.isNotBlank()) { "a turn needs text" }
        history.add(mutableMapOf("role" to "user", "content" to text))
        val body = linkedMapOf<String, Any?>("model" to model, "max_tokens" to maxTokens.toLong(), "messages" to copy(history))
        if (!system.isNullOrBlank()) body["system"] = system
        if (!stopSequences.isNullOrEmpty()) body["stop_sequences"] = stopSequences.toList()
        val response = try {
            send.send(body)
        } catch (e: RuntimeException) {
            history.removeAt(history.size - 1)
            throw e
        }
        history.add(mutableMapOf("role" to "assistant", "content" to response["content"]))
        (response["usage"] as? Map<*, *>)?.let { usage ->
            (usage["input_tokens"] as? Number)?.let { inputTokens += it.toLong() }
            (usage["output_tokens"] as? Number)?.let { outputTokens += it.toLong() }
        }
        val replyText = (response["content"] as List<*>).filterIsInstance<Map<*, *>>().filter { it["type"] == "text" }.joinToString("") { it["text"].toString() }
        val stop = response["stop_reason"] as String?
        return Reply(replyText, stop, stop == "max_tokens")
    }

    fun history(): MutableList<MutableMap<String, Any?>> = copy(history)

    fun totals(): Map<String, Long> = linkedMapOf("input_tokens" to inputTokens, "output_tokens" to outputTokens)

    fun reset() {
        history = mutableListOf()
        inputTokens = 0
        outputTokens = 0
    }
}
