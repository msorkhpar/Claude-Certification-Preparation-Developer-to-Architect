import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sqrt

private val log = System.getLogger("retrieval")

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

/** Windows of [size] words that start `size - overlap` words apart; the last window ends at the last word. */
fun chunk(text: String, size: Int, overlap: Int): List<String> {
    require(size >= 1 && overlap >= 0 && overlap < size) { "size must be at least 1 and overlap must be in 0 .. size - 1" }
    return windows(text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }, size, overlap)
}

private fun windows(words: List<String>, size: Int, overlap: Int): List<String> {
    // TODO 1 of 7 (finish this to pass e1): the windows over a list of words.
    // Receives the words, size and overlap (already checked). Returns the windows as strings of the words joined by one space: each holds
    // size words, the next starts size - overlap words later, and the loop stops at the first window that reaches the last word (so the
    // last window ends at the last word and none is repeated). No words gives an empty list.
    // Example: windows(listOf("a", "b", "c", "d", "e"), 3, 1) -> ["a b c", "c d e"]
    return emptyList()
}

/** A Chunk with id "doc#n" for every window of every document, in order. */
fun buildChunks(corpus: List<Doc>, size: Int, overlap: Int): List<Chunk> =
    corpus.flatMap { doc -> chunk(doc.text, size, overlap).mapIndexed { n, piece -> Chunk("${doc.id}#$n", doc.id, piece) } }

private fun texts(chunks: List<Chunk>, indexText: Map<String, String>?): Map<String, String> =
    chunks.associate { it.id to (indexText?.get(it.id) ?: it.text) }

private fun ordered(scores: Map<String, Double>): List<String> =
    scores.entries.sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key }).filter { it.value > 0 }.map { it.key }

private fun termScore(tf: Int, df: Int, n: Int, length: Int, average: Double, k1: Double, b: Double): Double {
    // TODO 2 of 7 (finish this to pass e2): the BM25 score one query term gives one chunk.
    // Receives the term's count in the chunk (tf, at least 1), the number of chunks that hold the term (df), the number of chunks (n), the
    // chunk's length in tokens, the average length, k1 and b. Returns idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * length / average)) with
    // idf = ln(1 + (n - df + 0.5) / (df + 0.5)). A rare term has a large idf; a short chunk gets a larger score than a long one.
    // Example: termScore(1, 1, 4, 5, 5.0, 1.5, 0.75) is about 1.2 times ln(1 + 3.5 / 1.5)
    return 0.0
}

/** Chunk ids by BM25 score (k1 = 1.5, b = 0.75), best first, ties by id, chunks with no word in common with the query left out. */
fun bm25Rank(chunks: List<Chunk>, query: String, indexText: Map<String, String>? = null): List<String> {
    val k1 = 1.5
    val b = 0.75
    val docs = texts(chunks, indexText).mapValues { tokenize(it.value) }
    val n = docs.size
    val average = if (n == 0) 0.0 else docs.values.sumOf { it.size.toDouble() } / n
    val df = mutableMapOf<String, Int>()
    for (tokens in docs.values) for (term in tokens.toSet()) df[term] = (df[term] ?: 0) + 1
    val terms = tokenize(query).distinct()
    val scores = linkedMapOf<String, Double>()
    for ((id, tokens) in docs) {
        var score = 0.0
        for (term in terms) {
            val tf = tokens.count { it == term }
            if (tf == 0) continue
            score += termScore(tf, df.getValue(term), n, tokens.size, average, k1, b)
        }
        scores[id] = score
    }
    return ordered(scores)
}

private fun dot(a: DoubleArray, b: DoubleArray): Double {
    // TODO 3 of 7 (finish this to pass m1): the dot product of two vectors of the same length.
    // Receives two arrays of numbers. Returns the sum of the products of the numbers in the same place (the vectors of embed() have length 1,
    // so this is their cosine). Example: dot(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)) -> 11.0
    return 0.0
}

/** Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out. */
fun embeddingRank(chunks: List<Chunk>, query: String, indexText: Map<String, String>? = null): List<String> {
    val q = embed(query)
    val scores = linkedMapOf<String, Double>()
    for ((id, text) in texts(chunks, indexText)) {
        scores[id] = dot(q, embed(text))
    }
    return ordered(scores)
}

/** Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat in
 *  one list ignored); best first, ties by id. */
fun fuse(rankings: List<List<String>>, k: Int = 60): List<String> {
    // TODO 4 of 7 (finish this to pass e3): reciprocal rank fusion of the lists in rankings.
    // Returns the ids best first: an id scores the sum of 1 / (k + rank) over the lists that hold it (rank starts at 1, a repeat in one list is
    // ignored); ordered(scores) sorts a map of scores best first, ties by id, and leaves out scores of 0 or less.
    // Example: fuse(listOf(listOf("a", "b"), listOf("b", "c"))) -> ["b", "a", "c"]
    return emptyList()
}

/** The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to [topN]. */
fun rerank(query: String, ids: List<String>, texts: Map<String, String>, scorer: Scorer, topN: Int? = null): List<String> {
    // TODO 5 of 7 (finish this to pass e4): the ids ordered by scorer(query, texts.getValue(id)) high to low.
    // Equal scores keep their input order (the sort is stable); cut to topN when it is not null. Only the given ids take part.
    // Example: rerank("q", listOf("a", "b"), mapOf("a" to "x", "b" to "xx"), { _, t -> t.length.toDouble() }) -> ["b", "a"]
    return ids
}

/** The share of the relevant documents that appear among the documents of the first k chunk ids. */
fun recallAtK(ids: List<String>, docOf: Map<String, String>, relevant: List<String>, k: Int): Double {
    require(relevant.isNotEmpty()) { "relevant must not be empty" }
    // TODO 6 of 7 (finish this to pass e5): the share of the relevant documents found among the documents of the first k chunk ids.
    // A document counts once however many of its chunks are there; relevant is not empty here. Returns a number from 0 to 1.
    // Example: recallAtK(listOf("a#0", "a#1", "b#0"), mapOf("a#0" to "a", "a#1" to "a", "b#0" to "b"), listOf("a", "c"), 3) -> 0.5
    return 0.0
}

private fun indexedText(chunks: List<Chunk>, contexts: Map<String, String>?): Map<String, String> {
    // TODO 7 of 7 (finish this to pass e6): the text each chunk is indexed under when it has a context sentence.
    // Receives the chunks and contexts (null or a map from chunk id to a sentence). Returns a map from chunk id to the sentence, a space
    // and the chunk's text, for the chunks that have a sentence only; the chunks themselves are not changed.
    // Example: indexedText(listOf(Chunk("a#0", "a", "x")), mapOf("a#0" to "About a.")) -> {a#0=About a. x}
    return emptyMap()
}

/** The ids of the best k chunks. [mode] is bm25, embedding or hybrid (fusion of both). [contexts] maps chunk ids to a sentence
 *  that is indexed in front of the chunk's text; [scorer] reranks the first [pool] ids of the ranking. */
fun retrieve(chunks: List<Chunk>, query: String, mode: String = "hybrid", k: Int = 3, pool: Int = 10, contexts: Map<String, String>? = null, scorer: Scorer? = null): List<String> {
    log.log(System.Logger.Level.DEBUG, "retrieve input: mode={0} k={1} query={2}", mode, k, query)
    val indexText = indexedText(chunks, contexts)
    val lexical = bm25Rank(chunks, query, indexText)
    val semantic = embeddingRank(chunks, query, indexText)
    val ranked = when (mode) { "bm25" -> lexical; "embedding" -> semantic; else -> fuse(listOf(lexical, semantic)) }
    if (scorer == null) return ranked.take(k)
    return rerank(query, ranked.take(pool), chunks.associate { it.id to it.text }, scorer, k)
}

/** The mean recall@k over the queries. */
fun evaluate(chunks: List<Chunk>, queries: List<Query>, mode: String = "hybrid", k: Int = 3, pool: Int = 10, contexts: Map<String, String>? = null, scorer: Scorer? = null): Double {
    val docOf = chunks.associate { it.id to it.doc }
    return queries.sumOf { recallAtK(retrieve(chunks, it.query, mode, k, pool, contexts, scorer), docOf, it.relevant, k) } / queries.size
}
