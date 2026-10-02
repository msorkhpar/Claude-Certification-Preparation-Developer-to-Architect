typealias Msg = Map<String, Any?>

fun runAgent(model: (List<Msg>) -> Msg, tools: Map<String, (Map<String, Any?>) -> Any?>, userText: String, maxTurns: Int = 5): String? {
    return null // replace with the loop
}
