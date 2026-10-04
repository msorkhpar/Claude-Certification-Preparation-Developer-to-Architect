import java.util.Base64

/**
 * Tool interfaces graded on rules, offline: a set that confuses a model, the same job split into tools with one contract each, and a long result paged.
 *
 * No model is called. The rules are the course's own and small: a description of three sentences or more, a when-to-use phrase, a boundary against the
 * neighbouring tool, a description on every parameter, and a pair of descriptions that overlap too much. Checked on 2026-10-03 against the "Define tools"
 * page of the Claude API documentation and the "Writing tools for agents" article.
 */
const val OVERLAP = 0.6

/** A tool as the lint sees it: a name, a description and the description of each parameter. */
data class Tool(val name: String, val description: String, val params: Map<String, String>)

/** One page of a long result: the rows, the opaque cursor of the next page (null on the last) and a note for the model. */
data class Page(val rows: List<String>, val cursor: String?, val note: String?)

fun lint(tool: Tool): List<String> {
    val text = tool.description.lowercase()
    val found = mutableListOf<String>()
    if (listOf("do not use", "not for", "instead of").none { it in text }) found += "no-boundary"
    if ("use when" !in text) found += "no-use-when"
    if (tool.params.values.any { it.isBlank() }) found += "param-undescribed"
    if (Regex("""[.!?](?:\s|$)""").findAll(tool.description).count() < 3) found += "short-description"
    return found
}

private fun words(t: Tool): Set<String> = Regex("[a-z]{3,}").findAll(t.description.lowercase()).map { it.value }.toSet()

fun overlap(a: Tool, b: Tool): Double {
    val wa = words(a)
    val wb = words(b)
    return (wa intersect wb).size.toDouble() / (wa union wb).size
}

fun report(title: String, tools: List<Tool>) {
    println(title)
    for (tool in tools) println("  ${tool.name}: ${lint(tool).joinToString(", ").ifEmpty { "clean" }}")
    for ((i, a) in tools.withIndex()) {
        for (b in tools.drop(i + 1)) {
            val score = overlap(a, b)
            if (score >= OVERLAP) println("  overlap: ${a.name} and ${b.name} (${"%.2f".format(java.util.Locale.ROOT, score)})")
        }
    }
}

fun page(items: List<String>, cursor: String? = null, limit: Int = 4): Page {
    val offset = if (cursor == null) 0 else String(Base64.getDecoder().decode(cursor)).split(":")[1].toInt()
    val chunk = items.drop(offset).take(limit)
    val more = offset + chunk.size < items.size
    val token = if (more) Base64.getEncoder().encodeToString("offset:${offset + chunk.size}".toByteArray()) else null
    val note = if (more) "Showing ${chunk.size} of ${items.size} results; pass next_cursor to continue, or narrow the query with a filter." else null
    return Page(chunk, token, note)
}

val POOR = listOf(
    Tool("analyze_content", "Analyzes content and returns the result.", mapOf("content" to "The content.")),
    Tool("analyze_document", "Analyzes a document and returns the result.", mapOf("document" to "")),
)

val SPLIT = listOf(
    Tool(
        "extract_web_results",
        "Pulls the title, date and main claims from one web page. Use when a search result needs to be read. Do not use it for uploaded files; use extract_data_points instead of this tool for those.",
        mapOf("url" to "The page address."),
    ),
    Tool(
        "extract_data_points",
        "Lists every figure and date in one uploaded document, each with its page. Use when a report or table must be mined for numbers. Not for web pages; call extract_web_results for those.",
        mapOf("document_id" to "The id of an uploaded document."),
    ),
    Tool(
        "summarize_content",
        "Writes a short summary of text you already hold. Use when a long passage must fit in a brief. Do not use it to check a claim; verify_claim_against_source does that.",
        mapOf("text" to "The text to shorten.", "max_words" to "The longest summary, in words."),
    ),
    Tool(
        "verify_claim_against_source",
        "Says whether one claim is supported by one named source and quotes the passage. Use when a figure or statement needs a check. Not for finding new sources; use extract_web_results instead of this tool for that.",
        mapOf("claim" to "One sentence to test.", "source_id" to "The id of the source to test it against."),
    ),
)

fun main() {
    report("the set as first written", POOR)
    println()
    report("the same job, one contract per tool", SPLIT)
    val rows = (0 until 25).map { "row-%02d".format(it) }
    println("\na long result, paged four rows at a time")
    var cursor: String? = null
    for (number in 1..2) {
        val p = page(rows, cursor)
        cursor = p.cursor
        println("  page $number: ${p.rows.joinToString(" ")}")
        println("    note: ${p.note ?: "None"}")
    }
    println("  the cursor is opaque: ${cursor ?: "None"}")
}
