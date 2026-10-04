from extraction_run import DOCS, REPLIES, extract, validate


def test_a_record_that_is_fine_is_valid_on_the_first_attempt():
    result = extract("d1", DOCS["d1"][1])
    assert (result["status"], result["attempts"], result["retried"]) == ("valid", 1, [])


def test_a_semantic_error_and_an_invented_vendor_are_retried_once_and_then_fixed():
    assert validate(REPLIES["d2"][0], DOCS["d2"][1]) == [("semantic", "total")]
    assert validate(REPLIES["d3"][0], DOCS["d3"][1]) == [("ungrounded", "vendor")]
    for doc in ("d2", "d3"):
        result = extract(doc, DOCS[doc][1])
        assert (result["status"], result["attempts"]) == ("valid", 2)


def test_an_absent_value_is_never_retried_and_goes_to_review():
    result = extract("d4", DOCS["d4"][1])
    assert (result["status"], result["attempts"], result["errors"]) == ("needs_review", 1, [("absent", "total")])


def test_a_flagged_conflict_is_information_and_goes_to_review_without_a_retry():
    result = extract("d5", DOCS["d5"][1])
    assert validate(result["record"], DOCS["d5"][1]) == [] and (result["status"], result["attempts"]) == ("needs_review", 1)


def test_an_error_that_survives_the_retry_fails_the_document_after_two_attempts():
    result = extract("d6", DOCS["d6"][1])
    assert (result["status"], result["attempts"], result["retried"]) == ("failed", 2, ["ungrounded"])
