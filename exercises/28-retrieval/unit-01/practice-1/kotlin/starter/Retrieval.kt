import kotlin.math.sqrt

/** A retrieval pipeline: chunking, lexical and embedding search, fusion, reranking and recall. See ../../statement.md. */

private val WORD = Regex("[a-z0-9]+")

/** Lower-case words and numbers: the runs of [a-z0-9] in the text. */
fun tokenize(text: String): List<String> = WORD.findAll(text.lowercase()).map { it.value }.toList()

// ---- given: a deterministic toy embedding (not a real model) -----------------------------------------------------------
const val DIMS = 64

private fun fnv1a(text: String): Long {
    var h = 2166136261L
    for (b in text.toByteArray(Charsets.UTF_8)) h = ((h xor (b.toLong() and 0xff)) * 16777619L) and 0xFFFFFFFFL
    return h
}

/** 64 numbers: letter-trigram counts of the tokenised text hashed into buckets, scaled to length 1.
 *  It sees spelling, not meaning: "delete" and "deleting" are close, "delete" and "remove" are not. */
fun embed(text: String): DoubleArray {
    val padded = " " + tokenize(text).joinToString(" ") + " "
    val vector = DoubleArray(DIMS)
    for (i in 0 until padded.length - 2) vector[(fnv1a(padded.substring(i, i + 3)) % DIMS).toInt()] += 1.0
    var sum = 0.0
    for (x in vector) sum += x * x
    val length = sqrt(sum)
    if (length != 0.0) for (i in 0 until DIMS) vector[i] /= length
    return vector
}
// ------------------------------------------------------------------------------------------------------------------------

fun chunk(text: String, size: Int, overlap: Int): List<String>? {
    // TODO: windows of size words starting size - overlap words apart; IllegalArgumentException for a size or overlap that cannot work.
    return null
}

fun buildChunks(corpus: List<Doc>, size: Int, overlap: Int): List<Chunk>? {
    // TODO: a Chunk with id "doc#n" for every window of every document, in order.
    return null
}

fun bm25Rank(chunks: List<Chunk>, query: String, indexText: Map<String, String>? = null): List<String>? {
    // TODO: chunk ids by BM25 score, best first, ties by id; chunks with no word in common with the query are left out.
    return null
}

fun embeddingRank(chunks: List<Chunk>, query: String, indexText: Map<String, String>? = null): List<String>? {
    // TODO: chunk ids by the dot product of embed() vectors, best first, ties by id; zero similarity is left out.
    return null
}

fun fuse(rankings: List<List<String>>, k: Int = 60): List<String>? {
    // TODO: reciprocal rank fusion of the rankings; best first, ties by id.
    return null
}

fun rerank(query: String, ids: List<String>, texts: Map<String, String>, scorer: Scorer, topN: Int? = null): List<String>? {
    // TODO: the given ids reordered by scorer(query, text), high to low, equal scores keeping their order, cut to topN.
    return null
}

fun recallAtK(ids: List<String>, docOf: Map<String, String>, relevant: List<String>, k: Int): Double? {
    // TODO: the share of the relevant documents among the documents of the first k chunk ids; IllegalArgumentException when none is relevant.
    return null
}

fun retrieve(chunks: List<Chunk>, query: String, mode: String = "hybrid", k: Int = 3, pool: Int = 10, contexts: Map<String, String>? = null, scorer: Scorer? = null): List<String>? {
    // TODO: the ids of the best k chunks for mode bm25, embedding or hybrid; contexts are indexed in front of a chunk's text;
    // a scorer reranks the first pool ids of the ranking.
    return null
}

fun evaluate(chunks: List<Chunk>, queries: List<Query>, mode: String = "hybrid", k: Int = 3, pool: Int = 10, contexts: Map<String, String>? = null, scorer: Scorer? = null): Double? {
    // TODO: the mean recall@k over the queries.
    return null
}
