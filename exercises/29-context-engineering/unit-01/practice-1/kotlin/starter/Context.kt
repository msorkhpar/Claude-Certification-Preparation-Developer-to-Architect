private val log = System.getLogger("context")

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
    log.log(System.Logger.Level.DEBUG, "splitTurns input: {0}", messages)
    // TODO 1 of 7 (unlocks m1, e2, e3): group the messages into turns.
    // Receives the list of messages (startsTurn(message) says whether one begins a turn; the first message always does).
    // Returns a list of turns, each a list of messages, in order.
    // Example: [user "q", assistant tool_use, user tool_result, assistant "a", user "q2"] -> [[q, tool_use, tool_result, a], [q2]]
    return listOf(messages)
}

// TODO 2 of 7 (unlocks e1): replace the content of every result but the newest [keep] with the placeholder.
// Receives the tool_result blocks in conversation order, keep and the placeholder; changes the blocks in place, returns nothing.
// Example: three results with keep = 2 -> the first one's content becomes "[cleared]".
private fun clearOldest(results: List<MutableMap<String, Any?>>, keep: Int, placeholder: String) {
}

/** Copy of the conversation in which every tool result but the newest [keep] has its content replaced by the placeholder. */
@Suppress("UNCHECKED_CAST")
fun clearToolResults(messages: List<Map<String, Any?>>, keep: Int = 2, exclude: List<String> = emptyList(), placeholder: String = "[cleared]"): List<Map<String, Any?>> {
    val out: List<Map<String, Any?>> = copy(messages)
    val names = mutableMapOf<String, String>()
    val blocks = out.filter { it["content"] is List<*> }.flatMap { it["content"] as List<MutableMap<String, Any?>> }
    for (b in blocks) if (b["type"] == "tool_use") names[b["id"] as String] = b["name"] as String
    val results = blocks.filter { it["type"] == "tool_result" && names[it["tool_use_id"] as String] !in exclude }
    clearOldest(results, keep, placeholder)
    return out
}

// TODO 3 of 7 (unlocks e2): the turns of [rest] that remain.
// Receives the pinned turns, the other turns oldest first, and the budget. Drops the oldest turn of [rest] while countTokens of
// pinned + rest is over the budget, but never the last one. Returns the remaining turns.
// Example: three turns that are over budget by one turn's size -> the last two.
private fun trim(pinned: List<List<Map<String, Any?>>>, rest: List<List<Map<String, Any?>>>, budget: Int): List<List<Map<String, Any?>>> {
    return rest
}

/** Drop the oldest whole turns until the conversation fits; the newest turn always stays. With [pin], the first turn stays too. */
fun window(messages: List<Map<String, Any?>>, budget: Int, pin: Boolean = false): List<Map<String, Any?>> {
    val turns = splitTurns(messages)
    val pinned = if (pin) turns.take(1) else emptyList()
    val rest = trim(pinned, if (pin) turns.drop(1) else turns, budget)
    return (pinned + rest).flatten()
}

// TODO 4 of 7 (unlocks e3): whether compaction has nothing to do.
// Receives the messages, their turns, the budget and keepTurns. True when the conversation fits the budget or there are no more
// turns than keepTurns. Example: a conversation of 40 tokens with budget 50 -> true.
private fun leaveAlone(messages: List<Map<String, Any?>>, turns: List<List<Map<String, Any?>>>, budget: Int, keepTurns: Int): Boolean {
    return false
}

// TODO 5 of 7 (unlocks m1, e4): the first kept message with the summary block placed before its own blocks.
// Receives the kept messages and the summary text. Returns one message with the role of kept[0] and as content the text block
// "$SUMMARY_OPEN$summary$SUMMARY_CLOSE" followed by blocksOf(kept[0]).
// Example: kept[0] = user "q2", summary "S" -> {role: user, content: [<summary block>, {type: text, text: q2}]}
private fun withSummary(kept: List<Map<String, Any?>>, summary: String): Map<String, Any?> {
    return mapOf("role" to kept[0]["role"], "content" to emptyList<Any?>())
}

/** When the conversation is over budget, replace everything before the newest [keepTurns] turns by one summary. */
fun compact(messages: List<Map<String, Any?>>, budget: Int, summarise: (List<Map<String, Any?>>) -> String, keepTurns: Int = 1): List<Map<String, Any?>> {
    val turns = splitTurns(messages)
    if (leaveAlone(messages, turns, budget, keepTurns)) return messages.toList()
    val older = turns.dropLast(keepTurns).flatten()
    val kept = turns.takeLast(keepTurns).flatten()
    val summary = summarise(older)
    return listOf(withSummary(kept, summary)) + kept.drop(1)
}

// TODO 6 of 7 (unlocks e5): the problem of a citation whose document exists, or null when it can be trusted.
// Receives the document text and the citation (start_char_index and end_char_index are numbers). Returns "bad_range" (start below
// 0, end not after start, or end past the text), else "text_mismatch" (text.substring(start, end) is not the cited text, end
// excluded), else null. Example: text "abcdef", span 1 to 3, cited_text "bc" -> null; cited_text "cd" -> "text_mismatch"
private fun spanProblem(text: String, cite: Map<String, Any?>): String? {
    return null
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
                    problem = spanProblem(documents[doc]["text"] as String, cite)
                }
            }
            if (problem != null) problems += mapOf("block" to i, "citation" to j, "problem" to problem)
        }
    }
    return problems
}

// TODO 7 of 7 (unlocks e6): give a new key the next number; true when the key was new.
// Receives the map of numbers so far and a key. A key not in it gets numbers.size + 1 and the answer is true; a known key keeps
// its number and the answer is false. Example: empty map and "a" -> true, map is {a=1}; then "a" again -> false
private fun numberFor(numbers: MutableMap<String, Int>, key: String): Boolean {
    return false
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
            if (numberFor(numbers, key)) {
                sources += "[${numbers[key]}] ${documents[(cite["document_index"] as Number).toInt()]["title"]}: \"${cite["cited_text"]}\""
            }
            out.append("[${numbers[key]}]")
        }
    }
    return out.toString() + if (sources.isEmpty()) "" else "\n\nSources:\n" + sources.joinToString("\n")
}
