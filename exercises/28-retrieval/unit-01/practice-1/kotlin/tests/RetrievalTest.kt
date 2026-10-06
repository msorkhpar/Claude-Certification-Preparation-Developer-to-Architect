import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RetrievalTest {
    private fun c(id: String, text: String) = Chunk(id, id.substringBefore("#"), text)

    /** "IllegalArgumentException" when the call throws it, "crash" for another exception, null when it returns. */
    private fun failureOf(call: () -> Any?): String? = try {
        call()
        null
    } catch (e: IllegalArgumentException) {
        "IllegalArgumentException"
    } catch (e: RuntimeException) {
        "crash"
    }

    private fun close(a: Double?, b: Double) = a != null && Math.abs(a - b) < 1e-9
    private fun ids(list: List<String>?): List<String> = list ?: listOf("no result")

    @Suppress("UNCHECKED_CAST")
    @Test
    fun m1_hybridSearchFindsWhatEachSingleIndexMisses() {
        val fx = Json.parse(Files.readString(Path.of("tests", "fixture.json"))) as Map<String, Any?>
        val corpus = (fx["corpus"] as List<Map<String, Any?>>).map { Doc(it["id"] as String, it["text"] as String) }
        val queries = (fx["queries"] as List<Map<String, Any?>>).map { Query(it["query"] as String, it["relevant"] as List<String>) }
        val chunks = buildChunks(corpus, (fx["chunk_size"] as Long).toInt(), (fx["overlap"] as Long).toInt()) ?: emptyList()
        val lexical = evaluate(chunks, queries, "bm25", 3)
        val semantic = evaluate(chunks, queries, "embedding", 3)
        val hybrid = evaluate(chunks, queries, "hybrid", 3)
        assertEquals(listOf(true, true, true), listOf(close(lexical, 0.5), close(semantic, 0.75), close(hybrid, 1.0)), "$lexical $semantic $hybrid")
    }

    @Test
    fun e1_chunksOverlapAndEndAtTheLastWord() {
        val words = "w1 w2 w3 w4 w5 w6 w7 w8 w9 w10"
        assertEquals(listOf("w1 w2 w3 w4", "w4 w5 w6 w7", "w7 w8 w9 w10"), chunk(words, 4, 1))
        assertEquals(listOf("w1 w2 w3 w4", "w5 w6 w7 w8", "w9 w10"), chunk(words, 4, 0))
        assertEquals(listOf("a b c d", "d e f g"), chunk("a b c d e f g", 4, 1))
        assertEquals(listOf("a b c"), chunk("a b c", 5, 2))
        assertEquals(listOf("a b c d"), chunk("a b c d", 4, 1))
        assertEquals(listOf(emptyList<String>(), emptyList<String>()), listOf(ids(chunk("", 3, 1)), ids(chunk("   ", 3, 1))))
        assertEquals(listOf("IllegalArgumentException", "IllegalArgumentException", "IllegalArgumentException"),
            listOf(failureOf { chunk("a b", 2, 2) }, failureOf { chunk("a b", 0, 0) }, failureOf { chunk("a b", 2, -1) }))
        val made = buildChunks(listOf(Doc("doc", "a b c d e"), Doc("other", "x y")), 3, 1)
        assertEquals(listOf(Chunk("doc#0", "doc", "a b c"), Chunk("doc#1", "doc", "c d e"), Chunk("other#0", "other", "x y")), made)
    }

    @Test
    fun e2_aRareWordOutweighsCommonOnesAndShortChunksWin() {
        val chunks = listOf(c("x#0", "alpha beta"), c("d1#0", "alpha beta delta"), c("d2#0", "alpha beta epsilon"), c("d3#0", "alpha beta zeta"),
            c("d4#0", "alpha beta eta"), c("y#0", "gamma theta"))
        assertEquals(listOf("y#0", "x#0", "d1#0", "d2#0", "d3#0", "d4#0"), ids(bm25Rank(chunks, "alpha beta gamma")))
        val shorter = listOf(c("long#0", "needle alpha beta gamma delta"), c("short#0", "needle alpha"), c("none#0", "other words"))
        assertEquals(listOf("short#0", "long#0"), ids(bm25Rank(shorter, "needle")))
        assertEquals(emptyList<String>(), ids(bm25Rank(shorter, "absent terms")))
        assertEquals(listOf("short#0", "long#0"), ids(bm25Rank(shorter, "Needle NEEDLE")))
        assertEquals("none#0", bm25Rank(shorter, "needle", mapOf("none#0" to "needle needle needle"))?.firstOrNull() ?: "no result")
    }

    @Test
    fun e3_fusionRewardsAgreementAndBreaksTiesById() {
        assertEquals(listOf("x", "z", "y", "w"), ids(fuse(listOf(listOf("x", "y", "z"), listOf("z", "x", "w")))))
        assertEquals(listOf("q", "p", "r"), ids(fuse(listOf(listOf("p", "q"), listOf("r", "q")))))
        assertEquals(listOf("a", "b"), ids(fuse(listOf(listOf("b"), listOf("a")))))
        assertEquals(listOf("x", "y"), ids(fuse(listOf(listOf("x", "x", "y")))))
        assertEquals(listOf("b", "a", "c"), ids(fuse(listOf(listOf("a", "b"), listOf("b", "c")), 1)))
        assertEquals(emptyList<String>(), ids(fuse(emptyList())))
    }

    @Test
    fun e4_rerankingOrdersThePoolByTheScorer() {
        val texts = mapOf("a" to "x", "b" to "y", "c" to "z", "d" to "w")
        val table = mapOf("x" to 0.2, "y" to 0.9, "z" to 0.9, "w" to 1.0)
        val scorer: Scorer = { _, t -> table.getValue(t) }
        assertEquals(listOf("b", "c"), ids(rerank("q", listOf("a", "b", "c"), texts, scorer, 2)))
        assertEquals(listOf("b", "c", "a"), ids(rerank("q", listOf("a", "b", "c"), texts, scorer)))
        val chunks = listOf(c("c1#0", "alpha"), c("c2#0", "alpha one"), c("c3#0", "alpha one two"), c("c4#0", "alpha one two three"))
        val preferLast = mapOf("alpha one two three" to 1.0, "alpha one two" to 0.5)
        val byTable: Scorer = { _, t -> preferLast[t] ?: 0.0 }
        assertEquals(listOf("c1#0", "c2#0", "c3#0", "c4#0"), ids(bm25Rank(chunks, "alpha")))
        assertEquals(listOf("c1#0"), ids(retrieve(chunks, "alpha", "bm25", 1, 2, null, byTable)))
        assertEquals(listOf("c4#0"), ids(retrieve(chunks, "alpha", "bm25", 1, 4, null, byTable)))
        assertEquals(listOf("c1#0", "c2#0"), ids(retrieve(chunks, "alpha", "bm25", 2)))
    }

    @Test
    fun e5_recallCountsDocumentsNotChunks() {
        val docOf = mapOf("a#0" to "A", "a#1" to "A", "b#0" to "B", "c#0" to "C")
        val ids = listOf("a#0", "a#1", "b#0", "c#0")
        assertTrue(close(recallAtK(ids, docOf, listOf("A", "B"), 2), 0.5))
        assertTrue(close(recallAtK(ids, docOf, listOf("A", "B"), 3), 1.0))
        assertTrue(close(recallAtK(ids, docOf, listOf("A", "Z"), 4), 0.5))
        assertTrue(close(recallAtK(ids, docOf, listOf("B"), 0), 0.0))
        assertTrue(close(recallAtK(ids, docOf, listOf("A"), 10), 1.0))
        assertEquals("IllegalArgumentException", failureOf { recallAtK(ids, docOf, emptyList(), 3) })
    }

    @Test
    fun e6_aContextSentenceMakesABareChunkFindable() {
        val chunks = listOf(c("a#0", "The limit is 30 days after delivery."), c("b#0", "The limit is 5 users per workspace."), c("c#0", "Our mascot is a friendly otter."))
        val contexts = mapOf("a#0" to "Returns policy: the return window for physical orders.")
        assertEquals(emptyList<String>(), ids(retrieve(chunks, "return window", "bm25", 2)))
        assertEquals(listOf("a#0"), ids(retrieve(chunks, "return window", "bm25", 2, 10, contexts, null)))
        val queries = listOf(Query("return window", listOf("a")))
        assertTrue(close(evaluate(chunks, queries, "bm25", 1), 0.0))
        assertTrue(close(evaluate(chunks, queries, "bm25", 1, 10, contexts, null), 1.0))
        assertEquals("The limit is 30 days after delivery.", chunks[0].text)
        assertEquals(64, embed("x").size)
    }
}
