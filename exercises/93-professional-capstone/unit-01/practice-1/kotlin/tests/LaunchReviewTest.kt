import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LaunchReviewTest {
    private val cleanFlags = setOf("feedback_loop", "model_measured", "replace_on_change", "deferral", "protected_segment", "rollback", "human_step", "owner", "accuracy_stated", "managed_settings", "irreversible_action", "team")
    private val cleanNumbers = mapOf("team_value_chats" to 15, "tool_tokens" to 10000, "eval_cases" to 20, "rollout_stages" to 3, "retain_days" to 365, "floor_days" to 90, "ceiling_days" to 365, "team_size" to 10, "latency_ms" to 2000, "availability_tenths" to 995)

    /** The clean design with some flags added or dropped and some numbers changed. */
    private fun review(add: List<String> = listOf(), drop: List<String> = listOf(), nums: Map<String, Int> = mapOf()): List<String> {
        val result: List<String>? = launchReview((cleanFlags + add) - drop.toSet(), cleanNumbers + nums)
        assertNotNull(result, "launchReview returned nothing")
        return result!!
    }

    @Test
    fun m1_aDesignThatSitsExactlyAtEveryThresholdHasNoFindingsAndIsApproved() {
        val findings = review()
        assertEquals(listOf<String>(), findings)
        assertEquals("approve", verdict(findings))
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0), scorecard(findings))
        assertEquals(98, neededAccuracy(250, 5))
    }

    @Test
    fun e1_aMissingFeedbackLoopAFilterAfterRankingOrNoWayBackIsAHighFindingAndTheDesignIsRejected() {
        val findings = review(add = listOf("filter_after_ranking"), drop = listOf("feedback_loop"))
        assertEquals(listOf("high P1 missing-feedback", "high P3 filter-after-ranking"), findings)
        assertEquals("reject", verdict(findings))
        assertEquals(listOf("high P4 no-way-back"), review(drop = listOf("rollback")))
    }

    @Test
    fun e2_mediumFindingsReviseTheDesignAndLowFindingsAloneApproveIt() {
        val medium = review(drop = listOf("owner"))
        assertEquals(listOf("medium P6 no-accountable-owner"), medium)
        assertEquals("revise", verdict(medium))
        val low = review(drop = listOf("model_measured"))
        assertEquals(listOf("low P2 model-not-measured"), low)
        assertEquals("approve", verdict(low))
        assertEquals("reject", verdict(review(drop = listOf("owner", "feedback_loop"))))
    }

    @Test
    fun e3_findingsAreOrderedBySeverityThenDomainThenRuleId() {
        val findings = review(add = listOf("volatile_prefix"), drop = listOf("protected_segment", "accuracy_stated", "owner", "feedback_loop"), nums = mapOf("rollout_stages" to 2))
        assertEquals(
            listOf(
                "high P1 missing-feedback",
                "medium P2 volatile-prefix",
                "medium P4 big-bang-rollout",
                "medium P4 no-protected-segment",
                "medium P6 no-accountable-owner",
                "low P6 accuracy-unstated",
            ),
            findings,
        )
    }

    @Test
    fun e4_eachNumericThresholdPassesExactlyAtItsValueAndFailsOneStepBeyond() {
        assertEquals(listOf<String>(), review(nums = mapOf("team_value_chats" to 15)))
        assertEquals(listOf("medium P1 team-below-price"), review(nums = mapOf("team_value_chats" to 14)))
        assertEquals(listOf<String>(), review(drop = listOf("deferral"), nums = mapOf("tool_tokens" to 10000)))
        assertEquals(listOf("medium P3 tool-bloat"), review(drop = listOf("deferral"), nums = mapOf("tool_tokens" to 10001)))
        assertEquals(listOf<String>(), review(nums = mapOf("tool_tokens" to 10001)))
        assertEquals(listOf<String>(), review(nums = mapOf("eval_cases" to 20)))
        assertEquals(listOf("low P4 small-eval-set"), review(nums = mapOf("eval_cases" to 19)))
        assertEquals(listOf<String>(), review(nums = mapOf("rollout_stages" to 3)))
        assertEquals(listOf("medium P4 big-bang-rollout"), review(nums = mapOf("rollout_stages" to 2)))
        assertEquals(listOf<String>(), review(nums = mapOf("retain_days" to 90)))
        assertEquals(listOf("medium P5 retention-outside-window"), review(nums = mapOf("retain_days" to 89)))
        assertEquals(listOf<String>(), review(nums = mapOf("retain_days" to 365)))
        assertEquals(listOf("medium P5 retention-outside-window"), review(nums = mapOf("retain_days" to 366)))
        assertEquals(listOf<String>(), review(drop = listOf("managed_settings"), nums = mapOf("team_size" to 10)))
        assertEquals(listOf("medium P7 unmanaged-team-settings"), review(drop = listOf("managed_settings"), nums = mapOf("team_size" to 11)))
        assertEquals(listOf<String>(), review(nums = mapOf("team_size" to 11)))
        assertEquals(listOf("medium P6 sla-without-numbers"), review(nums = mapOf("latency_ms" to 0)))
        assertEquals(listOf("medium P6 sla-without-numbers"), review(nums = mapOf("availability_tenths" to 0)))
        assertEquals(listOf<String>(), review(nums = mapOf("latency_ms" to 1, "availability_tenths" to 1)))
    }

    @Test
    fun e5_theScorecardCountsTheFindingsOfEachDomainFromP1ToP7() {
        assertEquals(listOf(2, 0, 1, 0, 0, 0, 1), scorecard(listOf("high P1 a", "medium P1 b", "low P7 c", "medium P3 d")))
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0), scorecard(listOf()))
    }

    @Test
    fun e6_theAccuracyADesignNeedsIsTheBreakEvenRoundedUpFromTheTwoCosts() {
        assertEquals(98, neededAccuracy(250, 5))
        assertEquals(91, neededAccuracy(60, 5))
        assertEquals(66, neededAccuracy(3, 1))
        assertEquals(0, neededAccuracy(5, 5))
        assertEquals(0, neededAccuracy(5, 6))
        assertEquals(0, neededAccuracy(0, 5))
        assertEquals(0, neededAccuracy(-1, 5))
    }

    @Test
    fun e7_rulesThatDependOnASecondFactFireOnlyWhenBothHold() {
        assertEquals(listOf("medium P1 autonomy-without-need"), review(add = listOf("path_known")))
        assertEquals(listOf<String>(), review(drop = listOf("team"), add = listOf("path_known")))
        assertEquals(listOf<String>(), review(add = listOf("agent")))
        assertEquals(listOf("medium P1 autonomy-without-need"), review(drop = listOf("team"), add = listOf("agent", "path_known")))
        assertEquals(listOf("high P5 irreversible-without-person"), review(drop = listOf("human_step")))
        assertEquals(listOf<String>(), review(drop = listOf("human_step", "irreversible_action")))
        assertEquals(listOf<String>(), review(drop = listOf("team"), nums = mapOf("team_value_chats" to 3)))
    }

    @Test
    fun e8_retrievalAndPrivacyFlawsAreHighFindingsInDomainsP3AndP5() {
        assertEquals(listOf("high P3 agent-rights-only", "high P3 filter-after-ranking", "high P3 stale-index"), review(add = listOf("filter_after_ranking", "agent_rights_only"), drop = listOf("replace_on_change")))
        assertEquals(listOf("high P5 identifiers-reach-model", "high P5 residency-unmet", "medium P5 audit-keeps-content"), review(add = listOf("pii_reaches_model", "residency_unmet", "audit_keeps_content")))
    }

    @Test
    fun e9_aDesignWithEveryFlawGetsAll22FindingsAndAScorecardThatAddsUp() {
        val flags = setOf("agent", "path_known", "volatile_prefix", "filter_after_ranking", "agent_rights_only", "pii_reaches_model", "residency_unmet", "audit_keeps_content", "irreversible_action", "team")
        val numbers = mapOf("team_value_chats" to 14, "tool_tokens" to 10001, "eval_cases" to 19, "rollout_stages" to 2, "retain_days" to 366, "floor_days" to 90, "ceiling_days" to 365, "team_size" to 11, "latency_ms" to 0, "availability_tenths" to 0)
        val findings = launchReview(flags, numbers)
        assertEquals(22, findings.size)
        assertEquals("high P1 missing-feedback", findings.first())
        assertEquals("low P6 accuracy-unstated", findings.last())
        assertEquals("reject", verdict(findings))
        assertEquals(listOf(3, 2, 4, 4, 5, 3, 1), scorecard(findings))
    }
}
