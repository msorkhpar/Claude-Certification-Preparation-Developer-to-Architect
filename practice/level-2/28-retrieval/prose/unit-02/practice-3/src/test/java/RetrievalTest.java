import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class RetrievalTest {
    private static Chunk c(String id, String text) {
        return new Chunk(id, id.split("#")[0], text);
    }

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    /** "IllegalArgumentException" when the call throws it, "crash" for another exception, null when it returns. */
    private static String failureOf(Supplier<Object> call) {
        try {
            call.get();
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException";
        } catch (RuntimeException e) {
            return "crash";
        }
        return null;
    }

    private static boolean close(Double a, double b) {
        return a != null && Math.abs(a - b) < 1e-9;
    }

    private static List<String> orEmptyMarker(List<String> ids) {
        return ids == null ? List.of("no result") : ids;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fixture() throws IOException {
        return (Map<String, Object>) Json.parse(Files.readString(Path.of("tests", "fixture.json")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void m1_hybridSearchFindsWhatEachSingleIndexMisses() throws IOException {
        Map<String, Object> fx = fixture();
        List<Doc> corpus = new ArrayList<>();
        for (Object o : (List<Object>) fx.get("corpus")) corpus.add(new Doc((String) ((Map<String, Object>) o).get("id"), (String) ((Map<String, Object>) o).get("text")));
        List<Query> queries = new ArrayList<>();
        for (Object o : (List<Object>) fx.get("queries")) queries.add(new Query((String) ((Map<String, Object>) o).get("query"), (List<String>) ((Map<String, Object>) o).get("relevant")));
        List<Chunk> chunks = Retrieval.buildChunks(corpus, ((Long) fx.get("chunk_size")).intValue(), ((Long) fx.get("overlap")).intValue());
        assertNotNull(chunks);
        double lexical = Retrieval.evaluate(chunks, queries, "bm25", 3);
        double semantic = Retrieval.evaluate(chunks, queries, "embedding", 3);
        double hybrid = Retrieval.evaluate(chunks, queries, "hybrid", 3);
        assertEquals(List.of(true, true, true), List.of(close(lexical, 0.5), close(semantic, 0.75), close(hybrid, 1.0)), lexical + " " + semantic + " " + hybrid);
    }

    @Test
    void e1_chunksOverlapAndEndAtTheLastWord() {
        String words = "w1 w2 w3 w4 w5 w6 w7 w8 w9 w10";
        assertEquals(List.of("w1 w2 w3 w4", "w4 w5 w6 w7", "w7 w8 w9 w10"), Retrieval.chunk(words, 4, 1));
        assertEquals(List.of("w1 w2 w3 w4", "w5 w6 w7 w8", "w9 w10"), Retrieval.chunk(words, 4, 0));
        assertEquals(List.of("a b c d", "d e f g"), Retrieval.chunk("a b c d e f g", 4, 1));
        assertEquals(List.of("a b c"), Retrieval.chunk("a b c", 5, 2));
        assertEquals(List.of("a b c d"), Retrieval.chunk("a b c d", 4, 1));
        assertEquals(List.of(List.of(), List.of()), List.of(orEmptyMarker(Retrieval.chunk("", 3, 1)), orEmptyMarker(Retrieval.chunk("   ", 3, 1))));
        assertEquals(List.of("IllegalArgumentException", "IllegalArgumentException", "IllegalArgumentException"),
                java.util.Arrays.asList(failureOf(() -> Retrieval.chunk("a b", 2, 2)), failureOf(() -> Retrieval.chunk("a b", 0, 0)), failureOf(() -> Retrieval.chunk("a b", 2, -1))));
        List<Chunk> made = Retrieval.buildChunks(List.of(new Doc("doc", "a b c d e"), new Doc("other", "x y")), 3, 1);
        assertEquals(List.of(new Chunk("doc#0", "doc", "a b c"), new Chunk("doc#1", "doc", "c d e"), new Chunk("other#0", "other", "x y")), made);
    }

    @Test
    void e2_aRareWordOutweighsCommonOnesAndShortChunksWin() {
        List<Chunk> chunks = List.of(c("x#0", "alpha beta"), c("d1#0", "alpha beta delta"), c("d2#0", "alpha beta epsilon"), c("d3#0", "alpha beta zeta"),
                c("d4#0", "alpha beta eta"), c("y#0", "gamma theta"));
        assertEquals(List.of("y#0", "x#0", "d1#0", "d2#0", "d3#0", "d4#0"), orEmptyMarker(Retrieval.bm25Rank(chunks, "alpha beta gamma")));
        List<Chunk> shorter = List.of(c("long#0", "needle alpha beta gamma delta"), c("short#0", "needle alpha"), c("none#0", "other words"));
        assertEquals(List.of("short#0", "long#0"), orEmptyMarker(Retrieval.bm25Rank(shorter, "needle")));
        assertEquals(List.of(), orEmptyMarker(Retrieval.bm25Rank(shorter, "absent terms")));
        assertEquals(List.of("short#0", "long#0"), orEmptyMarker(Retrieval.bm25Rank(shorter, "Needle NEEDLE")));
        List<String> boosted = Retrieval.bm25Rank(shorter, "needle", map("none#0", "needle needle needle"));
        assertEquals("none#0", boosted == null || boosted.isEmpty() ? "no result" : boosted.get(0));
    }

    @Test
    void e3_fusionRewardsAgreementAndBreaksTiesById() {
        assertEquals(List.of("x", "z", "y", "w"), orEmptyMarker(Retrieval.fuse(List.of(List.of("x", "y", "z"), List.of("z", "x", "w")))));
        assertEquals(List.of("q", "p", "r"), orEmptyMarker(Retrieval.fuse(List.of(List.of("p", "q"), List.of("r", "q")))));
        assertEquals(List.of("a", "b"), orEmptyMarker(Retrieval.fuse(List.of(List.of("b"), List.of("a")))));
        assertEquals(List.of("x", "y"), orEmptyMarker(Retrieval.fuse(List.of(List.of("x", "x", "y")))));
        assertEquals(List.of("b", "a", "c"), orEmptyMarker(Retrieval.fuse(List.of(List.of("a", "b"), List.of("b", "c")), 1)));
        assertEquals(List.of(), orEmptyMarker(Retrieval.fuse(List.of())));
    }

    @Test
    void e4_rerankingOrdersThePoolByTheScorer() {
        Map<String, String> texts = map("a", "x", "b", "y", "c", "z", "d", "w");
        Map<String, Double> table = Map.of("x", 0.2, "y", 0.9, "z", 0.9, "w", 1.0);
        BiFunction<String, String, Double> scorer = (q, t) -> table.get(t);
        assertEquals(List.of("b", "c"), orEmptyMarker(Retrieval.rerank("q", List.of("a", "b", "c"), texts, scorer, 2)));
        assertEquals(List.of("b", "c", "a"), orEmptyMarker(Retrieval.rerank("q", List.of("a", "b", "c"), texts, scorer, null)));
        List<Chunk> chunks = List.of(c("c1#0", "alpha"), c("c2#0", "alpha one"), c("c3#0", "alpha one two"), c("c4#0", "alpha one two three"));
        Map<String, Double> preferLast = Map.of("alpha one two three", 1.0, "alpha one two", 0.5);
        BiFunction<String, String, Double> byTable = (q, t) -> preferLast.getOrDefault(t, 0.0);
        assertEquals(List.of("c1#0", "c2#0", "c3#0", "c4#0"), orEmptyMarker(Retrieval.bm25Rank(chunks, "alpha")));
        assertEquals(List.of("c1#0"), orEmptyMarker(Retrieval.retrieve(chunks, "alpha", "bm25", 1, 2, null, byTable)));
        assertEquals(List.of("c4#0"), orEmptyMarker(Retrieval.retrieve(chunks, "alpha", "bm25", 1, 4, null, byTable)));
        assertEquals(List.of("c1#0", "c2#0"), orEmptyMarker(Retrieval.retrieve(chunks, "alpha", "bm25", 2)));
    }

    @Test
    void e5_recallCountsDocumentsNotChunks() {
        Map<String, String> docOf = map("a#0", "A", "a#1", "A", "b#0", "B", "c#0", "C");
        List<String> ids = List.of("a#0", "a#1", "b#0", "c#0");
        assertTrue(close(Retrieval.recallAtK(ids, docOf, List.of("A", "B"), 2), 0.5));
        assertTrue(close(Retrieval.recallAtK(ids, docOf, List.of("A", "B"), 3), 1.0));
        assertTrue(close(Retrieval.recallAtK(ids, docOf, List.of("A", "Z"), 4), 0.5));
        assertTrue(close(Retrieval.recallAtK(ids, docOf, List.of("B"), 0), 0.0));
        assertTrue(close(Retrieval.recallAtK(ids, docOf, List.of("A"), 10), 1.0));
        assertEquals("IllegalArgumentException", failureOf(() -> Retrieval.recallAtK(ids, docOf, List.of(), 3)));
    }

    @Test
    void e6_aContextSentenceMakesABareChunkFindable() {
        List<Chunk> chunks = List.of(c("a#0", "The limit is 30 days after delivery."), c("b#0", "The limit is 5 users per workspace."), c("c#0", "Our mascot is a friendly otter."));
        Map<String, String> contexts = map("a#0", "Returns policy: the return window for physical orders.");
        assertEquals(List.of(), orEmptyMarker(Retrieval.retrieve(chunks, "return window", "bm25", 2)));
        assertEquals(List.of("a#0"), orEmptyMarker(Retrieval.retrieve(chunks, "return window", "bm25", 2, 10, contexts, null)));
        List<Query> queries = List.of(new Query("return window", List.of("a")));
        assertTrue(close(Retrieval.evaluate(chunks, queries, "bm25", 1), 0.0));
        assertTrue(close(Retrieval.evaluate(chunks, queries, "bm25", 1, 10, contexts, null), 1.0));
        assertEquals("The limit is 30 days after delivery.", chunks.get(0).text());
        assertEquals(64, Retrieval.embed("x").length);
    }
}
