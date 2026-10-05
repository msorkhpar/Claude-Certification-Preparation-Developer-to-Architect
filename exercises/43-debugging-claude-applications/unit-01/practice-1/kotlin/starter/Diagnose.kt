private val log = System.getLogger("diagnose")
/** Diagnose a failure from a trace. See ../../statement.md. */
object Diagnose {
    private val HTTP = mapOf(
        400 to listOf("invalid_request", "integration", "fix_request"),
        401 to listOf("authentication", "account", "fix_credentials"),
        402 to listOf("billing", "account", "fix_billing"),
        403 to listOf("permission", "account", "fix_access"),
        404 to listOf("not_found", "integration", "fix_request"),
        409 to listOf("conflict", "integration", "resolve_then_retry"),
        413 to listOf("request_too_large", "integration", "shrink_request"),
        500 to listOf("server_error", "service", "retry_backoff"),
        504 to listOf("timeout", "service", "stream_or_batch"),
        529 to listOf("overloaded", "service", "retry_backoff"))
    private val STOP = mapOf(
        "max_tokens" to listOf("truncated", "integration", "raise_max_tokens"),
        "model_context_window_exceeded" to listOf("context_exceeded", "integration", "trim_context"),
        "refusal" to listOf("refusal", "model", "fallback_model"),
        "pause_turn" to listOf("paused", "integration", "continue_turn"))

    // TODO 1 of 6 (unlocks e1): the diagnosis of a 429.
    // Receives the error event. Returns listOf("rate_limit", "service", "wait_retry_after") when its "headers" map has a "retry-after" key (this wins),
    // else listOf("spend_cap", "account", "wait_for_reset") when "error_code" is "enforced_spend_limit_reached", else listOf("rate_limit", "service",
    // "retry_backoff"). Example: a 429 with error_code enforced_spend_limit_reached and no headers -> the spend_cap triple
    private fun rateLimit(event: Map<String, Any?>): List<String> = listOf("rate_limit", "service", "retry_backoff")

    // TODO 6 of 6 (unlocks e1 and m1): the triple for a status.
    // Receives the status number. Returns HTTP[status] for a documented status; any other status falls back by class: 500 and above use
    // the 500 row, everything else the 400 row. Example: byStatus(502) -> HTTP.getValue(500), byStatus(418) -> HTTP.getValue(400)
    private fun byStatus(status: Int): List<String> = HTTP.getValue(400)
    private fun http(event: Map<String, Any?>): List<String> {
        val status = (event["status"] as? Number)?.toInt() ?: 0
        if (status == 429) return rateLimit(event)
        if (status == 400 && (event["message"] as? String ?: "").lowercase().contains("spend limit")) return listOf("spend_limit", "account", "raise_limit")
        return byStatus(status)
    }

    // TODO 2 of 6 (unlocks e4): does the text hold a JSON object?
    // Receives a text. Returns true when the span from the first '{' to the last '}' parses as JSON (use Json.parse) and is an object
    // (a Map, not a list or a number); false for no braces, a broken span or another JSON type.
    // Example: hasJsonObject("ok {\"a\": 1} done") -> true, hasJsonObject("see {nope}") -> false
    private fun hasJsonObject(text: String): Boolean = false

    // TODO 3 of 6 (unlocks e3): who is to blame for an empty end turn?
    // Receives the block types of the last user message, in order. Returns listOf("empty_response", "integration", "remove_text_after_tool_result")
    // when a "text" block comes after a "tool_result" block, else listOf("empty_response", "model", "add_continue_prompt").
    // Example: ["tool_result", "text"] -> integration; ["text", "tool_result"] -> model
    private fun emptyOrigin(lastBlocks: List<*>): List<String> = listOf("empty_response", "model", "add_continue_prompt")

    // TODO 4 of 6 (unlocks e5): is this tool event a failure, and whose?
    // Receives an event and the tool names of the last request (null when unknown). Returns listOf("unknown_tool", "model", "return_error_result")
    // for a "tool_call" whose name is not in tools, listOf("tool_exception", "integration", "fix_tool_code") for a "tool_result" with a non-empty
    // "exception" string, and null otherwise (an is_error flag alone is not our failure).
    // Example: a tool_call named get_wether with tools [get_weather] -> the unknown_tool triple
    private fun toolFailure(event: Map<String, Any?>, tools: List<*>?): List<String>? = null

    // TODO 5 of 6 (unlocks e6): did a later response recover from the failure at index i?
    // Receives the trace and the index of the failure. Returns true when a later event is a response with status 200, stop_reason
    // "end_turn" and a non-empty content list; false otherwise. Example: a failure at 1 and a good end_turn at 5 -> true
    private fun recoveredAfter(trace: List<Map<String, Any?>>, i: Int): Boolean = false

    private fun classify(event: Map<String, Any?>, tools: List<*>?, lastBlocks: List<*>): List<String>? {
        when (event["kind"]) {
            "error" -> return http(event)
            "network_error" -> return listOf("network", "service", "retry_backoff")
            "response" -> if ((event["status"] as? Number)?.toInt() == 200) {
                val reason = event["stop_reason"]
                STOP[reason]?.let { return it }
                if (reason == "end_turn" && (event["content"] as? List<*>).isNullOrEmpty()) return emptyOrigin(lastBlocks)
            }
            "tool_call", "tool_result" -> toolFailure(event, tools)?.let { return it }
            "parse" -> if (event["ok"] == false) {
                return if (hasJsonObject(event["text"] as? String ?: "")) listOf("parse_failure", "integration", "extract_json")
                else listOf("parse_failure", "model", "validate_and_retry")
            }
        }
        return null
    }

    /** The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered. */
    fun diagnose(trace: List<Map<String, Any?>>): Map<String, Any?> {
        log.log(System.Logger.Level.DEBUG, "diagnose input: {0}", trace)
        var tools: List<*>? = null
        var lastBlocks: List<*> = emptyList<Any?>()
        for ((i, event) in trace.withIndex()) {
            if (event["kind"] == "request") {
                tools = event["tools"] as? List<*>
                lastBlocks = event["last_user_blocks"] as? List<*> ?: emptyList<Any?>()
                continue
            }
            val found = classify(event, tools, lastBlocks) ?: continue
            val recovered = recoveredAfter(trace, i)
            return linkedMapOf("index" to i, "type" to found[0], "origin" to found[1], "recovery" to found[2], "recovered" to recovered)
        }
        return linkedMapOf("index" to -1, "type" to "ok", "origin" to "none", "recovery" to "none", "recovered" to false)
    }
}
