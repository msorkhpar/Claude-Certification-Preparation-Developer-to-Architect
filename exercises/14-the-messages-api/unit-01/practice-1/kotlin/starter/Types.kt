/** The given types: the injected transport of the conversation, and what a reply carries. */
fun interface Send {
    fun send(body: Map<String, Any?>): Map<String, Any?>
}

data class Reply(val text: String, val stopReason: String?, val truncated: Boolean)
