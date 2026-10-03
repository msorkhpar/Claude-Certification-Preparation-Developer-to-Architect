/** Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md. Results are JSON-like maps. */

private val KINDS = mapOf("transient" to true, "validation" to false, "permission" to false, "business" to false, "outcome_unknown" to false, "internal" to false)
private val GENERIC = setOf("", "error", "failed", "failure", "operation failed", "something went wrong", "unknown error")
private val ACTIONS = mapOf("transient" to "retry_later", "validation" to "repair_input", "permission" to "escalate", "business" to "explain", "outcome_unknown" to "verify_first", "internal" to "escalate")

/** What a tool throws. kind is transient, validation, permission, business or timeout (no answer was received, so the effect is unknown). */
class ToolError(val kind: String, message: String, val retryAfterMs: Int? = null, val explanation: String? = null) : RuntimeException(message)

/** The structured result of a failed call. */
fun makeError(kind: String, message: String, explanation: String? = null, attempts: Int = 1, attempted: Map<String, Any?>? = null): Map<String, Any?> {
    require(kind in KINDS) { "unknown error kind: $kind" }
    require(message.trim().lowercase().trimEnd('.') !in GENERIC) { "an error message must say what went wrong and what to do" }
    val error = linkedMapOf<String, Any?>("is_error" to true, "category" to kind, "retryable" to KINDS[kind], "message" to message.trim(), "attempts" to attempts)
    if (!explanation.isNullOrEmpty()) error["explanation"] = explanation
    if (attempted != null) error["attempted"] = attempted
    return error
}

/** The tool_result block for the API: an error carries is_error true and its category, retry flag and message as text. */
fun toToolResult(toolUseId: String, result: Map<String, Any?>): Map<String, Any?> {
    if (result["is_error"] == true) {
        var text = "${result["category"]} error (retryable: ${if (result["retryable"] == true) "yes" else "no"}): ${result["message"]}"
        if (result["explanation"] != null) text += " Tell the customer: ${result["explanation"]}"
        return linkedMapOf("type" to "tool_result", "tool_use_id" to toolUseId, "content" to text, "is_error" to false)
    }
    return linkedMapOf("type" to "tool_result", "tool_use_id" to toolUseId, "content" to (result["content"]?.toString() ?: ""), "is_error" to false)
}

private fun isEmpty(value: Any?): Boolean = value == null || value == "" || (value is List<*> && value.isEmpty()) || (value is Map<*, *> && value.isEmpty())

/** Call a tool, recover locally from what is safe to recover from, and return a result or a structured error. */
fun runTool(tool: (Map<String, Any?>) -> Any?, args: Map<String, Any?>, policy: Map<String, Any?>, sleep: (Int) -> Unit): Map<String, Any?> {
    val key = policy["idempotency_key"] as String?
    val maxRetries = policy["max_retries"] as Int? ?: 2
    val base = policy["base_delay_ms"] as Int? ?: 100
    val safeToRepeat = policy["read_only"] == true || !key.isNullOrEmpty()
    var attempts = 0
    while (true) {
        attempts++
        val callArgs = LinkedHashMap(args)
        if (!key.isNullOrEmpty()) callArgs["idempotency_key"] = key
        try {
            val value = tool(callArgs)
            return linkedMapOf("ok" to true, "content" to value, "empty" to isEmpty(value), "attempts" to attempts)
        } catch (error: ToolError) {
            var kind = error.kind
            if (kind == "timeout") {
                if (!safeToRepeat) return makeError("outcome_unknown", "${error.message} The call may have taken effect: check the current state before trying again.", null, attempts, LinkedHashMap(args))
                kind = "transient"
            }
            if (kind != "transient") return makeError(kind, error.message ?: "", error.explanation, attempts, LinkedHashMap(args))
            if (attempts > maxRetries) return makeError("transient", "${error.message} Gave up after $attempts attempts.", null, attempts, LinkedHashMap(args))
            sleep(error.retryAfterMs ?: (base * (1 shl (attempts - 1))))
        } catch (error: RuntimeException) { // a bug in the tool must not end the run
            return makeError("internal", "unexpected failure in the tool: ${error.message}", null, attempts, LinkedHashMap(args))
        }
    }
}

/** What the loop does next with a result. */
fun nextAction(result: Map<String, Any?>): String? {
    if (result["is_error"] != true) return if (result["empty"] == true) "accept_empty" else "continue"
    return ACTIONS[result["category"] as String]
}
