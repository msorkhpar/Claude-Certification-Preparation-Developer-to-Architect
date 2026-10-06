import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RetrievalRecallTest {
    private fun meanRecall(mode: String, k: Int = 3): Double {
        val chunks = buildAll()
        return QUERIES.sumOf { (q, rel) -> recall(rankers(chunks, q).getValue(mode), chunks, rel, k) } / QUERIES.size
    }

    @Test
    fun hybridBeatsEachSingleIndexAtRecall3() {
        assertEquals(listOf(0.5, 0.75, 1.0), listOf(meanRecall("bm25"), meanRecall("embedding"), meanRecall("hybrid")))
    }

    @Test
    fun theEmbeddingHas64DimensionsAndLengthOne() {
        val v = embed("deleting accounts")
        assertEquals(64, v.size)
        assertEquals(1.0, v.sumOf { it * it }, 1e-9)
    }

    @Test
    fun fusionPutsAChunkFoundByBothListsFirst() {
        assertEquals("b", fuse(listOf(listOf("a", "b"), listOf("c", "b")))[0])
    }

    @Test
    fun aQueryWithNoSharedWordHasNoLexicalHit() {
        assertEquals(emptyList<String>(), bm25Rank(buildAll(), "zzzz qqqq"))
        assertNull(firstRank(rankers(buildAll(), "deleting accounts").getValue("bm25"), buildAll(), listOf("deletion")))
    }

    @Test
    fun chunkWindowsOverlapAndRejectBadSizes() {
        assertEquals(listOf("a b c", "c d e", "e f"), chunk("a b c d e f", 3, 1))
        assertThrows(IllegalArgumentException::class.java) { chunk("a b", 2, 2) }
    }
}
