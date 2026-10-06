import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PipelineTest {
    private static final String PLAN = "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.";
    private static final String MONTHLY = "# Monthly plan\n## Cancellation\nYou can cancel at any time.";
    private static final String ERRORS = "# Error codes\n## E-7310\nThe warehouse could not reserve stock.\n## E-4021\nThe payment gateway rejected the card.";
    private static final String LONG = "# Guide\n## Setup\nInstall the agent. Configure the proxy. Restart the service. Check the logs for errors.";

    private static List<String> ids(List<Pipeline.Chunk> chunks) {
        assertNotNull(chunks, "no chunks returned");
        return chunks.stream().map(Pipeline.Chunk::id).toList();
    }

    private static List<String> texts(List<Pipeline.Chunk> chunks) {
        assertNotNull(chunks, "no chunks returned");
        return chunks.stream().map(c -> c.id() + " | " + c.text()).toList();
    }

    @SafeVarargs
    private static List<Pipeline.Chunk> plus(List<Pipeline.Chunk>... lists) {
        List<Pipeline.Chunk> all = new java.util.ArrayList<>();
        for (List<Pipeline.Chunk> l : lists) {
            assertNotNull(l, "no chunks returned");
            all.addAll(l);
        }
        return all;
    }

    @Test
    void m1_sectionsBecomeChunksThatCarryTheirTitleAndSectionAndTheVersionOfTheirDocument() {
        var chunks = Pipeline.chunkSections("annual", PLAN);
        assertEquals(List.of("annual/Cancellation | Annual plan > Cancellation. You can cancel within 14 days for a full refund."), texts(chunks));
        assertEquals("annual", chunks.get(0).doc());
        assertEquals(Pipeline.docVersion(PLAN), chunks.get(0).version());
        assertEquals(List.of("errors/E-7310", "errors/E-4021"), ids(Pipeline.chunkSections("errors", ERRORS)));
    }

    @Test
    void e1_aLongSectionSplitsAtSentenceEndsUnderTheWordLimitAndEveryPartKeepsThePrefix() {
        assertEquals(List.of("guide/Setup#1 | Guide > Setup. Install the agent. Configure the proxy.", "guide/Setup#2 | Guide > Setup. Restart the service. Check the logs for errors."),
            texts(Pipeline.chunkSections("guide", LONG, 8)));
        assertEquals(4, Pipeline.chunkSections("guide", LONG, 3).size(), "a sentence longer than the limit stays whole");
    }

    @Test
    void e2_aCodeOutweighsAWordAndAWordInNoChunkMatchesNothing() {
        var chunks = plus(Pipeline.chunkSections("errors", ERRORS), Pipeline.chunkSections("annual", PLAN));
        assertEquals(List.of("errors/E-7310"), Pipeline.search(chunks, "what does E-7310 mean"));
        assertEquals(List.of("errors/E-4021", "annual/Cancellation"), Pipeline.search(chunks, "cancel E-4021 card"));
        assertEquals(List.of("errors/E-7310", "errors/E-4021"), Pipeline.search(chunks, "card gateway E-7310"), "a code outweighs two plain words");
        assertEquals(List.of(), Pipeline.search(chunks, "when will I be reimbursed"));
        assertEquals(List.of("annual/Cancellation"), Pipeline.search(chunks, "cancel", 1));
    }

    @Test
    void e3_aSearchForAReaderNeverReturnsAChunkOfADocumentThatReaderMayNotSee() {
        var chunks = plus(Pipeline.chunkSections("monthly", MONTHLY), Pipeline.chunkSections("annual", PLAN));
        assertEquals(List.of("annual/Cancellation"), Pipeline.search(chunks, "cancel annual plan", 1));
        assertEquals(List.of("monthly/Cancellation"), Pipeline.search(chunks, "cancel annual plan", 1, Set.of("monthly")));
        assertEquals(List.of("monthly/Cancellation"), Pipeline.search(chunks, "cancel annual plan", 3, Set.of("monthly")));
        assertEquals(List.of(), Pipeline.search(chunks, "cancel", 3, Set.of()));
    }

    @Test
    void e4_theMechanismFollowsTheCorpusSizeThenTheDataShapeThenTheQueryPattern() {
        assertEquals("cached prompt", Pipeline.chooseRetrieval(50000, "text", "identifier"));
        assertEquals("cached prompt", Pipeline.chooseRetrieval(199999, "table", "multi-hop"));
        assertEquals("embedding index", Pipeline.chooseRetrieval(200000, "text", "paraphrase"));
        assertEquals("structured query", Pipeline.chooseRetrieval(2000000, "table", "paraphrase"));
        assertEquals("structured query", Pipeline.chooseRetrieval(2000000, "table", "multi-hop"));
        assertEquals("agentic search", Pipeline.chooseRetrieval(2000000, "text", "multi-hop"));
        assertEquals("keyword index", Pipeline.chooseRetrieval(2000000, "text", "identifier"));
        assertEquals("hybrid index", Pipeline.chooseRetrieval(2000000, "text", "mixed"));
    }

    @Test
    void e5_aReindexKeepsUnchangedDocumentsReplacesChangedOnesAddsNewOnesAndDropsRemovedOnes() {
        var old = plus(Pipeline.chunkSections("annual", PLAN), Pipeline.chunkSections("monthly", MONTHLY), Pipeline.chunkSections("errors", ERRORS));
        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("annual", PLAN.replace("14 days", "30 days"));
        docs.put("errors", ERRORS);
        docs.put("guide", LONG);
        var result = Pipeline.reindex(old, docs);
        assertNotNull(result, "reindex returned nothing");
        assertEquals(Map.of("added", List.of("guide"), "replaced", List.of("annual"), "removed", List.of("monthly"), "kept", List.of("errors")), result.report());
        assertEquals(List.of("annual/Cancellation", "errors/E-7310", "errors/E-4021", "guide/Setup"), ids(result.chunks()));
        assertEquals(List.of("Annual plan > Cancellation. You can cancel within 30 days for a full refund."), result.chunks().stream().filter(c -> c.doc().equals("annual")).map(Pipeline.Chunk::text).toList());
        assertTrue(result.chunks().stream().noneMatch(c -> c.doc().equals("monthly")));
    }

    @Test
    void e6_staleListsTheChunksWhoseDocumentChangedOrVanished() {
        var old = plus(Pipeline.chunkSections("annual", PLAN), Pipeline.chunkSections("monthly", MONTHLY), Pipeline.chunkSections("errors", ERRORS));
        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("annual", PLAN.replace("14 days", "30 days"));
        docs.put("errors", ERRORS);
        assertEquals(List.of("annual/Cancellation", "monthly/Cancellation"), Pipeline.stale(old, docs));
        var result = Pipeline.reindex(old, docs);
        assertNotNull(result, "reindex returned nothing");
        assertEquals(List.of(), Pipeline.stale(result.chunks(), docs));
        assertEquals(List.of(), Pipeline.stale(old, Map.of("annual", PLAN, "monthly", MONTHLY, "errors", ERRORS)));
    }

    @Test
    void e7_recallCountsEveryLabelledQuestionAndAQuestionWithNoResultsIsAMiss() {
        Map<String, String> relevant = Map.of("q1", "a", "q2", "b", "q3", "c");
        Map<String, List<String>> results = Map.of("q1", List.of("a", "x"), "q2", List.of("x", "y", "b"));
        assertEquals(0.67, Pipeline.recallAtK(results, relevant, 3));
        assertEquals(0.33, Pipeline.recallAtK(results, relevant, 1));
        assertEquals(0.0, Pipeline.recallAtK(Map.of(), relevant, 3));
        assertEquals(0.0, Pipeline.recallAtK(results, Map.of(), 3));
    }
}
