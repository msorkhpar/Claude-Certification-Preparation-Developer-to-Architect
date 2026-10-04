from extraction_checks import DOC, accuracy, check, extract, request_choice, scripted_value

WRONG = {"items": [100.0, 20.5], "total": 130.0, "evidence": "Total due: 130.00 EUR"}
RIGHT = {"items": [100.0, 20.5, 9.5], "total": 130.0, "evidence": "Total due: 130.00 EUR"}


def test_a_required_field_gets_filled_and_a_nullable_one_stays_null():
    assert scripted_value("no order here", False) == "PO-0000" and scripted_value("no order here", True) is None
    assert scripted_value("Order PO 4471 shipped", False) == "4471"


def test_the_checks_find_a_wrong_sum_and_a_quotation_that_is_not_in_the_document():
    assert check(RIGHT, DOC) == []
    assert check(WRONG, DOC) == ["total: the items add up to 120.5, not 130.0"]
    assert check({**RIGHT, "evidence": "Total due: 130.00 USD"}, DOC) == ["evidence: this quotation is not in the document"]


def test_a_retry_carries_the_document_the_failed_answer_and_the_problems():
    result = extract(DOC, [WRONG, RIGHT])
    assert result["status"] == "valid" and result["attempts"] == 2 and len(result["feedback"]) == 1
    text = result["feedback"][0]
    assert DOC in text and '"total": 130.0' in text and "- total: the items add up to 120.5, not 130.0" in text
    assert extract(DOC, [WRONG, WRONG], max_retries=1)["status"] == "failed" and extract(DOC, [WRONG, RIGHT], max_retries=0)["status"] == "failed"


def test_accuracy_on_validated_records_alone_hides_the_failures():
    assert accuracy([("valid", True)] * 5 + [("valid", False)] + [("failed", False)] * 4) == {"validated_only": 0.83, "all_documents": 0.5}
    assert accuracy([]) == {"validated_only": 0.0, "all_documents": 0.0}


def test_forced_choice_is_used_where_accepted_and_auto_with_a_reply_check_elsewhere():
    two = ["a", "b"]
    assert request_choice("claude-haiku-4-5", two) == {"tool_choice": "any", "check_reply": False}
    assert request_choice("claude-haiku-4-5", ["a"]) == {"tool_choice": "tool:a", "check_reply": False}
    assert request_choice("claude-opus-5-5", two) == {"tool_choice": "auto", "check_reply": True}
