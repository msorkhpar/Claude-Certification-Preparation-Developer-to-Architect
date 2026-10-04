import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ChunkingAndRecallTest {
    private val rule = "Items marked final sale cannot be returned, except when they arrive damaged."

    @Test
    fun aFixedCutSplitsTheRuleFromItsExceptionAndAHeadingCutKeepsThemTogether() {
        val fixed = chunkFixed("refunds", DOCS.getValue("refunds"), 12)
        assertEquals(4, fixed.size)
        assertFalse(holds(fixed, fixed.map { it.id }, rule))
        val sections = chunkSections("refunds", DOCS.getValue("refunds"), false)
        assertEquals(listOf("refunds/Eligibility", "refunds/Exceptions", "refunds/Process"), sections.map { it.id })
        assertTrue(holds(sections, listOf("refunds/Exceptions"), rule))
    }

    @Test
    fun aChunkThatCarriesItsTitleAndSectionCanBeToldFromItsTwin() {
        assertEquals(listOf("monthly/Cancellation"), lexical(index(DOCS, false), "cancel the annual plan", 1))
        assertEquals(listOf("annual/Cancellation"), lexical(index(DOCS, true), "cancel the annual plan", 1))
        assertTrue(chunkSections("annual", DOCS.getValue("annual"), true)[0].text.startsWith("Annual plan > Cancellation. You can cancel"))
    }

    @Test
    fun aCodeOutweighsAWordAndTheFusionRewardsAgreement() {
        val chunks = index(DOCS, true)
        assertEquals(listOf("errors/E-7310"), lexical(chunks, "what does E-7310 mean"))
        assertEquals(emptyList<String>(), lexical(chunks, "when will I be reimbursed"))
        assertEquals(listOf("b", "a", "c"), fuse(listOf(listOf("a", "b"), listOf("b", "c"))))
        assertEquals(listOf("x"), fuse(listOf(emptyList(), listOf("x"))))
    }

    @Test
    fun addingChunksWithoutRemovingTheOldOnesLeavesAStaleAnswerInTheIndex() {
        val chunks = index(DOCS, true)
        val edited = DOCS.getValue("refunds").replace("within 30 days", "within 60 days")
        val live = DOCS + ("refunds" to edited)
        assertEquals(listOf("refunds/Eligibility"), stale(chunks, live))
        assertEquals(listOf("refunds/Eligibility"), stale(reindexAdditive(chunks, "refunds", edited), live))
        val replaced = reindexReplace(chunks, "refunds", edited)
        assertEquals(emptyList<String>(), stale(replaced, live))
        assertEquals(chunks.size, replaced.size)
    }

    @Test
    fun theMechanismFollowsTheSizeOfTheCorpusThenTheShapeOfTheDataThenTheQuery() {
        assertEquals("cached prompt", chooseRetrieval(199999, "table", "multi-hop"))
        assertEquals("structured query", chooseRetrieval(200000, "table", "identifier"))
        assertEquals("agentic search", chooseRetrieval(5000000, "text", "multi-hop"))
        assertEquals("keyword index", chooseRetrieval(5000000, "text", "identifier"))
        assertEquals("embedding index", chooseRetrieval(5000000, "text", "paraphrase"))
        assertEquals("hybrid index", chooseRetrieval(5000000, "text", "mixed"))
    }

    @Test
    fun retrievalAndGenerationAreJudgedApart() {
        assertEquals(listOf("ok", "generation", "retrieval", "unsupported"), listOf(true to true, true to false, false to false, false to true).map { layer(it.first, it.second) })
    }
}
