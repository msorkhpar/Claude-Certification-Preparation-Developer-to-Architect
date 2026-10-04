import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BatchReviewTest {
    private fun <T : Any> got(value: T?): T {
        assertNotNull(value, "the function returned nothing")
        return value!!
    }

    private fun finding(severity: String, confidence: Int) = Finding("a.py", 10, severity, "unchecked input", confidence)

    @Test
    fun m1_theIntervalBetweenSubmissionsLeavesRoomForTheWindowAndTheHandling() {
        assertEquals(4, submissionInterval(30))
        assertEquals(20, submissionInterval(48, 24, 4))
    }

    @Test
    fun e1_anSlaWithoutRoomForABatchIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { submissionInterval(26) }
        assertThrows(IllegalArgumentException::class.java) { submissionInterval(20) }
    }

    @Test
    fun e2_aBlockingCheckOrAToolLoopNeedsTheSynchronousApi() {
        assertEquals("synchronous", chooseApi(true))
        assertEquals("batch", chooseApi(false))
        assertEquals("synchronous", chooseApi(false, true))
    }

    @Test
    fun e3_onlyTheItemsThatDidNotSucceedAreResubmittedByCustomId() {
        val results = listOf(Result("a1", "succeeded"), Result("a2", "expired"), Result("a3", "succeeded"), Result("a4", "canceled"), Result("a5", "server_error"))
        assertEquals(listOf(Step("a2", "resubmit"), Step("a4", "resubmit"), Step("a5", "resubmit")), resubmissionPlan(results, emptyMap(), 1000))
        assertEquals(emptyList<Step>(), resubmissionPlan(listOf(Result("a1", "succeeded")), emptyMap(), 1000))
    }

    @Test
    fun e4_anItemOverTheLimitIsChunkedAndARejectedRequestIsFixedFirst() {
        val results = listOf(Result("big", "invalid_request"), Result("bad", "invalid_request"), Result("late", "expired"), Result("bigexp", "expired"))
        val sizes = mapOf("big" to 5000, "bad" to 100, "late" to 100, "bigexp" to 5000)
        assertEquals(listOf(Step("big", "chunk"), Step("bad", "fix"), Step("late", "resubmit"), Step("bigexp", "chunk")), resubmissionPlan(results, sizes, 1000))
    }

    @Test
    fun e5_aMultiFileReviewGetsALocalPassPerFileAndOneIntegrationPass() {
        val plan = got(reviewPlan(listOf("a.py", "b.py", "c.py")))
        assertEquals(listOf("local:a.py", "local:b.py", "local:c.py", "integration"), plan.map { it.name })
        assertEquals(listOf("a.py"), plan[0].files)
        assertEquals(listOf("a.py", "b.py", "c.py"), plan[3].files)
        assertEquals(listOf("local:a.py"), got(reviewPlan(listOf("a.py"))).map { it.name })
    }

    @Test
    fun e6_theSameFindingFromTwoPassesIsOneFindingWithTheHighestSeverityAndTheLowestConfidence() {
        val merged = got(mergePasses(listOf(listOf(finding("low", 95)), listOf(finding("high", 85), Finding("b.py", 3, "medium", "race", 90)))))
        assertEquals(2, merged.size)
        val first = merged[0]
        assertEquals(listOf<Any>("a.py", 10, "high", 2, 85), listOf(first.file, first.line, first.severity, first.passes, first.confidence))
        assertEquals(1, merged[1].passes)
    }

    @Test
    fun e7_aFindingIsAcceptedOnlyWhenTwoIndependentPassesAgreeWithConfidence() {
        val samePass = got(mergePasses(listOf(listOf(finding("medium", 90), finding("medium", 90)), emptyList())))
        assertEquals(1, samePass[0].passes)
        assertEquals("verify", samePass[0].route)
        assertEquals("verify", got(mergePasses(listOf(listOf(finding("medium", 99)))))[0].route)
        assertEquals("verify", got(mergePasses(listOf(listOf(finding("medium", 60)), listOf(finding("medium", 90)))))[0].route)
        assertEquals("accept", got(mergePasses(listOf(listOf(finding("medium", 80)), listOf(finding("medium", 90)))))[0].route)
    }
}
