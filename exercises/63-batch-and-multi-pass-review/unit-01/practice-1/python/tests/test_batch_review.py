import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from batch_review import choose_api, merge_passes, resubmission_plan, review_plan, submission_interval


def got(value):
    assert value is not None, "the function returned nothing"
    return value


def refused(fn, *args):
    try:
        fn(*args)
    except ValueError:
        return True
    return False


def finding(file="a.py", line=10, severity="medium", issue="unchecked input", confidence=90):
    return {"file": file, "line": line, "severity": severity, "issue": issue, "confidence": confidence}


def test_m1_the_interval_between_submissions_leaves_room_for_the_window_and_the_handling():
    assert submission_interval(30) == 4
    assert submission_interval(48, window_hours=24, handling_hours=4) == 20


def test_e1_an_sla_without_room_for_a_batch_is_refused():
    assert refused(submission_interval, 26) and refused(submission_interval, 20)


def test_e2_a_blocking_check_or_a_tool_loop_needs_the_synchronous_api():
    assert choose_api(blocking=True) == "synchronous"
    assert choose_api(blocking=False) == "batch"
    assert choose_api(blocking=False, needs_tool_loop=True) == "synchronous"


def test_e3_only_the_items_that_did_not_succeed_are_resubmitted_by_custom_id():
    results = [("a1", "succeeded"), ("a2", "expired"), ("a3", "succeeded"), ("a4", "canceled"), ("a5", "server_error")]
    assert resubmission_plan(results, {}, 1000) == [("a2", "resubmit"), ("a4", "resubmit"), ("a5", "resubmit")]
    assert resubmission_plan([("a1", "succeeded")], {}, 1000) == []


def test_e4_an_item_over_the_limit_is_chunked_and_a_rejected_request_is_fixed_first():
    results = [("big", "invalid_request"), ("bad", "invalid_request"), ("late", "expired"), ("bigexp", "expired")]
    sizes = {"big": 5000, "bad": 100, "late": 100, "bigexp": 5000}
    assert resubmission_plan(results, sizes, 1000) == [("big", "chunk"), ("bad", "fix"), ("late", "resubmit"), ("bigexp", "chunk")]


def test_e5_a_multi_file_review_gets_a_local_pass_per_file_and_one_integration_pass():
    plan = got(review_plan(["a.py", "b.py", "c.py"]))
    assert [p["name"] for p in plan] == ["local:a.py", "local:b.py", "local:c.py", "integration"]
    assert plan[0]["files"] == ["a.py"] and plan[-1]["files"] == ["a.py", "b.py", "c.py"]
    assert [p["name"] for p in got(review_plan(["a.py"]))] == ["local:a.py"]


def test_e6_the_same_finding_from_two_passes_is_one_finding_with_the_highest_severity_and_the_lowest_confidence():
    merged = got(merge_passes([[finding(severity="low", confidence=95)], [finding(severity="high", confidence=85), finding(file="b.py", line=3, issue="race")]]))
    assert len(merged) == 2
    first = merged[0]
    assert (first["file"], first["line"], first["severity"], first["passes"], first["confidence"]) == ("a.py", 10, "high", 2, 85)
    assert merged[1]["passes"] == 1


def test_e7_a_finding_is_accepted_only_when_two_independent_passes_agree_with_confidence():
    twice_same_pass = got(merge_passes([[finding(), finding()], []]))
    assert twice_same_pass[0]["passes"] == 1 and twice_same_pass[0]["route"] == "verify"
    lone_but_sure = got(merge_passes([[finding(confidence=99)]]))
    assert lone_but_sure[0]["route"] == "verify"
    unsure = got(merge_passes([[finding(confidence=60)], [finding(confidence=90)]]))
    assert unsure[0]["route"] == "verify"
    agreed = got(merge_passes([[finding(confidence=80)], [finding(confidence=90)]]))
    assert agreed[0]["route"] == "accept"
