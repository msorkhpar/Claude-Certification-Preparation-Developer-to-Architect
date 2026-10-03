/** Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md. Results are JSON-like maps. */

/** What a tool throws. kind is transient, validation, permission, business or timeout (no answer was received, so the effect is unknown). */
class ToolError(val kind: String, message: String, val retryAfterMs: Int? = null, val explanation: String? = null) : RuntimeException(message)

fun makeError(kind: String, message: String, explanation: String? = null, attempts: Int = 1, attempted: Map<String, Any?>? = null): Map<String, Any?>? {
    // TODO: the structured result of a failed call.
    return null
}

fun toToolResult(toolUseId: String, result: Map<String, Any?>): Map<String, Any?>? {
    // TODO: the tool_result block for the API.
    return null
}

fun runTool(tool: (Map<String, Any?>) -> Any?, args: Map<String, Any?>, policy: Map<String, Any?>, sleep: (Int) -> Unit): Map<String, Any?>? {
    // TODO: call the tool, retry only what is safe to retry, return a result or a structured error.
    return null
}

fun nextAction(result: Map<String, Any?>): String? {
    // TODO: what the loop does next with a result.
    return null
}
