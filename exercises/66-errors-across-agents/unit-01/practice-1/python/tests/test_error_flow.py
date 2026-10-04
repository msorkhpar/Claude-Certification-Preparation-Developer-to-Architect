import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
import error_flow as _solution


def _got(fn):
    def call(*args, **kwargs):
        value = fn(*args, **kwargs)
        assert value is not None, f"{fn.__name__} returned nothing"
        return value

    return call


search_with_recovery, coordinator_plan, coverage_note = (_got(getattr(_solution, n)) for n in ("search_with_recovery", "coordinator_plan", "coverage_note"))


def ok(*items):
    return {"status": "ok", "items": list(items)}


def err(kind, *partial):
    return {"status": "error", "type": kind, "partial": list(partial)}


def scripted(*replies):
    calls = []

    def call(query, attempt):
        calls.append((query, attempt))
        return replies[min(attempt, len(replies)) - 1]

    return call, calls


def failed(kind, attempts, partial=(), alternatives=(), query="q"):
    return {"status": "failed", "failure_type": kind, "attempted": query, "attempts": attempts, "partial_results": list(partial), "alternatives": list(alternatives)}


def test_m1_a_transient_failure_is_retried_locally_and_the_success_is_reported_with_its_attempts():
    call, calls = scripted(err("timeout"), ok("a", "b"))
    assert search_with_recovery("q", call) == {"status": "success", "items": ["a", "b"], "attempts": 2}
    assert calls == [("q", 1), ("q", 2)]


def test_e1_a_valid_empty_result_is_a_success_with_no_findings_and_never_an_error():
    call, calls = scripted(ok())
    assert search_with_recovery("q", call) == {"status": "empty", "items": [], "attempts": 1}
    assert len(calls) == 1


def test_e2_a_permission_or_invalid_query_error_is_not_retried_and_carries_what_was_attempted_and_its_alternatives():
    call, calls = scripted(err("permission"), ok("never reached"))
    assert search_with_recovery("q", call) == failed("permission", 1, alternatives=["request access", "use a public source"])
    assert len(calls) == 1
    call, calls = scripted(err("invalid_query"))
    assert search_with_recovery("q", call) == failed("invalid_query", 1, alternatives=["rewrite the query"])
    call, _ = scripted(err("weird"))
    assert search_with_recovery("q", call)["alternatives"] == []


def test_e3_a_failure_that_survives_the_retries_carries_the_partial_results_of_the_last_attempt():
    call, calls = scripted(err("timeout", "x"), err("timeout", "x", "y"))
    assert search_with_recovery("q", call) == failed("timeout", 2, partial=["x", "y"], alternatives=["retry later", "try a narrower query"])
    call, calls = scripted(err("unavailable"))
    assert search_with_recovery("q", call, max_attempts=3)["attempts"] == 3 and len(calls) == 3


def test_e4_the_coordinator_uses_partial_results_tries_an_alternative_or_flags_a_gap_and_never_stops_the_run():
    results = {
        "a": {"status": "success", "items": ["x"], "attempts": 1},
        "b": {"status": "empty", "items": [], "attempts": 1},
        "c": failed("timeout", 2, partial=["p"], alternatives=["retry later"]),
        "d": failed("unavailable", 2, alternatives=["use a cached source"]),
        "e": failed("weird", 1),
    }
    assert coordinator_plan(results) == [("a", "use"), ("b", "no_findings"), ("c", "use_partial"), ("d", "try_alternative"), ("e", "flag_gap")]
    assert coordinator_plan({}) == []


def test_e5_the_coverage_note_separates_supported_topics_from_gaps_and_names_the_cause():
    results = {
        "news": {"status": "success", "items": ["x"], "attempts": 1},
        "patents": {"status": "empty", "items": [], "attempts": 1},
        "papers": failed("timeout", 2, partial=["p"], query="q-papers"),
        "filings": failed("permission", 1, query="q-filings"),
    }
    assert coverage_note(results, ["news", "papers", "patents", "filings"]) == "Well-supported: news\nPartial: papers (timeout)\nNo findings: patents\nGaps: filings (permission: q-filings)"
    assert coverage_note({"news": results["news"]}, ["news"]) == "Well-supported: news"


def test_e6_a_topic_with_no_result_is_a_gap_that_was_not_searched():
    results = {"news": {"status": "success", "items": ["x"], "attempts": 1}}
    assert coverage_note(results, ["news", "blogs"]) == "Well-supported: news\nGaps: blogs (not searched)"
    assert coverage_note({}, []) == ""
