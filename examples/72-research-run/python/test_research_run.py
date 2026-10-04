from research_run import REQUIRED, coverage, recover, replan, report, research, search, verify_fact

NARROW = [{"scope": "visual arts", "query": q} for q in ("AI in digital art", "AI in graphic design", "AI in photography")]


def test_the_coordinator_finds_the_scopes_its_plan_leaves_out_and_adds_them_once():
    assert coverage(NARROW) == (["visual arts"], ["music", "writing", "film"])
    plan = replan(NARROW)
    assert coverage(plan) == (REQUIRED, []) and len(plan) == 6 and replan(plan) == plan


def test_a_failed_search_returns_its_type_query_partial_results_and_alternatives():
    error = search("AI in film")["error"]
    assert (error["type"], error["query"], error["partial"], error["alternatives"]) == ("timeout", "AI in film", [], ["AI in film production"])


def test_one_retry_uses_the_alternative_and_a_second_failure_keeps_both_queries():
    done = recover(search("AI in film"), ())
    assert done["status"] == "ok" and done["recovered_from"] == "AI in film"
    still = recover(search("AI in film", ("AI in film", "AI in film production")), ("AI in film", "AI in film production"))
    assert still["status"] == "error" and still["error"]["tried"] == ["AI in film", "AI in film production"]


def test_a_failure_with_no_alternative_is_returned_as_it_is_and_never_as_success():
    result = recover(search("AI in music", ("AI in music",)), ("AI in music",))
    assert result["status"] == "error" and result["error"]["alternatives"] == []


def test_the_report_is_partial_whenever_a_scope_is_not_covered_and_says_which():
    plan = replan(NARROW)
    assert report(research(plan))["status"] == "complete"
    partial = report(research(plan, ("AI in film", "AI in film production")))
    assert (partial["status"], partial["covered"], partial["errors"]) == ("partial", 3, 1)
    assert partial["notes"] == ["film not covered: timeout on 'AI in film' and on 'AI in film production'"]
    assert report(research(NARROW))["status"] == "partial"


def test_the_scoped_verification_tool_checks_dates_here_and_sends_the_rest_back():
    assert verify_fact("date", "survey-a", "2025-02-01") == "confirmed" and verify_fact("date", "survey-a", "2024-01-01") == "mismatch"
    assert verify_fact("statistic", "survey-a", "60%") == "needs_search"
