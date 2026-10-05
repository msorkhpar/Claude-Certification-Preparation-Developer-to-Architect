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
    return "$title > $name. $part"
}

private fun splitSection(body: String, maxWords: Int): List<String> {
    val pieces = mutableListOf<String>()
    var current = mutableListOf<String>()
    for (sentence in Regex("(?<=\\.) ").split(body)) {
        if (current.isNotEmpty() && (current.joinToString(" ") + " " + sentence).split(Regex("\\s+")).size > maxWords) {
            pieces += current.joinToString(" ")
            current = mutableListOf()
        }
        current.add(sentence)
    }
    pieces += current.joinToString(" ")
    return pieces
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
    return wanted.filter { it in have }.map { t -> if (t.any { c -> c.isDigit() }) 3 else 1 }.sum()
}

private fun visible(chunk: Chunk, allowedDocs: Set<String>?): Boolean {
    return allowedDocs == null || chunk.doc in allowedDocs
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
    return when {
        corpusTokens < 200000 -> "cached prompt"
        shape == "table" -> "structured query"
        pattern == "multi-hop" -> "agentic search"
        pattern == "identifier" -> "keyword index"
        pattern == "paraphrase" -> "embedding index"
        else -> "hybrid index"
    }
}

private fun status(oldChunks: List<Chunk>, text: String): String {
    if (oldChunks.isEmpty()) return "added"
    return if (oldChunks[0].version == docVersion(text)) "kept" else "replaced"
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
    return chunks.filter { it.doc !in docs || it.version != docVersion(docs.getValue(it.doc)) }.map { it.id }
}

fun recallAtK(results: Map<String, List<String>>, relevant: Map<String, String>, k: Int): Double? {
    if (relevant.isEmpty()) return 0.0
    val hits = relevant.count { (query, id) -> id in (results[query] ?: emptyList()).take(k) }
    return Math.round(hits * 100.0 / relevant.size) / 100.0
}
