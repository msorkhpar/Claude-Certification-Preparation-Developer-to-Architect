/** Tool interfaces graded on rules: lint a tool and a tool set, page large results, and weigh a tool's annotations. See ../../statement.md. Tools are JSON-like maps. */

fun lintTool(tool: Map<String, Any?>): List<String>? {
    // TODO: the sorted list of rule ids this tool breaks.
    return null
}

fun lintToolSet(tools: List<Map<String, Any?>>, maxTools: Int = 20): List<List<String>>? {
    // TODO: sorted [tool name, rule] pairs for the whole set.
    return null
}

fun pageResults(items: List<String>, cursor: String? = null, limit: Int = 10, maxChars: Int = 2000): Map<String, Any?>? {
    // TODO: a map with items, next_cursor, truncated and note.
    return null
}

fun effectiveHints(tool: Map<String, Any?>, trustedServer: Boolean): Map<String, Boolean>? {
    // TODO: the four hints a client acts on.
    return null
}

fun parallelSafe(tools: List<Map<String, Any?>>, trustedServers: Set<String>): List<String>? {
    // TODO: names of the tools that may run beside other read-only tools.
    return null
}
