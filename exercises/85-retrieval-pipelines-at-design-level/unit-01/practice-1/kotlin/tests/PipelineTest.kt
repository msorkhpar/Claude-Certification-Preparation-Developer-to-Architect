import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PipelineTest {
    private val plan = "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund."
    private val monthly = "# Monthly plan\n## Cancellation\nYou can cancel at any time."
    private val errors = "# Error codes\n## E-7310\nThe warehouse could not reserve stock.\n## E-4021\nThe payment gateway rejected the card."
    private val long = "# Guide\n## Setup\nInstall the agent. Configure the proxy. Restart the service. Check the logs for errors."

    private fun ids(chunks: List<Chunk>?): List<String> {
        assertNotNull(chunks, "no chunks returned")
        return chunks!!.map { it.id }
    }

    private fun texts(chunks: List<Chunk>?): List<String> {
        assertNotNull(chunks, "no chunks returned")
        return chunks!!.map { "${it.id} | ${it.text}" }
    }

    private fun cut(docId: String, text: String, maxWords: Int = 30): List<Chunk> {
        val result = chunkSections(docId, text, maxWords)
        assertNotNull(result, "no chunks returned")
        return result!!
    }

    @Test
    fun m1_sectionsBecomeChunksThatCarryTheirTitleAndSectionAndTheVersionOfTheirDocument() {
        val chunks = chunkSections("annual", plan)
        assertEquals(listOf("annual/Cancellation | Annual plan > Cancellation. You can cancel within 14 days for a full refund."), texts(chunks))
        assertEquals("annual", chunks!![0].doc)
        assertEquals(docVersion(plan), chunks[0].version)
        assertEquals(listOf("errors/E-7310", "errors/E-4021"), ids(chunkSections("errors", errors)))
    }

    @Test
    fun e1_aLongSectionSplitsAtSentenceEndsUnderTheWordLimitAndEveryPartKeepsThePrefix() {
        assertEquals(listOf("guide/Setup#1 | Guide > Setup. Install the agent. Configure the proxy.", "guide/Setup#2 | Guide > Setup. Restart the service. Check the logs for errors."), texts(chunkSections("guide", long, 8)))
        assertEquals(4, cut("guide", long, 3).size, "a sentence longer than the limit stays whole")
    }

    @Test
    fun e2_aCodeOutweighsAWordAndAWordInNoChunkMatchesNothing() {
        val chunks = cut("errors", errors) + cut("annual", plan)
        assertEquals(listOf("errors/E-7310"), search(chunks, "what does E-7310 mean"))
        assertEquals(listOf("errors/E-4021", "annual/Cancellation"), search(chunks, "cancel E-4021 card"))
        assertEquals(listOf("errors/E-7310", "errors/E-4021"), search(chunks, "card gateway E-7310"), "a code outweighs two plain words")
        assertEquals(emptyList<String>(), search(chunks, "when will I be reimbursed"))
        assertEquals(listOf("annual/Cancellation"), search(chunks, "cancel", 1))
    }

    @Test
    fun e3_aSearchForAReaderNeverReturnsAChunkOfADocumentThatReaderMayNotSee() {
        val chunks = cut("monthly", monthly) + cut("annual", plan)
        assertEquals(listOf("annual/Cancellation"), search(chunks, "cancel annual plan", 1))
        assertEquals(listOf("monthly/Cancellation"), search(chunks, "cancel annual plan", 1, setOf("monthly")))
        assertEquals(listOf("monthly/Cancellation"), search(chunks, "cancel annual plan", 3, setOf("monthly")))
        assertEquals(emptyList<String>(), search(chunks, "cancel", 3, emptySet()))
    }

    @Test
    fun e4_theMechanismFollowsTheCorpusSizeThenTheDataShapeThenTheQueryPattern() {
        assertEquals("cached prompt", chooseRetrieval(50000, "text", "identifier"))
        assertEquals("cached prompt", chooseRetrieval(199999, "table", "multi-hop"))
        assertEquals("embedding index", chooseRetrieval(200000, "text", "paraphrase"))
        assertEquals("structured query", chooseRetrieval(2000000, "table", "paraphrase"))
        assertEquals("structured query", chooseRetrieval(2000000, "table", "multi-hop"))
        assertEquals("agentic search", chooseRetrieval(2000000, "text", "multi-hop"))
        assertEquals("keyword index", chooseRetrieval(2000000, "text", "identifier"))
        assertEquals("hybrid index", chooseRetrieval(2000000, "text", "mixed"))
    }

    @Test
    fun e5_aReindexKeepsUnchangedDocumentsReplacesChangedOnesAddsNewOnesAndDropsRemovedOnes() {
        val old = cut("annual", plan) + cut("monthly", monthly) + cut("errors", errors)
        val docs = linkedMapOf("annual" to plan.replace("14 days", "30 days"), "errors" to errors, "guide" to long)
        val result = reindex(old, docs)
        assertNotNull(result, "reindex returned nothing")
        assertEquals(mapOf("added" to listOf("guide"), "replaced" to listOf("annual"), "removed" to listOf("monthly"), "kept" to listOf("errors")), result!!.report)
        assertEquals(listOf("annual/Cancellation", "errors/E-7310", "errors/E-4021", "guide/Setup"), ids(result.chunks))
        assertEquals(listOf("Annual plan > Cancellation. You can cancel within 30 days for a full refund."), result.chunks.filter { it.doc == "annual" }.map { it.text })
        assertTrue(result.chunks.none { it.doc == "monthly" })
    }

    @Test
    fun e6_staleListsTheChunksWhoseDocumentChangedOrVanished() {
        val old = cut("annual", plan) + cut("monthly", monthly) + cut("errors", errors)
        val docs = linkedMapOf("annual" to plan.replace("14 days", "30 days"), "errors" to errors)
        assertEquals(listOf("annual/Cancellation", "monthly/Cancellation"), stale(old, docs))
        val result = reindex(old, docs)
        assertNotNull(result, "reindex returned nothing")
        assertEquals(emptyList<String>(), stale(result!!.chunks, docs))
        assertEquals(emptyList<String>(), stale(old, mapOf("annual" to plan, "monthly" to monthly, "errors" to errors)))
    }

    @Test
    fun e7_recallCountsEveryLabelledQuestionAndAQuestionWithNoResultsIsAMiss() {
        val relevant = mapOf("q1" to "a", "q2" to "b", "q3" to "c")
        val results = mapOf("q1" to listOf("a", "x"), "q2" to listOf("x", "y", "b"))
        assertEquals(0.67, recallAtK(results, relevant, 3))
        assertEquals(0.33, recallAtK(results, relevant, 1))
        assertEquals(0.0, recallAtK(emptyMap(), relevant, 3))
        assertEquals(0.0, recallAtK(results, emptyMap(), 3))
    }
}
