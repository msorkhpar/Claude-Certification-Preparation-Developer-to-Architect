import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LaunchReviewTest {
    private static final List<String> CLEAN_FLAGS = List.of("feedback_loop", "model_measured", "replace_on_change", "deferral", "protected_segment", "rollback", "human_step", "owner", "accuracy_stated", "managed_settings", "irreversible_action", "team");
    private static final Map<String, Integer> CLEAN_NUMBERS = Map.of("team_value_chats", 15, "tool_tokens", 10000, "eval_cases", 20, "rollout_stages", 3, "retain_days", 365, "floor_days", 90, "ceiling_days", 365, "team_size", 10, "latency_ms", 2000, "availability_tenths", 995);

    /** The clean design with some flags added or dropped and some numbers changed. */
    private static final class Design {
        final Set<String> flags = new HashSet<>(CLEAN_FLAGS);
        final Map<String, Integer> numbers = new HashMap<>(CLEAN_NUMBERS);

        Design add(String... keys) {
            flags.addAll(List.of(keys));
            return this;
        }

        Design drop(String... keys) {
            List.of(keys).forEach(flags::remove);
            return this;
        }

        Design set(String key, int value) {
            numbers.put(key, value);
            return this;
        }

        List<String> run() {
            List<String> result = LaunchReview.launchReview(flags, numbers);
            assertNotNull(result, "launchReview returned nothing");
            return result;
        }
    }

    private static Design review() {
        return new Design();
    }

    @Test
    void m1_aDesignThatSitsExactlyAtEveryThresholdHasNoFindingsAndIsApproved() {
        List<String> findings = review().run();
        assertEquals(List.of(), findings);
        assertEquals("approve", LaunchReview.verdict(findings));
        assertEquals(List.of(0, 0, 0, 0, 0, 0, 0), LaunchReview.scorecard(findings));
        assertEquals(98, LaunchReview.neededAccuracy(250, 5));
    }

    @Test
    void e1_aMissingFeedbackLoopAFilterAfterRankingOrNoWayBackIsAHighFindingAndTheDesignIsRejected() {
        List<String> findings = review().add("filter_after_ranking").drop("feedback_loop").run();
        assertEquals(List.of("high P1 missing-feedback", "high P3 filter-after-ranking"), findings);
        assertEquals("reject", LaunchReview.verdict(findings));
        assertEquals(List.of("high P4 no-way-back"), review().drop("rollback").run());
    }

    @Test
    void e2_mediumFindingsReviseTheDesignAndLowFindingsAloneApproveIt() {
        List<String> medium = review().drop("owner").run();
        assertEquals(List.of("medium P6 no-accountable-owner"), medium);
        assertEquals("revise", LaunchReview.verdict(medium));
        List<String> low = review().drop("model_measured").run();
        assertEquals(List.of("low P2 model-not-measured"), low);
        assertEquals("approve", LaunchReview.verdict(low));
        assertEquals("reject", LaunchReview.verdict(review().drop("owner", "feedback_loop").run()));
    }

    @Test
    void e3_findingsAreOrderedBySeverityThenDomainThenRuleId() {
        List<String> findings = review().add("volatile_prefix").drop("protected_segment", "accuracy_stated", "owner", "feedback_loop").set("rollout_stages", 2).run();
        assertEquals(List.of(
            "high P1 missing-feedback",
            "medium P2 volatile-prefix",
            "medium P4 big-bang-rollout",
            "medium P4 no-protected-segment",
            "medium P6 no-accountable-owner",
            "low P6 accuracy-unstated"), findings);
    }

    @Test
    void e4_eachNumericThresholdPassesExactlyAtItsValueAndFailsOneStepBeyond() {
        assertEquals(List.of(), review().set("team_value_chats", 15).run());
        assertEquals(List.of("medium P1 team-below-price"), review().set("team_value_chats", 14).run());
        assertEquals(List.of(), review().drop("deferral").set("tool_tokens", 10000).run());
        assertEquals(List.of("medium P3 tool-bloat"), review().drop("deferral").set("tool_tokens", 10001).run());
        assertEquals(List.of(), review().set("tool_tokens", 10001).run());
        assertEquals(List.of(), review().set("eval_cases", 20).run());
        assertEquals(List.of("low P4 small-eval-set"), review().set("eval_cases", 19).run());
        assertEquals(List.of(), review().set("rollout_stages", 3).run());
        assertEquals(List.of("medium P4 big-bang-rollout"), review().set("rollout_stages", 2).run());
        assertEquals(List.of(), review().set("retain_days", 90).run());
        assertEquals(List.of("medium P5 retention-outside-window"), review().set("retain_days", 89).run());
        assertEquals(List.of(), review().set("retain_days", 365).run());
        assertEquals(List.of("medium P5 retention-outside-window"), review().set("retain_days", 366).run());
        assertEquals(List.of(), review().drop("managed_settings").set("team_size", 10).run());
        assertEquals(List.of("medium P7 unmanaged-team-settings"), review().drop("managed_settings").set("team_size", 11).run());
        assertEquals(List.of(), review().set("team_size", 11).run());
        assertEquals(List.of("medium P6 sla-without-numbers"), review().set("latency_ms", 0).run());
        assertEquals(List.of("medium P6 sla-without-numbers"), review().set("availability_tenths", 0).run());
        assertEquals(List.of(), review().set("latency_ms", 1).set("availability_tenths", 1).run());
    }

    @Test
    void e5_theScorecardCountsTheFindingsOfEachDomainFromP1ToP7() {
        assertEquals(List.of(2, 0, 1, 0, 0, 0, 1), LaunchReview.scorecard(List.of("high P1 a", "medium P1 b", "low P7 c", "medium P3 d")));
        assertEquals(List.of(0, 0, 0, 0, 0, 0, 0), LaunchReview.scorecard(List.of()));
    }

    @Test
    void e6_theAccuracyADesignNeedsIsTheBreakEvenRoundedUpFromTheTwoCosts() {
        assertEquals(98, LaunchReview.neededAccuracy(250, 5));
        assertEquals(91, LaunchReview.neededAccuracy(60, 5));
        assertEquals(66, LaunchReview.neededAccuracy(3, 1));
        assertEquals(0, LaunchReview.neededAccuracy(5, 5));
        assertEquals(0, LaunchReview.neededAccuracy(5, 6));
        assertEquals(0, LaunchReview.neededAccuracy(0, 5));
        assertEquals(0, LaunchReview.neededAccuracy(-1, 5));
    }

    @Test
    void e7_rulesThatDependOnASecondFactFireOnlyWhenBothHold() {
        assertEquals(List.of("medium P1 autonomy-without-need"), review().add("path_known").run());
        assertEquals(List.of(), review().drop("team").add("path_known").run());
        assertEquals(List.of(), review().add("agent").run());
        assertEquals(List.of("medium P1 autonomy-without-need"), review().drop("team").add("agent", "path_known").run());
        assertEquals(List.of("high P5 irreversible-without-person"), review().drop("human_step").run());
        assertEquals(List.of(), review().drop("human_step", "irreversible_action").run());
        assertEquals(List.of(), review().drop("team").set("team_value_chats", 3).run());
    }

    @Test
    void e8_retrievalAndPrivacyFlawsAreHighFindingsInDomainsP3AndP5() {
        assertEquals(List.of("high P3 agent-rights-only", "high P3 filter-after-ranking", "high P3 stale-index"), review().add("filter_after_ranking", "agent_rights_only").drop("replace_on_change").run());
        assertEquals(List.of("high P5 identifiers-reach-model", "high P5 residency-unmet", "medium P5 audit-keeps-content"), review().add("pii_reaches_model", "residency_unmet", "audit_keeps_content").run());
    }

    @Test
    void e9_aDesignWithEveryFlawGetsAll22FindingsAndAScorecardThatAddsUp() {
        Set<String> flags = Set.of("agent", "path_known", "volatile_prefix", "filter_after_ranking", "agent_rights_only", "pii_reaches_model", "residency_unmet", "audit_keeps_content", "irreversible_action", "team");
        Map<String, Integer> numbers = Map.of("team_value_chats", 14, "tool_tokens", 10001, "eval_cases", 19, "rollout_stages", 2, "retain_days", 366, "floor_days", 90, "ceiling_days", 365, "team_size", 11, "latency_ms", 0, "availability_tenths", 0);
        List<String> findings = LaunchReview.launchReview(flags, numbers);
        assertNotNull(findings, "launchReview returned nothing");
        assertEquals(22, findings.size());
        assertEquals("high P1 missing-feedback", findings.get(0));
        assertEquals("low P6 accuracy-unstated", findings.get(findings.size() - 1));
        assertEquals("reject", LaunchReview.verdict(findings));
        assertEquals(List.of(3, 2, 4, 4, 5, 3, 1), LaunchReview.scorecard(findings));
    }
}
