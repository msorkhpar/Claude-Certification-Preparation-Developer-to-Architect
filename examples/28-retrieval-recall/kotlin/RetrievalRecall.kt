import kotlin.math.ln
import kotlin.math.sqrt

private val log = System.getLogger("retrieval_recall")

/**
 * A retrieval pipeline measured on recall: BM25, a toy embedding, rank fusion, a reranker and contextual indexing.
 *
 * Everything is local and deterministic. The embedding is a TOY (hashed letter trigrams, no model), and the reranker is a
 * hand-written scoring function standing in for a reranking model. The knowledge base is invented. No API is called.
 */
data class Chunk(val id: String, val doc: String, val text: String)

data class Query(val text: String, val relevant: List<String>)

val CORPUS = listOf(
    "refunds" to
        "Refund policy: customers may request a refund within 30 days of purchase. Refunds are issued to the original payment method within five business days. Digital goods are not refundable after download.",
    "shipping" to
        "Shipping times: standard delivery takes three to five business days. Express delivery arrives the next day. Orders over fifty dollars ship free to domestic addresses.",
    "password" to
        "To reset a forgotten password, open the sign in page and choose forgot password. A reset link is emailed to you and expires after one hour.",
    "payment-errors" to
        "Error E-4012 means the payment gateway rejected the card. Check the billing address and try again, or use another card. Error E-4013 means the card has expired.",
    "warranty" to
        "All hardware carries a two year warranty that covers manufacturing defects. Accidental damage is covered only with the protection plan.",
    "deletion" to
        "You can delete your account from the privacy settings. Deletion removes personal data within thirty days and cannot be undone.",
    "api-limits" to
        "The API allows sixty requests per minute for each key. Exceeding the limit returns status 429 with a retry-after header.",
    "invoices" to
        "Invoices are generated on the first day of each month and sent as PDF attachments. Past invoices can be downloaded from the billing page.",
    "returns" to
        "To return a physical item, print the prepaid label from your orders page and drop the parcel at any carrier point. Returns must arrive within fourteen days and items must be unused.",
    "international" to
        "International orders ship with a tracked carrier and take seven to twelve business days. Customs duties are paid by the recipient and are not included in the order total.",
    "billing" to
        "Your plan renews automatically each month on the billing date. You can switch plans or cancel renewal from the billing page before the date.",
    "api-keys" to
        "Create API keys in the developer console. Rotate a key by creating a new one and deleting the old one. Keys are shown only once, so store them safely.",
    "warranty-claims" to
        "Warranty claims. Hardware owners can open a claim from the support portal. Send the serial number and a photo of the damage within thirty days. We reply within two business days.",
)

val QUERIES = listOf(
    Query("send back parcel", listOf("returns")),
    Query("free shipping threshold", listOf("shipping")),
    Query("tracked carrier", listOf("international")),
    Query("deleting accounts", listOf("deletion")),
    Query("resetting passwords", listOf("password")),
    Query("monthly invoicing", listOf("invoices")),
    Query("deliveries arrive", listOf("shipping")),
    Query("refund", listOf("refunds")),
)

const val CHUNK_WORDS = 14
const val OVERLAP = 4

/** Lower-case words and numbers: the runs of [a-z0-9] in the text. */
fun tokenize(text: String): List<String> = Regex("[a-z0-9]+").findAll(text.lowercase()).map { it.value }.toList()

// ---- given: a deterministic toy embedding (not a real model) -------------------------------------------------------
const val DIMS = 64

fun fnv1a(text: String): Long {
    var h = 2166136261L
    for (byte in text.toByteArray(Charsets.UTF_8)) h = ((h xor (byte.toLong() and 0xFF)) * 16777619L) and 0xFFFFFFFFL
    return h
}

/** 64 numbers: letter-trigram counts of the tokenised text hashed into buckets, scaled to length 1.
 *  It sees spelling, not meaning: 'delete' and 'deleting' are close, 'delete' and 'remove' are not. */
fun embed(text: String): List<Double> {
    val padded = " " + tokenize(text).joinToString(" ") + " "
    val vector = DoubleArray(DIMS)
    for (i in 0 until padded.length - 2) vector[(fnv1a(padded.substring(i, i + 3)) % DIMS).toInt()] += 1.0
    val length = sqrt(vector.sumOf { it * it })
    return if (length != 0.0) vector.map { it / length } else vector.toList()
}
// ---------------------------------------------------------------------------------------------------------------------

/** Windows of `size` words that start `size - overlap` words apart; the last window ends at the last word. */
fun chunk(text: String, size: Int, overlap: Int): List<String> {
    require(size >= 1 && overlap in 0 until size) { "size must be at least 1 and overlap must be in 0 .. size - 1" }
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < words.size) {
        chunks += words.subList(start, minOf(start + size, words.size)).joinToString(" ")
        if (start + size >= words.size) break
        start += size - overlap
    }
    return chunks
}

private fun texts(chunks: List<Chunk>, indexText: Map<String, String>?) = chunks.associate { it.id to (indexText?.get(it.id) ?: it.text) }

private fun ordered(scores: Map<String, Double>): List<String> =
    scores.entries.filter { it.value > 0 }.sortedWith(compareBy({ -it.value }, { it.key })).map { it.key }

