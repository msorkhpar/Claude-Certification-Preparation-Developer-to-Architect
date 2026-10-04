/**
 * Design decisions around a retrieval pipeline: where a document is cut, what a chunk carries, which index answers which query, and what a re-index must remove.
 *
 * The corpus is four short documents. The "semantic" rankings are scripted: they stand in for an embedding index, which this course does not build (the Claude documentation says Anthropic
 * offers no embedding model and points to a provider). What the code shows is the design around that index. Read on 2026-10-04 against the Anthropic post on contextual retrieval and the Claude
 * documentation page "Embeddings". Nothing here calls a model.
 */

/** A chunk: its id and its text. */
data class Chunk(val id: String, val text: String)

val STOP = setOf("a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at")
val DOCS = linkedMapOf(
    "monthly" to "# Monthly plan\n## Cancellation\nYou can cancel at any time and the current month is not refunded.",
    "annual" to "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.",
    "refunds" to "# Refund policy\n## Eligibility\nCustomers may return items within 30 days of delivery.\n## Exceptions\nItems marked final sale cannot be returned, except when they arrive damaged.\n## Process\nRefunds go back to the original payment method within 5 business days.",
    "errors" to "# Error codes\n## E-7310\nThe warehouse could not reserve stock. Retry after the next stock sync.\n## E-4021\nThe payment gateway rejected the card. Ask for another card.",
)

private val TOKEN = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

fun tokens(text: String): List<String> = TOKEN.findAll(text.lowercase()).map { it.value }.filter { it !in STOP }.toList()

/** Cut every `size` words, whatever the words mean. */
fun chunkFixed(docId: String, text: String, size: Int): List<Chunk> =
    text.trim().split(Regex("\\s+")).chunked(size).mapIndexed { i, words -> Chunk("$docId#$i", words.joinToString(" ")) }

/** Cut at the headings; with context, each chunk starts with the document title and its section name, so it can be found and read alone. */
fun chunkSections(docId: String, text: String, context: Boolean): List<Chunk> {
    val parts = text.split("\n## ")
    val title = parts[0].removePrefix("# ")
    return parts.drop(1).map { section ->
        val cut = section.indexOf('\n')
        val name = section.substring(0, cut)
        Chunk("$docId/$name", (if (context) "$title > $name. " else "") + section.substring(cut + 1))
    }
}

fun index(docs: Map<String, String>, context: Boolean): List<Chunk> = docs.flatMap { (docId, text) -> chunkSections(docId, text, context) }

/** Words that appear in the chunk score one, a token with a digit (a code or an id) scores three; ties keep the index order. */
fun lexical(chunks: List<Chunk>, query: String, k: Int = 3): List<String> {
    val wanted = tokens(query).toSet()
    val scored = chunks.mapIndexedNotNull { n, chunk ->
        val have = tokens(chunk.text).toSet()
        val score = wanted.filter { it in have }.map { t -> if (t.any { c -> c.isDigit() }) 3 else 1 }.sum()
        if (score > 0) Triple(-score, n, chunk.id) else null
    }
    return scored.sortedWith(compareBy({ it.first }, { it.second })).take(k).map { it.third }
}

/** Reciprocal rank fusion, in integers so that every language ranks the same: each list adds 1000000 // (k + rank) to a chunk. */
fun fuse(rankings: List<List<String>>, k: Int = 60): List<String> {
    val score = HashMap<String, Int>()
    for (ranking in rankings) ranking.forEachIndexed { i, id -> score[id] = (score[id] ?: 0) + 1000000 / (k + i + 1) }
    return score.keys.sortedWith(compareBy({ -score.getValue(it) }, { it }))
}

/** True when one retrieved chunk holds the whole answer sentence. */
fun holds(chunks: List<Chunk>, chunkIds: List<String>, answer: String): Boolean {
    val texts = chunks.associate { it.id to it.text }
    return chunkIds.any { answer in texts.getValue(it) }
}

/** The shortcut: add the new chunks and leave the old ones where they are. */
fun reindexAdditive(chunks: List<Chunk>, docId: String, text: String): List<Chunk> = chunks + chunkSections(docId, text, true)

/** Drop every chunk of the document first, then add the new ones. */
fun reindexReplace(chunks: List<Chunk>, docId: String, text: String): List<Chunk> = chunks.filter { !it.id.startsWith("$docId/") } + chunkSections(docId, text, true)

/** Chunks that no longer match what their source says now. */
fun stale(chunks: List<Chunk>, docs: Map<String, String>): List<String> {
    val current = index(docs, true).toSet()
    return chunks.filter { it !in current }.map { it.id }
}

