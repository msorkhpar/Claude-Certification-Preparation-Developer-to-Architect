import kotlin.math.ln
import kotlin.math.min
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

/** Windows of [size] words that start `size - overlap` words apart; the last window ends at the last word. */
fun chunk(text: String, size: Int, overlap: Int): List<String> {
    require(size >= 1 && overlap >= 0 && overlap < size) { "size must be at least 1 and overlap must be in 0 .. size - 1" }
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < words.size) {
        chunks += words.subList(start, min(start + size, words.size)).joinToString(" ")
        if (start + size >= words.size) break
        start += size - overlap
    }
    return chunks
}

/** A Chunk with id "doc#n" for every window of every document, in order. */
fun buildChunks(corpus: List<Doc>, size: Int, overlap: Int): List<Chunk> =
    corpus.flatMap { doc -> chunk(doc.text, size, overlap).mapIndexed { n, piece -> Chunk("${doc.id}#$n", doc.id, piece) } }

private fun texts(chunks: List<Chunk>, indexText: Map<String, String>?): Map<String, String> =
    chunks.associate { it.id to (indexText?.get(it.id) ?: it.text) }

private fun ordered(scores: Map<String, Double>): List<String> =
    scores.entries.sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key }).filter { it.value > 0 }.map { it.key }

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
            val idf = ln(1 + (n - df.getValue(term) + 0.5) / (df.getValue(term) + 0.5))
            score += idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * tokens.size / average))
        }
        scores[id] = score
    }
    return ordered(scores)
}

/** Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out. */
fun embeddingRank(chunks: List<Chunk>, query: String, indexText: Map<String, String>? = null): List<String> {
    val q = embed(query)
    val scores = linkedMapOf<String, Double>()
    for ((id, text) in texts(chunks, indexText)) {
        val v = embed(text)
        var total = 0.0
        for (i in q.indices) total += q[i] * v[i]
        scores[id] = total
    }
    return ordered(scores)
}

/** Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat in
 *  one list ignored); best first, ties by id. */
fun fuse(rankings: List<List<String>>, k: Int = 60): List<String> {
    val scores = linkedMapOf<String, Double>()
    for (ranking in rankings) ranking.distinct().forEachIndexed { i, id -> scores[id] = (scores[id] ?: 0.0) + 1.0 / (k + i + 1) }
    return ordered(scores)
}

/** The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to [topN]. */
fun rerank(query: String, ids: List<String>, texts: Map<String, String>, scorer: Scorer, topN: Int? = null): List<String> {
    val sorted = ids.sortedByDescending { scorer(query, texts.getValue(it)) } // sortedByDescending is stable
    return if (topN == null) sorted else sorted.take(topN)
}

/** The share of the relevant documents that appear among the documents of the first k chunk ids. */
fun recallAtK(ids: List<String>, docOf: Map<String, String>, relevant: List<String>, k: Int): Double {
    require(relevant.isNotEmpty()) { "relevant must not be empty" }
    val found = ids.take(k).map { docOf.getValue(it) }.toSet()
    val wanted = relevant.toSet()
    return wanted.count { it in found }.toDouble() / wanted.size
}

/** The ids of the best k chunks. [mode] is bm25, embedding or hybrid (fusion of both). [contexts] maps chunk ids to a sentence
 *  that is indexed in front of the chunk's text; [scorer] reranks the first [pool] ids of the ranking. */
fun retrieve(chunks: List<Chunk>, query: String, mode: String = "hybrid", k: Int = 3, pool: Int = 10, contexts: Map<String, String>? = null, scorer: Scorer? = null): List<String> {
    val indexText = chunks.filter { contexts != null && it.id in contexts }.associate { it.id to "${contexts!![it.id]} ${it.text}" }
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
