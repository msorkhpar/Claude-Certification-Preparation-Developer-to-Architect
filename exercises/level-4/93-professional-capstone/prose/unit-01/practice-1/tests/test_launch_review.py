import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from launch_review import launch_review, needed_accuracy, scorecard, verdict

CLEAN_FLAGS = {"feedback_loop", "model_measured", "replace_on_change", "deferral", "protected_segment", "rollback", "human_step", "owner", "accuracy_stated", "managed_settings", "irreversible_action", "team"}
CLEAN_NUMBERS = {"team_value_chats": 15, "tool_tokens": 10000, "eval_cases": 20, "rollout_stages": 3, "retain_days": 365, "floor_days": 90, "ceiling_days": 365, "team_size": 10, "latency_ms": 2000, "availability_tenths": 995}
ALL_BAD_FLAGS = {"agent", "path_known", "volatile_prefix", "filter_after_ranking", "agent_rights_only", "pii_reaches_model", "residency_unmet", "audit_keeps_content", "irreversible_action", "team"}
ALL_BAD_NUMBERS = {"team_value_chats": 14, "tool_tokens": 10001, "eval_cases": 19, "rollout_stages": 2, "retain_days": 366, "floor_days": 90, "ceiling_days": 365, "team_size": 11, "latency_ms": 0, "availability_tenths": 0}


def review(add=(), drop=(), **numbers):
    """The clean design with some flags added or dropped and some numbers changed."""
    result = launch_review((CLEAN_FLAGS | set(add)) - set(drop), {**CLEAN_NUMBERS, **numbers})
    assert isinstance(result, list), "launch_review returned nothing"
    return result


def test_m1_a_design_that_sits_exactly_at_every_threshold_has_no_findings_and_is_approved():
    findings = review()
    assert findings == []
    assert verdict(findings) == "approve"
    assert scorecard(findings) == [0, 0, 0, 0, 0, 0, 0]
    assert needed_accuracy(250, 5) == 98


def test_e1_a_missing_feedback_loop_a_filter_after_ranking_or_no_way_back_is_a_high_finding_and_the_design_is_rejected():
    findings = review(add=["filter_after_ranking"], drop=["feedback_loop"])
    assert findings == ["high P1 missing-feedback", "high P3 filter-after-ranking"]
    assert verdict(findings) == "reject"
    assert review(drop=["rollback"]) == ["high P4 no-way-back"]


def test_e2_medium_findings_revise_the_design_and_low_findings_alone_approve_it():
    medium = review(drop=["owner"])
    assert medium == ["medium P6 no-accountable-owner"]
    assert verdict(medium) == "revise"
    low = review(drop=["model_measured"])
    assert low == ["low P2 model-not-measured"]
    assert verdict(low) == "approve"
    assert verdict(review(drop=["owner", "feedback_loop"])) == "reject"


def test_e3_findings_are_ordered_by_severity_then_domain_then_rule_id():
    findings = review(add=["volatile_prefix"], drop=["protected_segment", "accuracy_stated", "owner", "feedback_loop"], rollout_stages=2)
    assert findings == [
        "high P1 missing-feedback",
        "medium P2 volatile-prefix",
        "medium P4 big-bang-rollout",
        "medium P4 no-protected-segment",
        "medium P6 no-accountable-owner",
        "low P6 accuracy-unstated",
    ]


def test_e4_each_numeric_threshold_passes_exactly_at_its_value_and_fails_one_step_beyond():
    assert review(team_value_chats=15) == []
    assert review(team_value_chats=14) == ["medium P1 team-below-price"]
    assert review(drop=["deferral"], tool_tokens=10000) == []
    assert review(drop=["deferral"], tool_tokens=10001) == ["medium P3 tool-bloat"]
    assert review(tool_tokens=10001) == []
    assert review(eval_cases=20) == []
    assert review(eval_cases=19) == ["low P4 small-eval-set"]
    assert review(rollout_stages=3) == []
    assert review(rollout_stages=2) == ["medium P4 big-bang-rollout"]
    assert review(retain_days=90) == []
    assert review(retain_days=89) == ["medium P5 retention-outside-window"]
    assert review(retain_days=365) == []
    assert review(retain_days=366) == ["medium P5 retention-outside-window"]
    assert review(drop=["managed_settings"], team_size=10) == []
    assert review(drop=["managed_settings"], team_size=11) == ["medium P7 unmanaged-team-settings"]
    assert review(team_size=11) == []
    assert review(latency_ms=0) == ["medium P6 sla-without-numbers"]
    assert review(availability_tenths=0) == ["medium P6 sla-without-numbers"]
    assert review(latency_ms=1, availability_tenths=1) == []


def test_e5_the_scorecard_counts_the_findings_of_each_domain_from_P1_to_P7():
    findings = ["high P1 a", "medium P1 b", "low P7 c", "medium P3 d"]
    assert scorecard(findings) == [2, 0, 1, 0, 0, 0, 1]
    assert scorecard([]) == [0, 0, 0, 0, 0, 0, 0]


def test_e6_the_accuracy_a_design_needs_is_the_break_even_rounded_up_from_the_two_costs():
    assert needed_accuracy(250, 5) == 98
    assert needed_accuracy(60, 5) == 91
    assert needed_accuracy(3, 1) == 66
    assert needed_accuracy(5, 5) == 0
    assert needed_accuracy(5, 6) == 0
    assert needed_accuracy(0, 5) == 0
    assert needed_accuracy(-1, 5) == 0


def test_e7_rules_that_depend_on_a_second_fact_fire_only_when_both_hold():
    assert review(add=["path_known"]) == ["medium P1 autonomy-without-need"]
    assert review(drop=["team"], add=["path_known"]) == []
    assert review(add=["agent"]) == []
    assert review(drop=["team"], add=["agent", "path_known"]) == ["medium P1 autonomy-without-need"]
    assert review(drop=["human_step"]) == ["high P5 irreversible-without-person"]
    assert review(drop=["human_step", "irreversible_action"]) == []
    assert review(drop=["team"], team_value_chats=3) == []


def test_e8_retrieval_and_privacy_flaws_are_high_findings_in_domains_P3_and_P5():
    assert review(add=["filter_after_ranking", "agent_rights_only"], drop=["replace_on_change"]) == ["high P3 agent-rights-only", "high P3 filter-after-ranking", "high P3 stale-index"]
    assert review(add=["pii_reaches_model", "residency_unmet", "audit_keeps_content"]) == ["high P5 identifiers-reach-model", "high P5 residency-unmet", "medium P5 audit-keeps-content"]


def test_e9_a_design_with_every_flaw_gets_all_22_findings_and_a_scorecard_that_adds_up():
    findings = launch_review(ALL_BAD_FLAGS, ALL_BAD_NUMBERS)
    assert len(findings) == 22
    assert findings[0] == "high P1 missing-feedback"
    assert findings[-1] == "low P6 accuracy-unstated"
    assert verdict(findings) == "reject"
    assert scorecard(findings) == [3, 2, 4, 4, 5, 3, 1]