/** The cheapest mechanism that fits: a corpus under 200,000 tokens fits a cached prompt; a table is queried; several hops need an agent that searches; otherwise the query pattern picks the index. */
fun chooseRetrieval(corpusTokens: Int, shape: String, pattern: String): String = when {
    corpusTokens < 200000 -> "cached prompt"
    shape == "table" -> "structured query"
    pattern == "multi-hop" -> "agentic search"
    pattern == "identifier" -> "keyword index"
    pattern == "paraphrase" -> "embedding index"
    else -> "hybrid index"
}

/** Where a question went wrong, judged by retrieval and by generation separately; a right answer without its evidence is a risk of its own. */
fun layer(evidenceRetrieved: Boolean, answerCorrect: Boolean): String =
    if (answerCorrect) (if (evidenceRetrieved) "ok" else "unsupported") else (if (evidenceRetrieved) "generation" else "retrieval")

private fun yes(flag: Boolean) = if (flag) "yes" else "no"

private fun names(items: List<String>) = if (items.isEmpty()) "none" else items.joinToString(", ")

private fun ids(chunks: List<Chunk>) = chunks.map { it.id }

fun main() {
    val answer = "Items marked final sale cannot be returned, except when they arrive damaged."
    val fixed = chunkFixed("refunds", DOCS.getValue("refunds"), 12)
    val sections = chunkSections("refunds", DOCS.getValue("refunds"), false)
    println("cut every 12 words: ${fixed.size} chunks, the whole rule in one chunk: ${yes(holds(fixed, ids(fixed), answer))}")
    println("cut at headings:    ${sections.size} chunks, the whole rule in one chunk: ${yes(holds(sections, ids(sections), answer))}")
    for (context in listOf(false, true)) {
        println("query 'cancel the annual plan', chunks ${if (context) "with" else "without"} context: top chunk ${lexical(index(DOCS, context), "cancel the annual plan", 1)[0]}")
    }
    val chunks = index(DOCS, true)
    val semantic = linkedMapOf(
        "what does E-7310 mean" to listOf("errors/E-4021", "errors/E-7310", "refunds/Process"),
        "when will I be reimbursed" to listOf("refunds/Process", "annual/Cancellation", "monthly/Cancellation"),
    )
    val answers = mapOf("what does E-7310 mean" to "The warehouse could not reserve stock.", "when will I be reimbursed" to "Refunds go back to the original payment method within 5 business days.")
    println("%-28s%-9s%-10s%s".format("query", "lexical", "semantic", "hybrid"))
    for ((query, ranking) in semantic) {
        val lex = lexical(chunks, query)
        val hybrid = fuse(listOf(lex, ranking))
        println("%-28s%-9s%-10s%s".format(query, yes(holds(chunks, lex.take(1), answers.getValue(query))), yes(holds(chunks, ranking.take(1), answers.getValue(query))), yes(holds(chunks, hybrid.take(1), answers.getValue(query)))))
    }
    println("mechanism by corpus size, data shape and query pattern:")
    for ((size, shape, pattern) in listOf(Triple(50000, "text", "identifier"), Triple(5000000, "table", "paraphrase"), Triple(5000000, "text", "multi-hop"), Triple(5000000, "text", "identifier"), Triple(5000000, "text", "paraphrase"), Triple(5000000, "text", "mixed"))) {
        println("  %8d tokens  %-6s%-11s-> %s".format(size, shape, pattern, chooseRetrieval(size, shape, pattern)))
    }
    val edited = DOCS.getValue("refunds").replace("within 30 days", "within 60 days")
    val live = DOCS + ("refunds" to edited)
    val additive = reindexAdditive(chunks, "refunds", edited)
    val replaced = reindexReplace(chunks, "refunds", edited)
    println("after the window changes from 30 to 60 days, add the new chunks only: ${additive.size} chunks, stale ${names(stale(additive, live))}")
    println("after the window changes from 30 to 60 days, replace the document's chunks: ${replaced.size} chunks, stale ${names(stale(replaced, live))}")
    val outcomes = listOf(true to true, true to true, true to true, true to true, true to false, false to false, false to false, false to true)
    val counts = outcomes.groupingBy { layer(it.first, it.second) }.eachCount().toSortedMap()
    println("8 questions: evidence retrieved for ${outcomes.count { it.first }}, answers correct ${outcomes.count { it.second }}, by layer ${counts.entries.joinToString(", ") { "${it.key} ${it.value}" }}")
}
