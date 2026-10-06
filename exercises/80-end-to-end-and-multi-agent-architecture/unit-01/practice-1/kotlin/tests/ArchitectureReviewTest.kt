import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArchitectureReviewTest {
    private fun stages(vararg over: Pair<String, Any?>): Map<String, Any?> {
        val s = linkedMapOf<String, Any?>("input" to listOf("parse"), "processing" to listOf("classify", "route"), "output" to listOf("validate", "send"), "feedback" to listOf("review a sample"))
        for ((k, v) in over) if (v == null) s.remove(k) else s[k] = v
        return s
    }

    private fun design(vararg over: Pair<String, Any?>): Map<String, Any?> = linkedMapOf<String, Any?>(
        "name" to "intake", "pattern" to "workflow", "agents" to 1, "cost" to 3, "path_known" to true, "parallel_independent" to false, "shared_context" to false,
        "needs_audit" to true, "writes_without_approval" to false, "stages" to stages(),
    ).also { it.putAll(over) }

    private fun finding(rule: String, severity: String): Map<String, Any?> = mapOf("rule" to rule, "severity" to severity)

    private fun reviewed(design: Map<String, Any?>): List<Map<String, Any?>> {
        val result = review(design)
        assertNotNull(result, "review returned nothing")
        return result!!
    }

    private fun verdictOf(findings: List<Map<String, Any?>>): String {
        val result = verdict(findings)
        assertNotNull(result, "verdict returned nothing")
        return result!!
    }

    private fun rules(design: Map<String, Any?>): List<String> = reviewed(design).map { it["rule"] as String }

    @Test
    fun m1_aSoundDesignPassesReviewWithNoFindings() {
        assertEquals(emptyList<Map<String, Any?>>(), reviewed(design()))
        assertEquals("approve", verdictOf(emptyList()))
    }

    @Test
    fun e1_aDesignWithoutAFeedbackLoopOrAStageIsRejected() {
        assertEquals(listOf(finding("no-feedback", "high")), reviewed(design("stages" to stages("feedback" to emptyList<String>()))))
        assertEquals(listOf("no-feedback"), rules(design("stages" to stages("feedback" to null))))
        for (stage in listOf("input", "processing")) assertEquals(listOf("missing-stage:$stage"), rules(design("stages" to stages(stage to emptyList<String>()))))
        assertEquals(listOf("missing-stage:output"), rules(design("stages" to stages("output" to emptyList<String>()))))
    }

    @Test
    fun e2_autonomyIsFlaggedOnlyWhenThePathIsKnown() {
        assertEquals(listOf(finding("autonomy-without-need", "medium")), reviewed(design("pattern" to "agent")))
        assertEquals(listOf("autonomy-without-need"), rules(design("pattern" to "multi-agent", "agents" to 1)))
        assertEquals(emptyList<String>(), rules(design("pattern" to "agent", "path_known" to false)))
        assertEquals(emptyList<String>(), rules(design("pattern" to "augmented")))
    }

    @Test
    fun e3_severalAgentsNeedIndependentPartsAndNoSharedContext() {
        assertEquals(listOf(finding("team-without-independence", "high")), reviewed(design("pattern" to "multi-agent", "path_known" to false, "agents" to 3, "shared_context" to true, "parallel_independent" to true)))
        assertEquals(listOf("team-without-independence"), rules(design("pattern" to "multi-agent", "path_known" to false, "agents" to 3, "parallel_independent" to false)))
        assertEquals(emptyList<String>(), rules(design("pattern" to "multi-agent", "path_known" to false, "agents" to 3, "parallel_independent" to true)))
        assertEquals(emptyList<String>(), rules(design("pattern" to "agent", "path_known" to false, "agents" to 1, "shared_context" to true)))
    }

    @Test
    fun e4_anUnapprovedWriteIsAFindingOnlyWhenAnAuditIsNeeded() {
        assertEquals(listOf(finding("unapproved-write", "high")), reviewed(design("writes_without_approval" to true)))
        assertEquals(emptyList<String>(), rules(design("writes_without_approval" to true, "needs_audit" to false)))
        assertEquals(emptyList<String>(), rules(design("writes_without_approval" to false)))
    }

    @Test
    fun e5_outputThatNobodyValidatesIsFlagged() {
        assertEquals(listOf(finding("unvalidated-output", "medium")), reviewed(design("stages" to stages("output" to listOf("send")))))
        assertEquals(emptyList<String>(), rules(design("stages" to stages("output" to listOf("validate")))))
    }

    @Test
    fun e6_findingsAreOrderedBySeverityThenRuleAndTheVerdictFollowsTheWorst() {
        val messy = design("pattern" to "agent", "writes_without_approval" to true, "stages" to mapOf("input" to listOf("parse"), "processing" to listOf("act"), "output" to listOf("send"), "feedback" to emptyList<String>()))
        assertEquals(listOf(finding("no-feedback", "high"), finding("unapproved-write", "high"), finding("autonomy-without-need", "medium"), finding("unvalidated-output", "medium")), reviewed(messy))
        assertEquals("reject", verdictOf(reviewed(messy)))
        assertEquals("revise", verdictOf(reviewed(design("pattern" to "agent"))))
        assertEquals("reject", verdictOf(listOf(finding("x", "medium"), finding("y", "high"))))
        assertEquals("revise", verdictOf(listOf(finding("x", "medium"))))
    }

    @Test
    fun e7_theCheapestDesignThatIsNotRejectedWinsAndTiesGoByName() {
        val cheapButRejected = design("name" to "a-cheap", "cost" to 1, "stages" to stages("feedback" to emptyList<String>()))
        val revise = design("name" to "b-revise", "cost" to 2, "pattern" to "agent")
        val sound = design("name" to "c-sound", "cost" to 5)
        assertEquals("b-revise", cheapestAdequate(listOf(cheapButRejected, sound, revise)))
        assertEquals("alpha", cheapestAdequate(listOf(design("name" to "zeta", "cost" to 2), design("name" to "alpha", "cost" to 2), design("name" to "mid", "cost" to 4))))
        assertNull(cheapestAdequate(listOf(cheapButRejected)))
        assertNull(cheapestAdequate(emptyList()))
    }
}
