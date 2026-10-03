/** Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md. Results are JSON-like maps. */

/** Models whose API rejects tool_choice any and tool, as read on 2026-10-03. */
@Suppress("unused")
private val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

fun assignTools(roles: Map<String, Map<String, Any?>>, catalog: List<Map<String, Any?>>, budget: Int = 5): Map<String, List<String>>? {
    // TODO: give each role the tools of its specialisation, plus what it is explicitly granted; refuse a set that is too large.
    return null
}

fun planTurn(model: String, need: String, tools: List<String>, forced: String? = null, manualThinking: Boolean = false): Map<String, Any?>? {
    // TODO: the tool_choice and tool list of one request, with the portable fallback where forcing is not accepted.
    return null
}

fun cacheImpact(previous: Map<String, Any?>?, next: Map<String, Any?>): String? {
    // TODO: what does a change between two requests cost in prompt caching: none, messages or all?
    return null
}

fun checkTurn(blocks: List<Map<String, Any?>>, need: String, forced: String? = null): String? {
    // TODO: compare a reply with the call that was required: ok, missed_call or wrong_tool.
    return null
}

fun authorize(call: Map<String, Any?>, policy: Map<String, Any?>, approvals: Collection<String>): Map<String, Any?>? {
    // TODO: decide a call to a tool that cannot be undone, in the tool layer.
    return null
}
