import json
import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from extractor import ParseError, extract, parse_json, validate

DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business."
SCHEMA = {
    "type": "object",
    "required": ["vendor", "total", "currency", "evidence"],
    "properties": {
        "vendor": {"type": "string"},
        "total": {"type": "number", "minimum": 0},
        "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]},
        "items": {"type": "array", "items": {"type": "object", "required": ["name", "qty"],
                                              "properties": {"name": {"type": "string"}, "qty": {"type": "integer", "minimum": 1}}}},
        "evidence": {"type": "string"},
    },
    "additionalProperties": False,
}
GOOD = {"vendor": "Acme Tools", "total": 120.5, "currency": "EUR", "evidence": "Total due: 120.50 EUR"}


def reply(text, stop_reason="end_turn"):
    return {"id": "msg_illustrative", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5",
            "content": [{"type": "text", "text": text}], "stop_reason": stop_reason, "usage": {"input_tokens": 1, "output_tokens": 1}}


class Scripted:
    """A hand-written, illustrative model: returns the next reply and records the messages it was sent."""

    def __init__(self, *replies):
        self.replies, self.seen = list(replies), []

    def __call__(self, messages):
        self.seen.append(json.loads(json.dumps(messages)))
        return self.replies.pop(0)


def parsed(text):
    """The parsed value, or the string ParseError when the text holds no JSON object."""
    try:
        return parse_json(text)
    except ParseError:
        return "ParseError"


def paths(errors):
    return [e["path"] for e in (errors or [])]


def test_m1_a_valid_reply_is_returned_after_one_call():
    model = Scripted(reply(json.dumps(GOOD)))
    result = extract(model, DOC, SCHEMA, evidence_fields=["evidence"]) or {}
    assert (result.get("status"), result.get("value"), result.get("attempts"), result.get("errors")) == ("ok", GOOD, 1, [])
    assert len(model.seen) == 1
    assert model.seen[0][0]["role"] == "user" and DOC in model.seen[0][0]["content"]


def test_e1_json_is_found_in_fences_and_prose_and_bad_text_is_retried():
    fenced = "Here you go:\n```json\n" + json.dumps(GOOD) + "\n```\nHope that helps."
    assert parsed(fenced) == GOOD
    assert parsed("Sure! " + json.dumps(GOOD) + " Done.") == GOOD
    for bad in ("I cannot find an invoice.", "{not json}", "```json\n```"):
        try:
            parse_json(bad)
            raised = False
        except ParseError:
            raised = True
        assert raised, bad
    model = Scripted(reply("I think the total is 120.50"), reply(json.dumps(GOOD)))
    result = extract(model, DOC, SCHEMA) or {}
    assert (result.get("status"), result.get("attempts")) == ("ok", 2)
    assert "$" in model.seen[1][2]["content"]


def test_e2_every_schema_violation_is_listed_and_sent_back_to_the_model():
    bad = {"vendor": "Acme Tools", "total": "120.50", "currency": "usd", "extra": 1, "evidence": "x"}
    assert paths(validate(SCHEMA, bad)) == ["$.total", "$.currency", "$.extra"]
    assert paths(validate(SCHEMA, {"total": -1, "currency": "EUR"})) == ["$.vendor", "$.evidence", "$.total"]
    items = dict(GOOD, items=[{"name": "bolt", "qty": 2}, {"name": "nut", "qty": 0}, {"name": "gear"}])
    assert paths(validate(SCHEMA, items)) == ["$.items[1].qty", "$.items[2].qty"]
    assert validate(SCHEMA, GOOD) == []
    first = json.dumps(bad)
    model = Scripted(reply(first), reply(json.dumps(GOOD)))
    result = extract(model, DOC, SCHEMA) or {}
    assert (result.get("status"), result.get("attempts")) == ("ok", 2)
    again = model.seen[1]
    assert [m["role"] for m in again] == ["user", "assistant", "user"]
    assert again[1]["content"] == first
    assert all(p in again[2]["content"] for p in ("$.total", "$.currency", "$.extra"))


def test_e3_the_number_of_attempts_is_bounded():
    wrong = reply(json.dumps({"vendor": 1}))
    model = Scripted(*[wrong] * 5)
    result = extract(model, DOC, SCHEMA, max_attempts=3) or {}
    assert (result.get("status"), result.get("attempts"), result.get("value")) == ("failed", 3, None)
    assert len(model.seen) == 3
    assert "$.vendor" in paths(result.get("errors"))
    once = Scripted(*[wrong] * 5)
    assert (extract(once, DOC, SCHEMA, max_attempts=1) or {}).get("status") == "failed"
    assert len(once.seen) == 1


def test_e4_a_refusal_or_a_cut_off_reply_is_not_retried():
    refusal = Scripted(reply("I can't help with that.", "refusal"), reply(json.dumps(GOOD)))
    result = extract(refusal, DOC, SCHEMA) or {}
    assert (result.get("status"), result.get("attempts"), result.get("value")) == ("refused", 1, None)
    assert len(refusal.seen) == 1
    cut = Scripted(reply('{"vendor": "Acme', "max_tokens"), reply(json.dumps(GOOD)))
    result = extract(cut, DOC, SCHEMA) or {}
    assert (result.get("status"), result.get("attempts")) == ("truncated", 1)
    assert len(cut.seen) == 1


def test_e5_a_quote_that_is_not_in_the_document_is_rejected():
    invented = dict(GOOD, evidence="Total due: 999.00 USD")
    model = Scripted(reply(json.dumps(invented)), reply(json.dumps(GOOD)))
    result = extract(model, DOC, SCHEMA, evidence_fields=["evidence"]) or {}
    assert (result.get("status"), result.get("attempts")) == ("ok", 2)
    assert "$.evidence" in model.seen[1][2]["content"]
    unchecked = Scripted(reply(json.dumps(invented)))
    assert (extract(unchecked, DOC, SCHEMA) or {}).get("status") == "ok"


def test_e6_types_are_exact_booleans_are_not_numbers_and_integers_have_no_fraction():
    assert paths(validate({"type": "integer"}, True)) == ["$"]
    assert paths(validate({"type": "number"}, False)) == ["$"]
    assert paths(validate({"type": "integer"}, 2.5)) == ["$"]
    assert validate({"type": "integer"}, 2.0) == []
    assert validate({"type": "number"}, 3) == []
    assert paths(validate({"type": "string"}, None)) == ["$"]
    assert validate({"type": "null"}, None) == []
    assert paths(validate({"type": "array", "items": {"type": "boolean"}}, [True, 1])) == ["$[1]"]
