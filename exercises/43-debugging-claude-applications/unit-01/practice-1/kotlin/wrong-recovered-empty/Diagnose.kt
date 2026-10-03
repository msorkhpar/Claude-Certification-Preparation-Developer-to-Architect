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

    private fun http(event: Map<String, Any?>): List<String> {
        val status = (event["status"] as? Number)?.toInt() ?: 0
        if (status == 429) {
            if ((event["headers"] as? Map<*, *>)?.containsKey("retry-after") == true) return listOf("rate_limit", "service", "wait_retry_after")
            if (event["error_code"] == "enforced_spend_limit_reached") return listOf("spend_cap", "account", "wait_for_reset")
            return listOf("rate_limit", "service", "retry_backoff")
        }
        if (status == 400 && (event["message"] as? String ?: "").lowercase().contains("spend limit")) return listOf("spend_limit", "account", "raise_limit")
        return HTTP[status] ?: if (status >= 500) HTTP.getValue(500) else HTTP.getValue(400)
    }

    private fun hasJsonObject(text: String): Boolean {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end < start) return false
        return try { Json.parse(text.substring(start, end + 1)) is Map<*, *> } catch (e: RuntimeException) { false }
    }

    private fun classify(event: Map<String, Any?>, tools: List<*>?, lastBlocks: List<*>): List<String>? {
        when (event["kind"]) {
            "error" -> return http(event)
            "network_error" -> return listOf("network", "service", "retry_backoff")
            "response" -> if ((event["status"] as? Number)?.toInt() == 200) {
                val reason = event["stop_reason"]
                STOP[reason]?.let { return it }
                if (reason == "end_turn" && (event["content"] as? List<*>).isNullOrEmpty()) {
                    val at = lastBlocks.indexOf("tool_result")
                    return if (at >= 0 && "text" in lastBlocks.subList(at, lastBlocks.size)) listOf("empty_response", "integration", "remove_text_after_tool_result")
                    else listOf("empty_response", "model", "add_continue_prompt")
                }
            }
            "tool_call" -> if (tools != null && event["name"] !in tools) return listOf("unknown_tool", "model", "return_error_result")
            "tool_result" -> if (!(event["exception"] as? String).isNullOrEmpty()) return listOf("tool_exception", "integration", "fix_tool_code")
            "parse" -> if (event["ok"] == false) {
                return if (hasJsonObject(event["text"] as? String ?: "")) listOf("parse_failure", "integration", "extract_json")
                else listOf("parse_failure", "model", "validate_and_retry")
            }
        }
        return null
    }

    /** The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered. */
    fun diagnose(trace: List<Map<String, Any?>>): Map<String, Any?> {
        var tools: List<*>? = null
        var lastBlocks: List<*> = emptyList<Any?>()
        for ((i, event) in trace.withIndex()) {
            if (event["kind"] == "request") {
                tools = event["tools"] as? List<*>
                lastBlocks = event["last_user_blocks"] as? List<*> ?: emptyList<Any?>()
                continue
            }
            val found = classify(event, tools, lastBlocks) ?: continue
            val recovered = trace.drop(i + 1).any {
                it["kind"] == "response" && (it["status"] as? Number)?.toInt() == 200 && it["stop_reason"] == "end_turn"
            }
            return linkedMapOf("index" to i, "type" to found[0], "origin" to found[1], "recovery" to found[2], "recovered" to recovered)
        }
        return linkedMapOf("index" to -1, "type" to "ok", "origin" to "none", "recovery" to "none", "recovered" to false)
    }
}
