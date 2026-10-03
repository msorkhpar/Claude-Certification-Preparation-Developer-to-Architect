/** A conversation client that keeps the state the API does not. See ../../statement.md for the contract. */
class Conversation(
    private val send: Send,
    private val model: String,
    private val maxTokens: Int,
    private val system: String? = null,
    private val stopSequences: List<String>? = null,
) {
    fun say(text: String): Reply {
        // TODO: add the user turn, send the whole history, keep the assistant turn, count usage.
        return Reply("", "", false)
    }

    fun history(): MutableList<MutableMap<String, Any?>> = mutableListOf()

    fun totals(): Map<String, Long> = linkedMapOf("input_tokens" to 0L, "output_tokens" to 0L)

    fun reset() {}
}
