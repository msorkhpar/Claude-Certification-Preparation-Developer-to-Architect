/** A tool loop against a scripted model. See ../../statement.md. Messages and replies are JSON-like maps. */

fun runAgent(ask: Ask, tools: List<Tool>, userText: String, model: String = "claude-sonnet-5-5", maxTurns: Int = 8, toolChoice: Map<String, Any?>? = null): Map<String, Any?>? {
    // TODO: call the model, run every tool it asks for, send the results back, until it ends its turn or a limit is hit.
    return null
}
