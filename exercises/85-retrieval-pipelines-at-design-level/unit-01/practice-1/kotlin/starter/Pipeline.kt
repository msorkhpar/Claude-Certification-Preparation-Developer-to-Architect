private val log = System.getLogger("pipeline")

/** A retrieval pipeline: chunks that carry their context, a search that respects access, rank fusion, a re-index that removes what changed, and recall over every question. See ../../statement.md. */

/** A chunk of a document: its id, the document it came from, that document's version and its text. */
data class Chunk(val id: String, val doc: String, val version: Int, val text: String)

/** The chunks after a re-index and what happened to each document: the keys added, replaced, removed and kept. */
data class Reindexed(val chunks: List<Chunk>, val report: Map<String, List<String>>)

val STOP = setOf("a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at")
private val TOKEN = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

/** A cheap fingerprint of a document's text: when it changes, the document changed. */
fun docVersion(text: String): Int = (text.sumOf { it.code.toLong() } % 1000003).toInt()

fun tokens(text: String): List<String> = TOKEN.findAll(text.lowercase()).map { it.value }.filter { it !in STOP }.toList()

private fun chunkText(title: String, name: String, part: String): String {
    // TODO 1 of 8 (unlocks m1): the text of one chunk.
    // Receives the document title, the section name and one part of the section's text. Returns the part with its context in front, as
    // "<title> > <name>. <part>", so a chunk still says where it came from.
    // Example: chunkText("Annual plan", "Cancellation", "You can cancel.") -> "Annual plan > Cancellation. You can cancel."
    return part
}

private fun splitSection(body: String, maxWords: Int): List<String> {
    // TODO 2 of 8 (unlocks e1): the parts of a long section.
    // Receives the text of one section and the word limit. Splits the text after each sentence end (a full stop followed by a space) and fills parts
    // with whole sentences: a part is closed before the sentence that would push it over `maxWords` words, and a single sentence longer than the limit
    // stays whole. Returns the parts as a list of strings (a short section is one part).
    // Example: "Install it. Configure it. Restart it." with maxWords 4 -> ["Install it. Configure it.", "Restart it."]
    return listOf(body)
}

fun chunkSections(docId: String, text: String, maxWords: Int = 30): List<Chunk>? {
    log.log(System.Logger.Level.DEBUG, "chunkSections input: {0}", text)
    val parts = text.split("\n## ")
    val title = parts[0].removePrefix("# ")
    val version = docVersion(text)
    val chunks = mutableListOf<Chunk>()
    for (section in parts.drop(1)) {
        val cut = section.indexOf('\n')
        val name = section.substring(0, cut)
        val pieces = splitSection(section.substring(cut + 1), maxWords)
        pieces.forEachIndexed { n, piece -> chunks += Chunk("$docId/$name" + (if (pieces.size == 1) "" else "#${n + 1}"), docId, version, chunkText(title, name, piece)) }
    }
    return chunks
}

private fun score(wanted: Set<String>, have: Set<String>): Int {
    // TODO 3 of 8 (unlocks e2): how well a chunk matches a query.
    // Receives the set of query words and the set of words of one chunk. Returns the sum, over the query words that the chunk has, of 3 for a code (a word that
    // contains a digit, such as E-7310) and 1 for any other word; 0 when the chunk has none of them.
    // Example: wanted {"cancel", "e-7310"}, have {"cancel", "e-7310", "card"} -> 4
    return 0
}

private fun visible(chunk: Chunk, allowedDocs: Set<String>?): Boolean {
    // TODO 4 of 8 (unlocks e3): may this reader see this chunk?
    // Receives a chunk and the set of document ids the reader may read, or null when the reader may read all of them. Returns true when the chunk's `doc` is allowed.
    // Example: visible(chunk of "annual", setOf("monthly")) -> false, visible(chunk of "annual", null) -> true
    return true
}

fun search(chunks: List<Chunk>, query: String, k: Int = 3, allowedDocs: Set<String>? = null): List<String>? {
    log.log(System.Logger.Level.DEBUG, "search input: {0}", query)
    val wanted = tokens(query).toSet()
    val scored = chunks.mapIndexedNotNull { n, chunk ->
        if (!visible(chunk, allowedDocs)) return@mapIndexedNotNull null
        val points = score(wanted, tokens(chunk.text).toSet())
        if (points > 0) Triple(-points, n, chunk.id) else null
    }
    return scored.sortedWith(compareBy({ it.first }, { it.second })).take(k).map { it.third }
}

fun chooseRetrieval(corpusTokens: Int, shape: String, pattern: String): String? {
    // TODO 5 of 8 (unlocks e4): the retrieval mechanism.
    // Receives the corpus size in tokens, the data shape ("text" or "table") and the query pattern. Decide in this order: under 200000 tokens "cached prompt";
    // a table "structured query"; a "multi-hop" pattern "agentic search"; then "identifier" gives "keyword index", "paraphrase" gives "embedding index" and any other pattern "hybrid index".
    // Example: chooseRetrieval(2000000, "text", "identifier") -> "keyword index"
    return null
}

private fun status(oldChunks: List<Chunk>, text: String): String {
    // TODO 6 of 8 (unlocks e5): what a re-index does with one document.
    // Receives the chunks the index already holds for the document (an empty list when it has none) and the document's current text. Returns "added" when
    // there are no chunks, "kept" when the version of the first chunk equals `docVersion(text)`, and "replaced" when it differs.
    // Example: no chunks -> "added"; chunks made from the same text -> "kept"
    return "added"
}

fun reindex(chunks: List<Chunk>, docs: Map<String, String>): Reindexed? {
    val old = chunks.groupBy { it.doc }
    val report = linkedMapOf("added" to mutableListOf<String>(), "replaced" to mutableListOf(), "removed" to old.keys.filter { it !in docs }.toMutableList(), "kept" to mutableListOf())
    val result = mutableListOf<Chunk>()
    for ((docId, text) in docs) {
        val state = status(old[docId] ?: emptyList(), text)
        report.getValue(state) += docId
        result += if (state == "kept") old.getValue(docId) else chunkSections(docId, text)!!
    }
    return Reindexed(result, report)
}

fun stale(chunks: List<Chunk>, docs: Map<String, String>): List<String>? {
    // TODO 7 of 8 (unlocks e6): the chunks that no longer match their source.
    // Receives the chunks and a map of the current documents (id to text). Returns the ids of the chunks, in order, whose document is gone or whose
    // version differs from `docVersion` of the current text.
    // Example: a chunk of a document that is no longer in `docs` is stale
    return null
}

fun recallAtK(results: Map<String, List<String>>, relevant: Map<String, String>, k: Int): Double? {
    // TODO 8 of 8 (unlocks e7): the share of questions answered in the first k results.
    // Receives `results` (question to the list of chunk ids returned) and `relevant` (every labelled question to the chunk id that answers it). Returns the
    // number of labelled questions whose relevant id is among the first k results, divided by the number of labelled questions, rounded to two decimals;
    // a question with no results counts as a miss, and 0.0 when there are no labelled questions.
    // Example: 2 of 3 questions hit -> 0.67
    return null
}
