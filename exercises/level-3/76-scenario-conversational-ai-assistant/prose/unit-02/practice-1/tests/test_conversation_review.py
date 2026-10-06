import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from conversation_review import review

POLICY = {"max_turns": 12, "max_repeat": 10, "min_resolved": 80, "min_n": 3}


def conv(segment="billing", turns=5, resolved=True, handoff="none", needed=False, repeated=False, risk=False):
    return {"id": "c", "segment": segment, "turns": turns, "resolved": resolved, "handoff": handoff, "needed_person": needed, "repeated": repeated, "risk": risk}


def many(n, **fields):
    return [conv(**fields) for _ in range(n)]


def seg(segment, n, resolved, percent, weak):
    return {"segment": segment, "n": n, "resolved": resolved, "percent": percent, "weak": weak}


def test_m1_a_mixed_batch_gets_every_count_the_segments_and_a_verdict():
    batch = many(3, segment="billing") + [conv("billing", resolved=False, handoff="requested", needed=True), conv("smalltalk"), conv("smalltalk", turns=15),
                                           conv("safety", resolved=False, handoff="safety", needed=True, risk=True), conv("billing", repeated=True)]
    assert review(batch, POLICY) == {
        "n": 8, "resolved": 6, "resolved_pct": 75, "safety_missed": 0, "overlong": 1, "repeat_pct": 13, "repeat_ok": False, "over_escalated": 0,
        "under_escalated": 0, "segments": [seg("billing", 5, 4, 80, False), seg("safety", 1, 0, 0, False), seg("smalltalk", 2, 2, 100, False)],
        "verdict": "hold", "reason": "repeats"}


def test_e1_an_empty_batch_has_zero_figures_and_is_held_for_lack_of_data():
    assert review([], POLICY) == {
        "n": 0, "resolved": 0, "resolved_pct": 0, "safety_missed": 0, "overlong": 0, "repeat_pct": 0, "repeat_ok": True, "over_escalated": 0,
        "under_escalated": 0, "segments": [], "verdict": "hold", "reason": "no_data"}


def test_e2_a_safety_signal_counts_as_missed_unless_it_went_to_a_person_as_a_safety_hand_off():
    batch = many(5) + [conv("safety", resolved=False, handoff="requested", needed=True, risk=True), conv("safety", resolved=False, handoff="none", risk=True),
                       conv("safety", resolved=False, handoff="safety", needed=True, risk=True)]
    report = review(batch, POLICY)
    assert report["safety_missed"] == 2 and report["verdict"] == "hold" and report["reason"] == "safety"
    assert review(many(5) + [conv("safety", resolved=False, handoff="safety", needed=True, risk=True)], POLICY)["safety_missed"] == 0


def test_e3_a_conversation_is_overlong_only_above_the_turn_limit_and_only_when_nobody_took_over():
    assert review([conv(turns=12)], POLICY)["overlong"] == 0
    assert review([conv(turns=13)], POLICY)["overlong"] == 1
    assert review([conv(turns=30, resolved=False, handoff="stalled", needed=True)], POLICY)["overlong"] == 0


def test_e4_repeated_questions_are_acceptable_at_exactly_the_limit_and_not_above_it():
    at = review(many(9) + many(1, repeated=True), POLICY)
    assert (at["repeat_pct"], at["repeat_ok"], at["verdict"]) == (10, True, "ship")
    over = review(many(8) + many(2, repeated=True), POLICY)
    assert (over["repeat_pct"], over["repeat_ok"], over["verdict"], over["reason"]) == (20, False, "hold", "repeats")


def test_e5_a_segment_is_weak_only_below_the_resolution_floor_and_not_at_it():
    at = review(many(8, segment="billing") + many(2, segment="billing", resolved=False, handoff="requested", needed=True), POLICY)
    assert at["segments"] == [seg("billing", 10, 8, 80, False)] and at["reason"] == "none"
    below = review(many(7, segment="billing") + many(3, segment="billing", resolved=False, handoff="requested", needed=True), POLICY)
    assert below["segments"] == [seg("billing", 10, 7, 70, True)] and below["reason"] == "weak_segment"


def test_e6_a_segment_needs_the_minimum_number_of_conversations_before_it_can_be_called_weak():
    two = review(many(2, segment="refunds", resolved=False, handoff="requested", needed=True), POLICY)
    assert two["segments"] == [seg("refunds", 2, 0, 0, False)]
    three = review(many(3, segment="refunds", resolved=False, handoff="requested", needed=True), POLICY)
    assert three["segments"] == [seg("refunds", 3, 0, 0, True)] and three["reason"] == "weak_segment"


def test_e7_a_hand_off_is_over_escalation_only_when_no_person_was_needed_and_no_safety_signal_was_present():
    batch = [conv(handoff="requested"), conv(handoff="requested", needed=True), conv(handoff="safety", risk=True), conv(handoff="none", needed=True),
             conv(handoff="stalled", resolved=False)]
    report = review(batch, POLICY)
    assert (report["over_escalated"], report["under_escalated"]) == (2, 1)


def test_e8_only_conversations_the_assistant_settled_alone_count_as_resolved():
    report = review([conv(), conv(handoff="requested", needed=True), conv(resolved=False)], POLICY)
    assert (report["resolved"], report["resolved_pct"]) == (1, 33)


def test_e9_percentages_are_whole_numbers_rounded_half_up():
    report = review(many(1) + many(7, resolved=False, handoff="requested", needed=True), POLICY)
    assert (report["resolved_pct"], report["segments"][0]["percent"]) == (13, 13)
    assert review(many(2) + many(1, resolved=False, handoff="requested", needed=True), POLICY)["resolved_pct"] == 67
