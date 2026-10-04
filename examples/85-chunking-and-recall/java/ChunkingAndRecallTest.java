import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChunkingAndRecallTest {
    private static final String RULE = "Items marked final sale cannot be returned, except when they arrive damaged.";

    private static List<String> ids(List<ChunkingAndRecall.Chunk> chunks) {
        return chunks.stream().map(ChunkingAndRecall.Chunk::id).toList();
    }

    @Test
    void aFixedCutSplitsTheRuleFromItsExceptionAndAHeadingCutKeepsThemTogether() {
        var fixed = ChunkingAndRecall.chunkFixed("refunds", ChunkingAndRecall.DOCS.get("refunds"), 12);
        assertEquals(4, fixed.size());
        assertFalse(ChunkingAndRecall.holds(fixed, ids(fixed), RULE));
        var sections = ChunkingAndRecall.chunkSections("refunds", ChunkingAndRecall.DOCS.get("refunds"), false);
        assertEquals(List.of("refunds/Eligibility", "refunds/Exceptions", "refunds/Process"), ids(sections));
        assertTrue(ChunkingAndRecall.holds(sections, List.of("refunds/Exceptions"), RULE));
    }

    @Test
    void aChunkThatCarriesItsTitleAndSectionCanBeToldFromItsTwin() {
        assertEquals(List.of("monthly/Cancellation"), ChunkingAndRecall.lexical(ChunkingAndRecall.index(ChunkingAndRecall.DOCS, false), "cancel the annual plan", 1));
        assertEquals(List.of("annual/Cancellation"), ChunkingAndRecall.lexical(ChunkingAndRecall.index(ChunkingAndRecall.DOCS, true), "cancel the annual plan", 1));
        assertTrue(ChunkingAndRecall.chunkSections("annual", ChunkingAndRecall.DOCS.get("annual"), true).get(0).text().startsWith("Annual plan > Cancellation. You can cancel"));
    }

    @Test
    void aCodeOutweighsAWordAndTheFusionRewardsAgreement() {
        var chunks = ChunkingAndRecall.index(ChunkingAndRecall.DOCS, true);
        assertEquals(List.of("errors/E-7310"), ChunkingAndRecall.lexical(chunks, "what does E-7310 mean"));
        assertEquals(List.of(), ChunkingAndRecall.lexical(chunks, "when will I be reimbursed"));
        assertEquals(List.of("b", "a", "c"), ChunkingAndRecall.fuse(List.of(List.of("a", "b"), List.of("b", "c"))));
        assertEquals(List.of("x"), ChunkingAndRecall.fuse(List.of(List.of(), List.of("x"))));
    }

    @Test
    void addingChunksWithoutRemovingTheOldOnesLeavesAStaleAnswerInTheIndex() {
        var chunks = ChunkingAndRecall.index(ChunkingAndRecall.DOCS, true);
        String edited = ChunkingAndRecall.DOCS.get("refunds").replace("within 30 days", "within 60 days");
        Map<String, String> live = new LinkedHashMap<>(ChunkingAndRecall.DOCS);
        live.put("refunds", edited);
        assertEquals(List.of("refunds/Eligibility"), ChunkingAndRecall.stale(chunks, live));
        assertEquals(List.of("refunds/Eligibility"), ChunkingAndRecall.stale(ChunkingAndRecall.reindexAdditive(chunks, "refunds", edited), live));
        var replaced = ChunkingAndRecall.reindexReplace(chunks, "refunds", edited);
        assertEquals(List.of(), ChunkingAndRecall.stale(replaced, live));
        assertEquals(chunks.size(), replaced.size());
    }

    @Test
    void theMechanismFollowsTheSizeOfTheCorpusThenTheShapeOfTheDataThenTheQuery() {
        assertEquals("cached prompt", ChunkingAndRecall.chooseRetrieval(199999, "table", "multi-hop"));
        assertEquals("structured query", ChunkingAndRecall.chooseRetrieval(200000, "table", "identifier"));
        assertEquals("agentic search", ChunkingAndRecall.chooseRetrieval(5000000, "text", "multi-hop"));
        assertEquals("keyword index", ChunkingAndRecall.chooseRetrieval(5000000, "text", "identifier"));
        assertEquals("embedding index", ChunkingAndRecall.chooseRetrieval(5000000, "text", "paraphrase"));
        assertEquals("hybrid index", ChunkingAndRecall.chooseRetrieval(5000000, "text", "mixed"));
    }

    @Test
    void retrievalAndGenerationAreJudgedApart() {
        List<String> got = new ArrayList<>();
        for (boolean[] c : new boolean[][] {{true, true}, {true, false}, {false, false}, {false, true}}) got.add(ChunkingAndRecall.layer(c[0], c[1]));
        assertEquals(List.of("ok", "generation", "retrieval", "unsupported"), got);
    }
}
