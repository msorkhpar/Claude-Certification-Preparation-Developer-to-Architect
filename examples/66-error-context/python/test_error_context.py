from error_context import OUTCOMES, generic, structured, suppress, terminate


def test_the_generic_status_loses_the_partial_result_and_the_cause():
    assert generic(OUTCOMES) == "found n1, n2, b1; sources unavailable: papers, filings"


def test_silent_suppression_reports_a_failed_source_as_a_search_that_found_nothing():
    assert suppress(OUTCOMES) == "found n1, n2, b1; nothing found in: papers, patents, filings"


def test_aborting_on_the_first_failure_loses_every_later_source():
    assert terminate(OUTCOMES) == "aborted at papers; found n1, n2"
    assert terminate({"a": ("ok", ["x"])}) == "found x"


def test_structured_context_keeps_the_partial_result_the_empty_answer_and_the_way_forward():
    assert structured(OUTCOMES) == "well supported: news, blogs; partial: papers (timeout, kept p1); no findings: patents; gaps: filings (permission, try: request access)"
