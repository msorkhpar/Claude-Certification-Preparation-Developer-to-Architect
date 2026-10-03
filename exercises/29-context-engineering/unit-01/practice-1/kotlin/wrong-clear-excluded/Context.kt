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

@Suppress("UNCHECKED_CAST")
private fun <T> copy(value: T): T = Json.parse(Json.stringify(value)) as T

@Suppress("UNCHECKED_CAST")
private fun blocksOf(message: Map<String, Any?>): List<Map<String, Any?>> {
    val content = message["content"]
    return if (content is String) listOf(mapOf("type" to "text", "text" to content)) else copy(content as List<Map<String, Any?>>)
}

/** A user message that is not made only of tool results begins a turn. */
@Suppress("UNCHECKED_CAST")
private fun startsTurn(message: Map<String, Any?>): Boolean {
    if (message["role"] != "user") return false
    val content = message["content"]
    return content is String || (content as List<Map<String, Any?>>).any { it["type"] != "tool_result" }
}

/** The messages as a list of turns: each turn is a user message that is not a tool result, and everything up to the next one. */
fun splitTurns(messages: List<Map<String, Any?>>): List<List<Map<String, Any?>>> {
    val turns = mutableListOf<MutableList<Map<String, Any?>>>()
    for (message in messages) {
        if (startsTurn(message) || turns.isEmpty()) turns += mutableListOf<Map<String, Any?>>()
        turns.last() += message
    }
    return turns
}

/** Copy of the conversation in which every tool result but the newest [keep] has its content replaced by the placeholder. */
@Suppress("UNCHECKED_CAST")
fun clearToolResults(messages: List<Map<String, Any?>>, keep: Int = 2, exclude: List<String> = emptyList(), placeholder: String = "[cleared]"): List<Map<String, Any?>> {
    val out: List<Map<String, Any?>> = copy(messages)
    val names = mutableMapOf<String, String>()
    val blocks = out.filter { it["content"] is List<*> }.flatMap { it["content"] as List<MutableMap<String, Any?>> }
    for (b in blocks) if (b["type"] == "tool_use") names[b["id"] as String] = b["name"] as String
    val results = blocks.filter { it["type"] == "tool_result" }
    for (b in results.take(maxOf(results.size - keep, 0))) b["content"] = placeholder
    return out
}

/** Drop the oldest whole turns until the conversation fits; the newest turn always stays. With [pin], the first turn stays too. */
fun window(messages: List<Map<String, Any?>>, budget: Int, pin: Boolean = false): List<Map<String, Any?>> {
    val turns = splitTurns(messages)
    val pinned = if (pin) turns.take(1) else emptyList()
    var rest = if (pin) turns.drop(1) else turns
    while (rest.size > 1 && countTokens((pinned + rest).flatten()) > budget) rest = rest.drop(1)
    return (pinned + rest).flatten()
}

/** When the conversation is over budget, replace everything before the newest [keepTurns] turns by one summary. */
fun compact(messages: List<Map<String, Any?>>, budget: Int, summarise: (List<Map<String, Any?>>) -> String, keepTurns: Int = 1): List<Map<String, Any?>> {
    if (countTokens(messages) <= budget) return messages.toList()
    val turns = splitTurns(messages)
    if (turns.size <= keepTurns) return messages.toList()
    val older = turns.dropLast(keepTurns).flatten()
    val kept = turns.takeLast(keepTurns).flatten()
    val summary = summarise(older)
    val first = mapOf("role" to kept[0]["role"], "content" to listOf(mapOf("type" to "text", "text" to "$SUMMARY_OPEN$summary$SUMMARY_CLOSE")) + blocksOf(kept[0]))
    return listOf(first) + kept.drop(1)
}

/** One {block, citation, problem} per citation that cannot be trusted, in order. */
@Suppress("UNCHECKED_CAST")
fun verifyCitations(blocks: List<Map<String, Any?>>, documents: List<Map<String, Any?>>): List<Map<String, Any?>> {
    val problems = mutableListOf<Map<String, Any?>>()
    blocks.forEachIndexed { i, block ->
        (block["citations"] as List<Map<String, Any?>>?)?.forEachIndexed { j, cite ->
            var problem: String? = null
            if (cite["type"] != "char_location") {
                problem = "unsupported_type"
            } else {
                val doc = (cite["document_index"] as Number).toInt()
                if (doc < 0 || doc >= documents.size) {
                    problem = "unknown_document"
                } else {
                    val text = documents[doc]["text"] as String
                    val start = (cite["start_char_index"] as Number).toInt()
                    val end = (cite["end_char_index"] as Number).toInt()
                    if (start < 0 || end <= start || end > text.length) problem = "bad_range"
                    else if (text.substring(start, end) != cite["cited_text"]) problem = "text_mismatch"
                }
            }
            if (problem != null) problems += mapOf("block" to i, "citation" to j, "problem" to problem)
        }
    }
    return problems
}

/** The answer text with a [n] after each cited block and a Sources list; one number per distinct cited span, in order. */
@Suppress("UNCHECKED_CAST")
fun footnotes(blocks: List<Map<String, Any?>>, documents: List<Map<String, Any?>>): String {
    val numbers = linkedMapOf<String, Int>()
    val sources = mutableListOf<String>()
    val out = StringBuilder()
    for (block in blocks) {
        out.append(block["text"])
        for (cite in (block["citations"] as List<Map<String, Any?>>?) ?: emptyList()) {
            val key = "${cite["document_index"]}:${cite["start_char_index"]}:${cite["end_char_index"]}"
            if (key !in numbers) {
                numbers[key] = numbers.size + 1
                sources += "[${numbers[key]}] ${documents[(cite["document_index"] as Number).toInt()]["title"]}: \"${cite["cited_text"]}\""
            }
            out.append("[${numbers[key]}]")
        }
    }
    return out.toString() + if (sources.isEmpty()) "" else "\n\nSources:\n" + sources.joinToString("\n")
}
