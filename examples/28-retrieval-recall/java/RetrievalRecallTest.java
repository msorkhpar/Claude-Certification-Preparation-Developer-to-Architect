import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetrievalRecallTest {
    private static double meanRecall(String mode) {
        List<RetrievalRecall.Chunk> chunks = RetrievalRecall.buildAll();
        double sum = 0;
        for (RetrievalRecall.Query q : RetrievalRecall.QUERIES) sum += RetrievalRecall.recall(RetrievalRecall.rankers(chunks, q.text()).get(mode), chunks, q.relevant(), 3);
        return sum / RetrievalRecall.QUERIES.size();
    }

    @Test
    void hybridBeatsEachSingleIndexAtRecall3() {
        assertEquals(List.of(0.5, 0.75, 1.0), List.of(meanRecall("bm25"), meanRecall("embedding"), meanRecall("hybrid")));
    }

    @Test
    void theEmbeddingHas64DimensionsAndLengthOne() {
        double[] v = RetrievalRecall.embed("deleting accounts");
        double sum = 0;
        for (double x : v) sum += x * x;
        assertEquals(64, v.length);
        assertEquals(1.0, sum, 1e-9);
    }

    @Test
    void fusionPutsAChunkFoundByBothListsFirst() {
        assertEquals("b", RetrievalRecall.fuse(List.of(List.of("a", "b"), List.of("c", "b")), 60).get(0));
    }

    @Test
    void aQueryWithNoSharedWordHasNoLexicalHit() {
        List<RetrievalRecall.Chunk> chunks = RetrievalRecall.buildAll();
        assertEquals(List.of(), RetrievalRecall.bm25Rank(chunks, "zzzz qqqq", null));
        assertNull(RetrievalRecall.firstRank(RetrievalRecall.rankers(chunks, "deleting accounts").get("bm25"), chunks, List.of("deletion")));
    }

    @Test
    void chunkWindowsOverlapAndRejectBadSizes() {
        assertEquals(List.of("a b c", "c d e", "e f"), RetrievalRecall.chunk("a b c d e f", 3, 1));
        assertThrows(IllegalArgumentException.class, () -> RetrievalRecall.chunk("a b", 2, 2));
    }
}
