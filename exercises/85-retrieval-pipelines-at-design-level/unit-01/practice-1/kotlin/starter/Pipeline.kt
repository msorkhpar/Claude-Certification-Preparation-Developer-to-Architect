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

fun chunkSections(docId: String, text: String, maxWords: Int = 30): List<Chunk>? {
    // TODO: one chunk per section, its text starting with "<title> > <section>. ", split at sentence ends when a section is longer than maxWords.
    return null
}

fun search(chunks: List<Chunk>, query: String, k: Int = 3, allowedDocs: Set<String>? = null): List<String>? {
    // TODO: the ids of the k best chunks for the query among the documents the caller may read.
    return null
}

fun chooseRetrieval(corpusTokens: Int, shape: String, pattern: String): String? {
    // TODO: the retrieval mechanism for a corpus of this size, this data shape ("text" or "table") and this query pattern.
    return null
}

fun reindex(chunks: List<Chunk>, docs: Map<String, String>): Reindexed? {
    // TODO: bring the chunks in line with the documents and report what was kept, replaced, added and removed.
    return null
}

fun stale(chunks: List<Chunk>, docs: Map<String, String>): List<String>? {
    // TODO: the ids of the chunks that no longer match their source.
    return null
}

fun recallAtK(results: Map<String, List<String>>, relevant: Map<String, String>, k: Int): Double? {
    // TODO: the share of all labelled questions whose relevant chunk is among the first k results.
    return null
}
