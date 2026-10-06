import copy
import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from extraction import accuracy, extract_document, merge_chunks, request_choice, validate

DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 120.50 EUR\nThank you."
DOC_NO_VENDOR = "Items: 10.00\nTotal due: 10.00 USD"
GOOD = {"vendor": "Acme Tools", "currency": "EUR", "currency_detail": None, "line_items": [100.0, 20.5], "stated_total": 120.5, "calculated_total": 120.5, "conflict_detected": False,
        "provenance": {"vendor": "Invoice from Acme Tools", "currency": "120.50 EUR", "stated_total": "Total due: 120.50 EUR"}}


def record(**over):
    r = copy.deepcopy(GOOD)
    r.update(over)
    return r


def scripted(*records):
    calls = []

    def call_model(document, feedback):
        calls.append({"document": document, "feedback": copy.deepcopy(feedback)})
        return copy.deepcopy(records[min(len(calls) - 1, len(records) - 1)])

    return call_model, calls


def run(document, *records, **kw):
    call_model, calls = scripted(*records)
    result = extract_document(document, call_model, **kw)
    assert isinstance(result, dict), "extract_document returned nothing"
    return result, calls


def kinds(errors):
    assert isinstance(errors, list), "validate returned nothing"
    return [(e["kind"], e["field"]) for e in errors]


def test_m1_a_document_with_every_value_present_and_quoted_comes_back_valid_on_the_first_attempt():
    result, calls = run(DOC, GOOD)
    assert result == {"status": "valid", "record": GOOD, "attempts": 1, "errors": []}
    assert calls == [{"document": DOC, "feedback": None}]


def test_e1_a_value_the_document_does_not_give_is_null_and_needs_no_quote_while_an_invented_value_fails_as_ungrounded():
    no_vendor = record(vendor=None, currency="USD", line_items=[10.0], stated_total=10.0, calculated_total=10.0, provenance={"currency": "10.00 USD", "stated_total": "Total due: 10.00 USD"})
    assert kinds(validate(no_vendor, DOC_NO_VENDOR)) == []
    invented = record(vendor="Acme Tools", currency="USD", line_items=[10.0], stated_total=10.0, calculated_total=10.0, provenance={"vendor": "Invoice from Acme Tools", "currency": "10.00 USD", "stated_total": "Total due: 10.00 USD"})
    assert kinds(validate(invented, DOC_NO_VENDOR)) == [("ungrounded", "vendor")]
    unquoted = record(provenance={"vendor": "Invoice from Acme Tools", "currency": "120.50 EUR"})
    assert kinds(validate(unquoted, DOC)) == [("ungrounded", "stated_total")]
    assert kinds(validate(record(provenance={"vendor": "", "currency": "120.50 EUR", "stated_total": "Total due: 120.50 EUR"}), DOC)) == [("ungrounded", "vendor")]


def test_e2_a_retry_carries_the_original_document_the_failed_record_and_only_the_errors_a_second_look_can_fix():
    bad = record(vendor="Acme Corp", provenance={"vendor": "Invoice from Acme Corp", "currency": "120.50 EUR", "stated_total": "Total due: 120.50 EUR"}, currency_detail=None)
    result, calls = run(DOC, bad, GOOD, required=("vendor", "currency"))
    assert result["status"] == "valid" and result["attempts"] == 2 and len(calls) == 2
    assert calls[0] == {"document": DOC, "feedback": None}
    assert calls[1]["document"] == DOC
    assert calls[1]["feedback"]["previous"] == bad
    assert [(e["kind"], e["field"]) for e in calls[1]["feedback"]["errors"]] == [("ungrounded", "vendor")]
    assert all(e["message"] for e in calls[1]["feedback"]["errors"])
    mixed = record(vendor="Acme Corp", currency="unclear", provenance={"vendor": "Invoice from Acme Corp", "stated_total": "Total due: 120.50 EUR"})
    result, calls = run(DOC, mixed, GOOD, required=("currency",))
    assert [(e["kind"], e["field"]) for e in calls[1]["feedback"]["errors"]] == [("ungrounded", "vendor")], "the absent currency is not something a second look can fix"


def test_e3_a_required_value_that_the_model_reports_as_absent_is_not_retried_and_goes_to_review():
    nothing = record(stated_total=None, provenance={"vendor": "Invoice from Acme Tools", "currency": "120.50 EUR"})
    result, calls = run(DOC, nothing, GOOD, required=("stated_total",))
    assert result["status"] == "needs_review" and result["attempts"] == 1 and len(calls) == 1
    assert kinds(result["errors"]) == [("absent", "stated_total")]
    result, calls = run(DOC, nothing)
    assert result["status"] == "valid", "a value that is not required may stay null"


def test_e4_retries_stop_after_the_limit_and_the_document_is_marked_failed():
    bad = record(currency="dollars")
    result, calls = run(DOC, bad, required=())
    assert result["status"] == "failed" and result["attempts"] == 3 and len(calls) == 3 and kinds(result["errors"]) == [("syntax", "currency")]
    result, calls = run(DOC, bad, max_retries=0)
    assert result["status"] == "failed" and result["attempts"] == 1
    result, calls = run(DOC, bad, GOOD, max_retries=1)
    assert result["status"] == "valid" and result["attempts"] == 2
    assert run(DOC, {"vendor": "x"})[0]["status"] == "failed"


