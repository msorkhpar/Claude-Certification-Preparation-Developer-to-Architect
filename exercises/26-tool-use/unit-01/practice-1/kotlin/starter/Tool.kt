/** A tool the model may call: its definition (name, description, input schema) and the handler that runs it. */
class Tool(val name: String, val description: String, val inputSchema: Map<String, Any?>, val handler: (Map<String, Any?>) -> Any?)

/** A tool refuses or fails; its message goes back to the model as an error result. */
class ToolError(message: String) : RuntimeException(message)

/** The request would be rejected with a 400. [field] names the offending part. */
class RequestError(val field: String, reason: String) : RuntimeException("$field: $reason")

typealias Ask = (Map<String, Any?>) -> Map<String, Any?>