/** Chunk ids by BM25 score, best first, ties by id, chunks that share no word with the query left out. */
fun bm25Rank(chunks: List<Chunk>, query: String, indexText: Map<String, String>? = null, k1: Double = 1.5, b: Double = 0.75): List<String> {
    val docs = texts(chunks, indexText).mapValues { tokenize(it.value) }
    val n = docs.size
    val average = if (n == 0) 0.0 else docs.values.sumOf { it.size } / n.toDouble()
    val df = mutableMapOf<String, Int>()
    for (tokens in docs.values) for (term in tokens.toSet()) df.merge(term, 1, Int::plus)
    val terms = tokenize(query).distinct()
    val scores = docs.mapValues { (_, tokens) ->
        var score = 0.0
        for (term in terms) {
            val tf = tokens.count { it == term }
            if (tf == 0) continue
            val idf = ln(1 + (n - df.getValue(term) + 0.5) / (df.getValue(term) + 0.5))
            score += idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * tokens.size / average))
        }
        score
    }
    return ordered(scores)
}

/** Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out. */
fun embeddingRank(chunks: List<Chunk>, query: String, indexText: Map<String, String>? = null): List<String> {
    val q = embed(query)
    return ordered(texts(chunks, indexText).mapValues { (_, text) -> q.zip(embed(text)).fold(0.0) { total, (a, b) -> total + a * b } })
}

/** Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat
 *  in one list ignored); best first, ties by id. */
fun fuse(rankings: List<List<String>>, k: Int = 60): List<String> {
    val scores = mutableMapOf<String, Double>()
    for (ranking in rankings) for ((i, id) in ranking.distinct().withIndex()) scores.merge(id, 1.0 / (k + i + 1), Double::plus)
    return ordered(scores)
}

/** The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to topN. */
fun rerank(query: String, ids: List<String>, texts: Map<String, String>, scorer: (String, String) -> Double, topN: Int? = null): List<String> {
    val sorted = ids.sortedBy { -scorer(query, texts.getValue(it)) } // sortedBy is stable
    return if (topN == null) sorted else sorted.take(topN)
}

fun docOf(chunks: List<Chunk>) = chunks.associate { it.id to it.doc }

/** 1-based position of the first chunk of a relevant document, or null. */
fun firstRank(ids: List<String>, chunks: List<Chunk>, relevant: List<String>): Int? {
    val docs = docOf(chunks)
    return ids.indexOfFirst { docs[it] in relevant }.takeIf { it >= 0 }?.plus(1)
}

fun recall(ids: List<String>, chunks: List<Chunk>, relevant: List<String>, k: Int): Double {
    val docs = docOf(chunks)
    return (ids.take(k).map { docs.getValue(it) }.toSet() intersect relevant.toSet()).size / relevant.toSet().size.toDouble()
}

fun rankers(chunks: List<Chunk>, query: String): Map<String, List<String>> {
    val lexical = bm25Rank(chunks, query)
    val semantic = embeddingRank(chunks, query)
    return linkedMapOf("bm25" to lexical, "embedding" to semantic, "hybrid" to fuse(listOf(lexical, semantic)))
}

val SYNONYMS = mapOf("send" to listOf("return", "returns"), "back" to listOf("return", "returns"))

/** Stands in for a reranking model: it reads the query with a tiny synonym list added and scores the share of those words that the chunk holds. */
fun toyReranker(query: String, text: String): Double {
    val words = tokenize(query).flatMap { listOf(it) + (SYNONYMS[it] ?: emptyList()) }.distinct()
    val have = tokenize(text).toSet()
    return words.count { it in have } / words.size.toDouble()
}

fun buildAll(): List<Chunk> = CORPUS.flatMap { (doc, text) -> chunk(text, CHUNK_WORDS, OVERLAP).mapIndexed { n, piece -> Chunk("$doc#$n", doc, piece) } }

private fun py(ids: List<String>) = ids.joinToString(", ", "[", "]") { "'$it'" }

private fun num(v: Double) = if (v == Math.rint(v)) "%.1f".format(v) else (Math.round(v * 1000) / 1000.0).toString()

fun main() {
    val chunks = buildAll()
    println("${CORPUS.size} documents, ${chunks.size} chunks of at most $CHUNK_WORDS words, overlap $OVERLAP")
    println("rank of the first relevant chunk".padEnd(34) + "bm25".padStart(6) + "embedding".padStart(11) + "hybrid".padStart(8))
    val totals = linkedMapOf("bm25" to 0.0, "embedding" to 0.0, "hybrid" to 0.0)
    for ((query, relevant) in QUERIES) {
        val ranked = rankers(chunks, query)
        val cells = ranked.mapValues { (_, ids) -> firstRank(ids, chunks, relevant)?.toString() ?: "-" }
        for ((mode, ids) in ranked) totals[mode] = totals.getValue(mode) + recall(ids, chunks, relevant, 3) / QUERIES.size
        println(query.padEnd(34) + cells.getValue("bm25").padStart(6) + cells.getValue("embedding").padStart(11) + cells.getValue("hybrid").padStart(8))
    }
    println("mean recall@3: " + totals.entries.joinToString(", ", "{", "}") { "'${it.key}': ${num(it.value)}" })
    val texts = chunks.associate { it.id to it.text }
    val query = "send back parcel"
    val hybrid = rankers(chunks, query).getValue("hybrid")
    val reranked = rerank(query, hybrid.take(10), texts, ::toyReranker, 3)
    println("'$query': hybrid top 3 ${py(hybrid.take(3))} -> reranked top 3 ${py(reranked)}")
    val bare = listOf(Chunk("a#0", "a", "The limit is 30 days after delivery."), Chunk("b#0", "b", "The limit is 5 users per workspace."))
    val context = mapOf("a#0" to "Returns policy: the return window for physical orders. ")
    val indexed = bare.associate { it.id to (context[it.id] ?: "") + it.text }
    println("'return window' on the bare chunks: ${py(bm25Rank(bare, "return window"))} | with a context sentence indexed in front: ${py(bm25Rank(bare, "return window", indexed))}")
}
