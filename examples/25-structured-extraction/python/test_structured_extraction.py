from harness import scripted_client
from harness.scripted import message, text
from structured_extraction import DOC, LOCAL_SCHEMA, MODEL, REPLIES, body, extract, for_api, normalise_enum, problems


def test_the_api_schema_has_no_numeric_constraint_and_keeps_it_in_the_description():
    sent = for_api(LOCAL_SCHEMA)
    assert "minimum" not in sent["properties"]["total"] and "minimum 0" in sent["properties"]["total"]["description"]
    assert LOCAL_SCHEMA["properties"]["total"]["minimum"] == 0


def test_a_value_below_the_minimum_is_rejected_by_the_program():
    import json
    assert "$.total: must be a number of at least 0" in problems(json.loads(body(total=-5)), DOC)


def test_enum_capitalisation_is_normalised_before_the_enum_check():
    assert normalise_enum({"currency": "Eur"})["currency"] == "EUR"
    assert normalise_enum({"currency": "XYZ"})["currency"] == "XYZ"


def test_an_invented_quotation_is_sent_back_and_the_second_reply_is_accepted():
    client, transport = scripted_client(*REPLIES[1:3])
    result = extract(client, DOC)
    assert result["status"] == "ok" and result["attempts"] == 2
    assert [m["role"] for m in transport.requests[1]["messages"]] == ["user", "assistant", "user"]


def test_a_refusal_and_a_cut_off_reply_are_not_retried():
    client, transport = scripted_client(*REPLIES[3:5])
    assert extract(client, DOC)["status"] == "refused" and extract(client, DOC)["status"] == "truncated"
    assert len(transport.requests) == 2