def test_e5_a_total_that_differs_from_the_line_items_is_a_semantic_error_and_a_flagged_conflict_goes_to_review_without_a_retry():
    wrong_sum = record(calculated_total=100.0, stated_total=100.0, provenance={"vendor": "Invoice from Acme Tools", "currency": "120.50 EUR", "stated_total": "Total due: 120.50 EUR"})
    assert kinds(validate(wrong_sum, DOC)) == [("semantic", "calculated_total")]
    unflagged = record(stated_total=130.0, provenance={"vendor": "Invoice from Acme Tools", "currency": "120.50 EUR", "stated_total": "Total due: 120.50 EUR"})
    assert kinds(validate(unflagged, DOC)) == [("semantic", "stated_total")]
    flagged = record(stated_total=130.0, conflict_detected=True)
    assert kinds(validate(flagged, DOC)) == [], "a conflict the model flagged is information, not an error"
    result, calls = run(DOC, flagged)
    assert result["status"] == "needs_review" and result["attempts"] == 1 and result["errors"] == [] and len(calls) == 1


def test_e6_currency_takes_unclear_and_other_with_a_detail_and_rejects_anything_else_as_a_syntax_error():
    no_currency = record(currency="unclear", provenance={"vendor": "Invoice from Acme Tools", "stated_total": "Total due: 120.50 EUR"})
    assert kinds(validate(no_currency, DOC)) == []
    chf = "Invoice from Acme Tools.\nTotal due: 120.50 CHF"
    other = record(currency="other", currency_detail="CHF", provenance={"vendor": "Invoice from Acme Tools", "currency": "120.50 CHF", "stated_total": "Total due: 120.50 CHF"})
    assert kinds(validate(other, chf)) == []
    assert kinds(validate({**other, "currency_detail": None}, chf)) == [("syntax", "currency_detail")]
    assert kinds(validate({**other, "currency_detail": "  "}, chf)) == [("syntax", "currency_detail")]
    assert kinds(validate(record(currency="dollars"), DOC)) == [("syntax", "currency")]
    assert kinds(validate(record(line_items="100"), DOC)) == [("syntax", "line_items")]
    missing = record()
    del missing["provenance"]
    assert kinds(validate(missing, DOC)) == [("syntax", "provenance")]


def chunk(**over):
    base = {"vendor": None, "currency": "unclear", "currency_detail": None, "line_items": [], "stated_total": None, "calculated_total": 0, "conflict_detected": False, "provenance": {}}
    base.update(over)
    return base


def test_e7_chunk_results_merge_by_keeping_the_first_value_and_recording_a_conflict_when_two_chunks_disagree():
    one = chunk(vendor="Acme Tools", line_items=[100.0], provenance={"vendor": "Invoice from Acme Tools"})
    two = chunk(currency="EUR", line_items=[20.5], stated_total=120.5, provenance={"currency": "120.50 EUR", "stated_total": "Total due: 120.50 EUR"})
    three = chunk(vendor="Acme Tool Ltd", stated_total=120.5, provenance={"vendor": "Acme Tool Ltd", "stated_total": "Total due: 120.50 EUR"})
    merged = merge_chunks([one, two, three])
    assert merged == {"vendor": "Acme Tools", "currency": "EUR", "currency_detail": None, "line_items": [100.0, 20.5], "stated_total": 120.5, "calculated_total": 120.5, "conflict_detected": True,
                      "provenance": {"vendor": "Invoice from Acme Tools", "currency": "120.50 EUR", "stated_total": "Total due: 120.50 EUR"}, "conflicts": ["vendor"]}
    clean = merge_chunks([one, two])
    assert clean["conflicts"] == [] and clean["conflict_detected"] is False and clean["vendor"] == "Acme Tools"
    assert merge_chunks([chunk(), chunk()])["vendor"] is None and merge_chunks([chunk(conflict_detected=True)])["conflict_detected"] is True


def test_e8_accuracy_counts_every_document_and_not_only_the_validated_ones():
    labels = {f"d{i}": {"vendor": f"V{i}", "stated_total": float(i)} for i in range(10)}
    results = {}
    for i in range(5):
        results[f"d{i}"] = {"status": "valid", "record": {"vendor": f"V{i}", "stated_total": float(i)}}
    results["d5"] = {"status": "valid", "record": {"vendor": "wrong", "stated_total": 5.0}}
    results["d6"] = {"status": "needs_review", "record": {"vendor": "V6", "stated_total": 6.0}}
    results["d7"] = {"status": "needs_review", "record": {"vendor": "V7", "stated_total": 7.0}}
    results["d8"] = {"status": "failed", "record": {"vendor": "V8", "stated_total": 8.0}}
    report = accuracy(results, labels)
    assert report == {"all_documents": 0.5, "validated_only": 0.83, "validated": 6, "total": 10}
    assert accuracy({}, {}) == {"all_documents": 0.0, "validated_only": 0.0, "validated": 0, "total": 0}


def test_e9_the_request_forces_a_tool_where_the_model_allows_it_and_falls_back_to_auto_with_a_reply_check_where_it_does_not():
    two = ["extract_invoice", "extract_receipt"]
    assert request_choice("claude-haiku-4-5", two) == {"tool_choice": {"type": "any"}, "strict": True, "verify_reply": False}
    assert request_choice("claude-haiku-4-5", ["extract_invoice"]) == {"tool_choice": {"type": "tool", "name": "extract_invoice"}, "strict": True, "verify_reply": False}
    assert request_choice("claude-haiku-4-5", two, forced="extract_metadata") == {"tool_choice": {"type": "tool", "name": "extract_metadata"}, "strict": True, "verify_reply": False}
    for model in ("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1"):
        assert request_choice(model, two) == {"tool_choice": {"type": "auto"}, "strict": True, "verify_reply": True}
        assert request_choice(model, two, forced="extract_metadata") == {"tool_choice": {"type": "auto"}, "strict": True, "verify_reply": True}
