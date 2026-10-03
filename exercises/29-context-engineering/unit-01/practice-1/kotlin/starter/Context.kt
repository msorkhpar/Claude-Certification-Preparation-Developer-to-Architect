/** Keeping a conversation inside its budget, and checking the citations in an answer. See ../../statement.md. Messages and blocks are JSON-like maps. */

const val SUMMARY_OPEN = "<summary>\n"
const val SUMMARY_CLOSE = "\n</summary>"

private fun tokens(text: String) = (text.length + 3) / 4

/** Given: a rough size of a conversation. 4 per message, plus 1 per 4 characters of text and of tool result text, plus 10 per tool call. */
@Suppress("UNCHECKED_CAST")
fun countTokens(messages: List<Map<String, Any?>>): Int {
    var total = 0
    for (message in messages) {
        total += 4
        val content = message["content"]
        if (content is String) {
            total += tokens(content)
            continue
        }
        for (block in content as List<Map<String, Any?>>) {
            when (block["type"]) {
                "text" -> total += tokens(block["text"] as String)
                "tool_result" -> total += tokens((block["content"] as String?) ?: "")
                "tool_use" -> total += 10
            }
        }
    }
    return total
}

/** The messages as a list of turns: a turn is a user message that is not only tool results, and everything up to the next one. */
fun splitTurns(messages: List<Map<String, Any?>>): List<List<Map<String, Any?>>>? {
    // TODO
    return null
}

fun clearToolResults(messages: List<Map<String, Any?>>, keep: Int = 2, exclude: List<String> = emptyList(), placeholder: String = "[cleared]"): List<Map<String, Any?>>? {
    // TODO: a copy in which every tool result but the newest keep has its content replaced by the placeholder.
    return null
}

fun window(messages: List<Map<String, Any?>>, budget: Int, pin: Boolean = false): List<Map<String, Any?>>? {
    // TODO: drop the oldest whole turns until the conversation fits; the newest turn always stays; with pin the first stays too.
    return null
}

fun compact(messages: List<Map<String, Any?>>, budget: Int, summarise: (List<Map<String, Any?>>) -> String, keepTurns: Int = 1): List<Map<String, Any?>>? {
    // TODO: over budget, replace everything before the newest keepTurns turns by one summary block.
    return null
}

fun verifyCitations(blocks: List<Map<String, Any?>>, documents: List<Map<String, Any?>>): List<Map<String, Any?>>? {
    // TODO: one {block, citation, problem} per citation that cannot be trusted, in order.
    return null
}

fun footnotes(blocks: List<Map<String, Any?>>, documents: List<Map<String, Any?>>): String? {
    // TODO: the answer text with a [n] after each cited block and a Sources list; one number per distinct cited span, in order.
    return null
}
