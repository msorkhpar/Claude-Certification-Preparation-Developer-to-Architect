import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from synthesis import synthesize


def f(claim, value, source, date="2025-01-01"):
    return {"claim": claim, "value": value, "source": source, "date": date}


def ok(scope, *findings):
    return {"scope": scope, "status": "ok", "findings": list(findings), "error": None}


def err(scope, query, partial=(), alternatives=(), kind="timeout"):
    return {"scope": scope, "status": "error", "findings": [], "error": {"type": kind, "query": query, "partial": list(partial), "alternatives": list(alternatives)}}


def src(source, date="2025-01-01"):
    return {"source": source, "date": date}


def test_m1_a_full_run_with_agreement_returns_claims_with_every_source_and_a_complete_status():
    report = synthesize(["a", "b"], [ok("a", f("X", "1", "s1"), f("Y", "2", "s2")), ok("b", f("X", "1", "s3"))])
    assert report == {"status": "complete", "covered": ["a", "b"], "gaps": [], "partial": [], "conflicts": [], "errors": [], "note": "all scopes covered",
                      "claims": [{"claim": "X", "value": "1", "sources": [src("s1"), src("s3")], "partial": False}, {"claim": "Y", "value": "2", "sources": [src("s2")], "partial": False}]}


def test_e1_no_results_leave_every_scope_a_gap_and_say_not_researched():
    report = synthesize(["a", "b"], [])
    assert (report["status"], report["covered"], report["gaps"], report["claims"]) == ("partial", [], ["a", "b"], [])
    assert report["note"] == "not covered: a (not researched), b (not researched)"


def test_e2_a_scope_the_plan_never_covered_and_a_scope_whose_search_failed_read_differently_in_the_note():
    report = synthesize(["a", "b", "c"], [ok("a", f("X", "1", "s1")), err("b", "query b", alternatives=["query b2"])])
    assert report["status"] == "partial" and report["gaps"] == ["b", "c"]
    assert report["note"] == "not covered: b (timeout on 'query b'), c (not researched)"
    assert report["errors"] == [{"scope": "b", "type": "timeout", "query": "query b", "alternatives": ["query b2"]}]


def test_e3_two_values_for_one_claim_are_a_conflict_that_names_both_sources_and_no_claim():
    report = synthesize(["a", "b"], [ok("a", f("X", "12", "s1", "2024-03-01")), ok("b", f("X", "14", "s2", "2025-01-15"), f("X", "14", "s2", "2025-01-15"))])
    assert report["claims"] == []
    assert report["conflicts"] == [{"claim": "X", "values": [{"value": "12", "source": "s1", "date": "2024-03-01"}, {"value": "14", "source": "s2", "date": "2025-01-15"}]}]
    assert report["status"] == "complete"


def test_e4_partial_results_are_kept_and_flagged_but_do_not_cover_the_scope_and_a_retry_that_worked_clears_the_error():
    report = synthesize(["a", "b"], [ok("a", f("X", "1", "s1")), err("b", "qb", partial=[f("Z", "9", "s9")], alternatives=["qb2"])])
    assert report["claims"] == [{"claim": "X", "value": "1", "sources": [src("s1")], "partial": False}, {"claim": "Z", "value": "9", "sources": [src("s9")], "partial": True}]
    assert (report["covered"], report["gaps"], report["partial"]) == (["a"], ["b"], ["b"])
    healed = synthesize(["a", "b"], [ok("a", f("X", "1", "s1")), err("b", "qb", alternatives=["qb2"]), ok("b", f("Z", "9", "s9"))])
    assert healed["errors"] == [] and healed["status"] == "complete" and healed["covered"] == ["a", "b"]
    mixed = synthesize(["a"], [err("a", "qa", partial=[f("X", "1", "s1")]), ok("a", f("X", "1", "s2"))])
    assert mixed["claims"][0]["partial"] is False


def test_e5_a_result_with_no_findings_does_not_cover_its_scope():
    report = synthesize(["a", "b"], [ok("a", f("X", "1", "s1")), ok("b")])
    assert (report["covered"], report["gaps"], report["status"]) == (["a"], ["b"], "partial")
    assert report["note"] == "not covered: b (no findings)"


def test_e6_claims_sources_and_scopes_come_out_in_a_fixed_order_without_duplicates():
    report = synthesize(["b", "a"], [ok("a", f("Y", "2", "s2"), f("X", "1", "s9")), ok("b", f("X", "1", "s1"), f("X", "1", "s9"))])
    assert report["covered"] == ["b", "a"]
    assert [c["claim"] for c in report["claims"]] == ["X", "Y"]
    assert report["claims"][0]["sources"] == [src("s9"), src("s1")]
