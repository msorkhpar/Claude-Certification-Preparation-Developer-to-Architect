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
        // TODO 1 of 8 (finish this to pass e6): refuse a blank turn before anything is sent.
        // Receives the text of the turn. Throws IllegalArgumentException (use require) when it is empty or only whitespace; otherwise
        // returns nothing. Example: checkText("   ") -> IllegalArgumentException, checkText("hi") -> nothing
    }

    private fun requestBody(): MutableMap<String, Any?> {
        // TODO 2 of 8 (finish this to pass m1 and e5): the request body, a snapshot.
        // Receives nothing (it reads the fields). Returns a map with "model", "max_tokens" (as a Long) and "messages" = copy(history), a
        // deep copy of the whole history so far (the new user turn is already in it), so a later turn cannot change a request already sent.
        // Example: after say("a"), the first body has messages [{role=user, content=a}]
        return linkedMapOf("model" to model, "max_tokens" to maxTokens.toLong(), "messages" to emptyList<Any?>())
    }

    private fun optionalFields(body: MutableMap<String, Any?>) {
        // TODO 3 of 8 (finish this to pass e4): the top-level fields that are only sometimes there.
        // Receives the body and adds to it: "system" (a top-level field, never a message) when system is not blank, and
        // "stop_sequences" (a copy of the list) when stopSequences is given and not empty. Returns nothing.
        // Example: with system "Be brief." the body gains system=Be brief.; with system "  " it gains nothing
    }

    private fun sendOrRollBack(body: Map<String, Any?>): Map<String, Any?> {
        // TODO 4 of 8 (finish this to pass e2): send the body, and leave no dangling user turn when the call fails.
        // Receives the body. Returns send.send(body). When send throws a RuntimeException, removes the user turn that say added to
        // history and throws the same exception again, so roles keep alternating on the next call.
        return send.send(body)
    }

    private fun assistantTurn(response: Map<String, Any?>): MutableMap<String, Any?> {
        // TODO 5 of 8 (finish this to pass m1): the turn to store for the reply.
        // Receives the response. Returns a map with "role" = "assistant" and "content" = the response's content list, as received.
        // Example: content [{type=text, text=Paris.}] -> {role=assistant, content=[that same list]}
        return mutableMapOf("role" to "assistant", "content" to emptyList<Any?>())
    }

    private fun addUsage(usageValue: Any?) {
        // TODO 6 of 8 (finish this to pass e1): keep the running totals.
        // Receives the response's usage (a Map, or null; a key may be missing). Adds its input_tokens and output_tokens to the
        // inputTokens and outputTokens fields. Returns nothing.
        // Example: inputTokens 12 plus usage {input_tokens=30, output_tokens=9} -> inputTokens 42
    }

    private fun makeReply(response: Map<String, Any?>): Reply {
        // TODO 7 of 8 (finish this to pass m1 and e3): what say returns.
        // Receives the response. Returns a Reply: the text of the content blocks whose type is "text" joined with nothing between
        // them, the response's stop_reason, and truncated, true only when the stop reason is "max_tokens".
        // Example: stop_reason "max_tokens" -> truncated true; "end_turn" -> truncated false
        return Reply("", "", false)
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

    fun history(): MutableList<MutableMap<String, Any?>> {
        // TODO 8 of 8 (finish this to pass e5): the turns so far, as a copy.
        // Returns copy(history), so changing what the caller gets changes nothing here.
        // Example: chat.history().add(x) leaves chat.history().size unchanged
        return mutableListOf()
    }

    fun totals(): Map<String, Long> = linkedMapOf("input_tokens" to inputTokens, "output_tokens" to outputTokens)

    fun reset() {
        history = mutableListOf()
        inputTokens = 0
        outputTokens = 0
    }
}
