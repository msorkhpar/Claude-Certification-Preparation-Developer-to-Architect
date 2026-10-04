import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
import escalation as _solution


def _got(fn):
    def call(*args, **kwargs):
        value = fn(*args, **kwargs)
        assert value is not None, f"{fn.__name__} returned nothing"
        return value

    return call


clarifying_fields, decide, handoff_text = (_got(getattr(_solution, n)) for n in ("clarifying_fields", "decide", "handoff_text"))


def refused(fn, *args):
    try:
        fn(*args)
    except ValueError:
        return True
    return False


def action(case, **kw):
    return decide(case, **kw)["action"]


def test_m1_a_customer_who_asks_for_a_person_is_escalated_at_once_even_when_the_agent_could_resolve_it():
    result = decide({"asked_for_person": True, "matches": 1, "policy_covers": True, "attempts_without_progress": 0, "sentiment": "calm", "confidence": 95})
    assert result == {"action": "escalate", "reason": "customer asked for a person", "acknowledge": False}


def test_e1_frustration_alone_does_not_escalate_and_the_reply_acknowledges_it():
    result = decide({"sentiment": "frustrated"})
    assert result == {"action": "resolve", "reason": "within capability", "acknowledge": True}
    assert decide({"sentiment": "calm"})["acknowledge"] is False


def test_e2_a_request_the_policy_does_not_cover_is_escalated_and_a_covered_one_is_resolved():
    assert decide({"policy_covers": False}) == {"action": "escalate", "reason": "policy does not cover the request", "acknowledge": False}
    assert action({"policy_covers": True}) == "resolve"


def test_e3_several_matching_records_need_a_clarifying_question_and_never_a_guess():
    assert decide({"matches": 3}) == {"action": "clarify", "reason": "ambiguous customer match", "acknowledge": False}
    assert action({"matches": 1}) == "resolve" and action({"matches": 0}) == "resolve"


def test_e4_an_explicit_request_for_a_person_outranks_an_ambiguous_match():
    assert action({"asked_for_person": True, "matches": 4}) == "escalate"
    assert action({"asked_for_person": True, "policy_covers": False}) == "escalate"


def test_e5_no_progress_after_the_attempt_limit_escalates_and_below_it_does_not():
    assert decide({"attempts_without_progress": 2}) == {"action": "escalate", "reason": "no progress", "acknowledge": False}
    assert action({"attempts_without_progress": 1}) == "resolve"
    assert action({"attempts_without_progress": 3}, max_attempts=4) == "resolve"


def test_e6_sentiment_and_confidence_scores_never_change_the_decision():
    for sentiment in ("calm", "frustrated", "angry"):
        for confidence in (5, 50, 99):
            assert action({"sentiment": sentiment, "confidence": confidence}) == "resolve"
    assert action({"sentiment": "angry", "confidence": 1, "matches": 2}) == "clarify"


def test_e7_the_clarifying_question_names_only_the_fields_that_tell_the_matches_apart():
    matches = [{"id": "c1", "name": "Ana Ruiz", "email": "ana@example.com", "zip": "10115"}, {"id": "c2", "name": "Ana Ruiz", "email": "ana.r@example.com", "zip": "10115"}]
    assert clarifying_fields(matches) == ["email"]
    assert clarifying_fields([{"id": "c1", "name": "Ana", "zip": "1"}, {"id": "c2", "name": "Bo", "zip": "2"}, {"id": "c3", "name": "Ana", "zip": "3"}]) == ["name", "zip"]
    assert clarifying_fields([{"id": "c1", "name": "Ana"}]) == []


def test_e8_the_hand_off_carries_the_structured_facts_and_no_transcript_and_refuses_a_case_without_an_id():
    case = {"customer_id": "C-77", "issue": "refund over the limit", "root_cause": "duplicate charge", "amount": "$129.50", "actions": ["verified identity", "checked order"], "recommended": "approve the refund", "transcript": "user: hello ... 40 turns ..."}
    assert handoff_text(case) == "Customer: C-77\nIssue: refund over the limit\nRoot cause: duplicate charge\nAmount: $129.50\nActions taken: verified identity; checked order\nRecommended action: approve the refund"
    assert "40 turns" not in handoff_text(case)
    assert handoff_text({"customer_id": "C-1", "issue": "late parcel"}) == "Customer: C-1\nIssue: late parcel\nRoot cause: unknown\nAmount: unknown\nActions taken: none\nRecommended action: review the case"
    assert refused(handoff_text, {"issue": "late parcel"})
