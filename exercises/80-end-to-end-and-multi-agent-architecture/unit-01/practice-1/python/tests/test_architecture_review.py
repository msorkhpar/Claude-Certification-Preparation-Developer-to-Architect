import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from architecture_review import cheapest_adequate as _cheapest_adequate
from architecture_review import review as _review
from architecture_review import verdict as _verdict


def review(design):
    result = _review(design)
    assert isinstance(result, list), "review returned nothing"
    return result


def verdict(findings):
    result = _verdict(findings)
    assert isinstance(result, str), "verdict returned nothing"
    return result


def design(**over):
    base = {"name": "intake", "pattern": "workflow", "agents": 1, "cost": 3, "path_known": True, "parallel_independent": False, "shared_context": False,
            "needs_audit": True, "writes_without_approval": False,
            "stages": {"input": ["parse"], "processing": ["classify", "route"], "output": ["validate", "send"], "feedback": ["review a sample"]}}
    base.update(over)
    return base


def stages(**over):
    base = design()["stages"]
    base.update(over)
    return base


def rules(d):
    return [f["rule"] for f in review(d)]


def test_m1_a_sound_design_passes_review_with_no_findings():
    assert review(design()) == []
    assert verdict([]) == "approve"


def test_e1_a_design_without_a_feedback_loop_or_a_stage_is_rejected():
    assert review(design(stages=stages(feedback=[]))) == [{"rule": "no-feedback", "severity": "high"}]
    assert rules(design(stages={k: v for k, v in stages().items() if k != "feedback"})) == ["no-feedback"]
    for stage in ("input", "processing"):
        assert rules(design(stages=stages(**{stage: []}))) == [f"missing-stage:{stage}"]
    assert rules(design(stages=stages(output=[]))) == ["missing-stage:output"]


def test_e2_autonomy_is_flagged_only_when_the_path_is_known():
    assert review(design(pattern="agent")) == [{"rule": "autonomy-without-need", "severity": "medium"}]
    assert rules(design(pattern="multi-agent", agents=1)) == ["autonomy-without-need"]
    assert rules(design(pattern="agent", path_known=False)) == []
    assert rules(design(pattern="augmented")) == []


def test_e3_several_agents_need_independent_parts_and_no_shared_context():
    assert review(design(pattern="multi-agent", path_known=False, agents=3, shared_context=True, parallel_independent=True)) == [{"rule": "team-without-independence", "severity": "high"}]
    assert rules(design(pattern="multi-agent", path_known=False, agents=3, parallel_independent=False)) == ["team-without-independence"]
    assert rules(design(pattern="multi-agent", path_known=False, agents=3, parallel_independent=True)) == []
    assert rules(design(pattern="agent", path_known=False, agents=1, shared_context=True)) == []


def test_e4_an_unapproved_write_is_a_finding_only_when_an_audit_is_needed():
    assert review(design(writes_without_approval=True)) == [{"rule": "unapproved-write", "severity": "high"}]
    assert rules(design(writes_without_approval=True, needs_audit=False)) == []
    assert rules(design(writes_without_approval=False)) == []


def test_e5_output_that_nobody_validates_is_flagged():
    assert review(design(stages=stages(output=["send"]))) == [{"rule": "unvalidated-output", "severity": "medium"}]
    assert rules(design(stages=stages(output=["validate"]))) == []


def test_e6_findings_are_ordered_by_severity_then_rule_and_the_verdict_follows_the_worst():
    messy = design(pattern="agent", writes_without_approval=True, stages={"input": ["parse"], "processing": ["act"], "output": ["send"], "feedback": []})
    assert [(f["severity"], f["rule"]) for f in review(messy)] == [("high", "no-feedback"), ("high", "unapproved-write"), ("medium", "autonomy-without-need"), ("medium", "unvalidated-output")]
    assert verdict(review(messy)) == "reject"
    assert verdict(review(design(pattern="agent"))) == "revise"
    assert verdict([{"rule": "x", "severity": "medium"}, {"rule": "y", "severity": "high"}]) == "reject"
    assert verdict([{"rule": "x", "severity": "medium"}]) == "revise"


def test_e7_the_cheapest_design_that_is_not_rejected_wins_and_ties_go_by_name():
    def pick(designs):
        result = _cheapest_adequate(designs)
        assert result is None or isinstance(result, str), "cheapest_adequate returned something that is not a name"
        return result

    cheap_but_rejected = design(name="a-cheap", cost=1, stages=stages(feedback=[]))
    revise = design(name="b-revise", cost=2, pattern="agent")
    sound = design(name="c-sound", cost=5)
    assert pick([cheap_but_rejected, sound, revise]) == "b-revise"
    assert pick([design(name="zeta", cost=2), design(name="alpha", cost=2), design(name="mid", cost=4)]) == "alpha"
    assert pick([cheap_but_rejected]) is None
    assert pick([]) is None
