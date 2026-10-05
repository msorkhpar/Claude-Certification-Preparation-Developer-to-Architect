/** Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md. Results are JSON-like maps. */

private val log = System.getLogger("errors")

private val KINDS = mapOf("transient" to true, "validation" to false, "permission" to false, "business" to false, "outcome_unknown" to false, "internal" to false)
private val GENERIC = setOf("", "error", "failed", "failure", "operation failed", "something went wrong", "unknown error")
private val ACTIONS = mapOf("transient" to "retry_later", "validation" to "repair_input", "permission" to "escalate", "business" to "explain", "outcome_unknown" to "verify_first", "internal" to "escalate")

/** What a tool throws. kind is transient, validation, permission, business or timeout (no answer was received, so the effect is unknown). */
class ToolError(val kind: String, message: String, val retryAfterMs: Int? = null, val explanation: String? = null) : RuntimeException(message)

/** The structured result of a failed call. */
fun makeError(kind: String, message: String, explanation: String? = null, attempts: Int = 1, attempted: Map<String, Any?>? = null): Map<String, Any?> {
    // TODO 2 of 9 (finish this to pass e1): the refusals of make_error. Refuse with an error when the kind is not one of
    //   KINDS, and when the message, trimmed, lower-cased and without final periods, is in GENERIC. Example:
    //   make_error("transient", "Operation failed.") -> raises.
    // TODO 1 of 9 (finish this to pass m1): the structured error. Receives the kind, the message and the attempts. Build
    //   the map: is_error true, category the kind, retryable the flag KINDS holds for that kind, message trimmed,
    //   attempts. Example: make_error("transient", "Service busy, retry later") -> is_error true, retryable true.
    val error = linkedMapOf<String, Any?>("is_error" to false, "category" to kind, "retryable" to false, "message" to message, "attempts" to attempts)
    if (!explanation.isNullOrEmpty()) error["explanation"] = explanation
    if (attempted != null) error["attempted"] = attempted
    return error
}

/** The tool_result block for the API: an error carries is_error true and its category, retry flag and message as text. */
fun toToolResult(toolUseId: String, result: Map<String, Any?>): Map<String, Any?> {
    if (result["is_error"] == true) {
        var text = "${result["category"]} error (retryable: ${if (result["retryable"] == true) "yes" else "no"}): ${result["message"]}"
        if (result["explanation"] != null) text += " Tell the customer: ${result["explanation"]}"
        return linkedMapOf("type" to "tool_result", "tool_use_id" to toolUseId, "content" to text, "is_error" to true)
    }
    return linkedMapOf("type" to "tool_result", "tool_use_id" to toolUseId, "content" to (result["content"]?.toString() ?: ""), "is_error" to false)
}

// TODO 5 of 9 (finish this to pass e4): the empty check. Return true for none, an empty text, an empty list and an empty
//   map, and false for anything else (0 and false are real values). Example: [] -> true, 0 -> false.
private fun isEmpty(value: Any?): Boolean = false

/** Call a tool, recover locally from what is safe to recover from, and return a result or a structured error. */
fun runTool(tool: (Map<String, Any?>) -> Any?, args: Map<String, Any?>, policy: Map<String, Any?>, sleep: (Int) -> Unit): Map<String, Any?> {
    log.log(System.Logger.Level.DEBUG, "runTool input: {0}", args)
    val key = policy["idempotency_key"] as String?
    val maxRetries = policy["max_retries"] as Int? ?: 2
    val base = policy["base_delay_ms"] as Int? ?: 100
    val safeToRepeat = policy["read_only"] == true || !key.isNullOrEmpty()
    var attempts = 0
    while (true) {
        attempts++
        val callArgs = LinkedHashMap(args)
        // TODO 7 of 9 (finish this to pass e5): the idempotency key. When the policy holds a key, add it to `call_args`,
        //   the copy of the arguments that this attempt sends (never to the caller's own map). Example: key "k-1" -> every
        //   attempt receives idempotency_key "k-1".
        try {
            val value = tool(callArgs)
            return linkedMapOf("ok" to true, "content" to value, "empty" to isEmpty(value), "attempts" to attempts)
        } catch (error: ToolError) {
            var kind = error.kind
            // TODO 6 of 9 (finish this to pass e5): the timeout. When the kind is timeout: if the call is not safe to
            //   repeat, return make_error("outcome_unknown", the message plus "The call may have taken effect: check the
            //   current state before trying again.", attempts, a copy of the arguments); otherwise treat it as transient.
            //   Example: timeout on a write with no key -> outcome_unknown.
            if (kind == "timeout") {
                kind = "transient"
            }
            // TODO 3 of 9 (finish this to pass e2): the kinds that are not retried. When the kind is not transient,
            //   return the structured error at once (message and explanation of the tool error, the attempts so far, a
            //   copy of the arguments). Example: a validation error on the first attempt -> make_error(validation, ...,
            //   attempts=1) and no sleep.
            if (attempts > maxRetries) return makeError("transient", "${error.message} Gave up after $attempts attempts.", null, attempts, LinkedHashMap(args))
            // TODO 4 of 9 (finish this to pass e2, e3): the wait before the next attempt, in milliseconds. Receives the
            //   tool error's retry-after value (or none), the base delay and the attempt number from 1. Sleep the value
            //   the service asked for when there is one, otherwise base times 2 to the power of attempts - 1. Example: no
            //   retry-after, base 100 -> waits 100, 200, 400.
            sleep(base)
        } catch (error: RuntimeException) {
            // TODO 9 of 9 (finish this to pass e7): the unexpected exception. When the tool raises something that is not a
            //   ToolError, return make_error("internal", "unexpected failure in the tool: " + its message, attempts, a copy of
            //   the arguments) instead of letting it end the run. Example: a tool that raises ValueError("boom") -> category
            //   internal.
            return makeError("transient", "unexpected failure in the tool: ${error.message}", null, attempts, LinkedHashMap(args))
        }
    }
}

/** What the loop does next with a result. */
fun nextAction(result: Map<String, Any?>): String? {
    // TODO 8 of 9 (finish this to pass e6): the next action of the loop. Receives a result. For an error return the
    //   action ACTIONS holds for its category; for a success return accept_empty when it is empty, otherwise continue.
    //   Example: a permission error -> escalate.
    return "continue"
}
