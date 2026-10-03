/** The agent loop, driven by the stop reason. See ../../statement.md. Messages and replies are JSON-like maps. */

typealias Model = (List<Map<String, Any?>>) -> Map<String, Any?>
typealias Tools = Map<String, (Map<String, Any?>) -> String>

fun runAgent(model: Model, tools: Tools, task: String, maxTurns: Int = 8): Map<String, Any?>? {
    // TODO: send the task, run the tool calls the model asks for, and decide what to do next from the stop reason only.
    return null
}
