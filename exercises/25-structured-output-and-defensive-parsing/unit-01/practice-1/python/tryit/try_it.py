"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

import json

from extractor import extract

DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business."
SCHEMA = {
    "type": "object",
    "required": ["vendor", "total", "currency", "evidence"],
    "properties": {
        "vendor": {"type": "string"},
        "total": {"type": "number", "minimum": 0},
        "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]},
        "evidence": {"type": "string"},
    },
    "additionalProperties": False,
}
GOOD = {"vendor": "Acme Tools", "total": 120.5, "currency": "EUR", "evidence": "Total due: 120.50 EUR"}


def ask(messages):
    """A hand-written stand-in for the model, in the shape of a Messages API reply: it always answers with valid JSON."""
    return {"id": "msg_illustrative", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5",
            "content": [{"type": "text", "text": json.dumps(GOOD)}], "stop_reason": "end_turn",
            "usage": {"input_tokens": 1, "output_tokens": 1}}


result = extract(ask, DOC, SCHEMA, evidence_fields=["evidence"]) or {}

print("status:", result.get("status"))
print("attempts:", result.get("attempts"))
print("value:", result.get("value"))
print("errors:", result.get("errors"))
