/** A conversation client that keeps the state the API does not. See ../../statement.md for the contract. */
private val log = System.getLogger("conversation")

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

    private fun checkText(text: String) {
        require(text.isNotBlank()) { "a turn needs text" }
    }

    private fun requestBody(): MutableMap<String, Any?> =
        linkedMapOf("model" to model, "max_tokens" to maxTokens.toLong(), "messages" to copy(history))

    private fun optionalFields(body: MutableMap<String, Any?>) {
        if (!system.isNullOrBlank()) body["system"] = system
        if (!stopSequences.isNullOrEmpty()) body["stop_sequences"] = stopSequences.toList()
    }

    private fun sendOrRollBack(body: Map<String, Any?>): Map<String, Any?> =
        try {
            send.send(body)
        } catch (e: RuntimeException) {
            history.removeAt(history.size - 1)
            throw e
        }

    private fun assistantTurn(response: Map<String, Any?>): MutableMap<String, Any?> =
        mutableMapOf("role" to "assistant", "content" to response["content"])

    private fun addUsage(usageValue: Any?) {
        (usageValue as? Map<*, *>)?.let { usage ->
            (usage["input_tokens"] as? Number)?.let { inputTokens += it.toLong() }
            (usage["output_tokens"] as? Number)?.let { outputTokens += it.toLong() }
        }
    }

    private fun makeReply(response: Map<String, Any?>): Reply {
        val replyText = (response["content"] as List<*>).filterIsInstance<Map<*, *>>().filter { it["type"] == "text" }.joinToString("") { it["text"].toString() }
        val stop = response["stop_reason"] as String?
        return Reply(replyText, stop, stop == "max_tokens")
    }

    fun say(text: String): Reply {
        log.log(System.Logger.Level.DEBUG, "say input: {0}", text)
        checkText(text)
        history.add(mutableMapOf("role" to "user", "content" to text))
        val body = requestBody()
        optionalFields(body)
        val response = sendOrRollBack(body)
        history.add(assistantTurn(response))
        addUsage(response["usage"])
        return makeReply(response)
    }

    fun history(): MutableList<MutableMap<String, Any?>> = copy(history)

    fun totals(): Map<String, Long> = linkedMapOf("input_tokens" to inputTokens, "output_tokens" to outputTokens)

    fun reset() {
        history = mutableListOf()
        inputTokens = 0
        outputTokens = 0
    }
}
