"""Structured outputs plus the checks a schema cannot make, against a scripted model.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
The API gets a schema without numeric constraints (structured outputs do not support them); the program enforces them.
"""
import logging
import copy
import json

from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
LOCAL_SCHEMA = {
    "type": "object",
    "properties": {
        "vendor": {"type": "string"},
        "total": {"type": "number", "minimum": 0},
        "currency": {"type": "string", "enum": ["USD", "EUR", "GBP"]},
        "evidence": {"type": "string"},
    },
    "required": ["vendor", "total", "currency", "evidence"],
    "additionalProperties": False,
}
UNSUPPORTED = ("minimum", "maximum", "multipleOf", "minLength", "maxLength")


def for_api(schema):
    """The schema without the constraints that structured outputs reject; they move to the field's description."""
    out = copy.deepcopy(schema)

    def walk(node):
        notes = [f"{k} {node.pop(k)}" for k in UNSUPPORTED if k in node]
        if notes:
            node["description"] = (node.get("description", "") + " (" + ", ".join(notes) + ")").strip()
        for sub in node.get("properties", {}).values():
            walk(sub)
    walk(out)
    return out


def problems(value, document):
    """What the API cannot promise: numeric limits, the enum's capital letters, and a quotation that is really in the document."""
    found = []
    if not isinstance(value.get("total"), (int, float)) or value["total"] < 0:
        found.append("$.total: must be a number of at least 0")
    if value.get("currency") not in LOCAL_SCHEMA["properties"]["currency"]["enum"]:
        found.append(f"$.currency: must be one of USD, EUR, GBP, not {value.get('currency')!r}")
    if value.get("evidence") not in document:
        found.append("$.evidence: is not found in the document")
    return found


def normalise_enum(value):
    """Structured outputs may change the capital letters of an enum value; compare without them."""
    allowed = {e.lower(): e for e in LOCAL_SCHEMA["properties"]["currency"]["enum"]}
    if isinstance(value.get("currency"), str) and value["currency"].lower() in allowed:
        value = {**value, "currency": allowed[value["currency"].lower()]}
    return value


def extract(client, document, max_attempts=2):
    messages = [{"role": "user", "content": f"Extract the invoice data.\n<document>\n{document}\n</document>"}]
    for attempt in range(1, max_attempts + 1):
        reply = client.messages.create(model=MODEL, max_tokens=300, messages=messages,
                                       output_config={"format": {"type": "json_schema", "schema": for_api(LOCAL_SCHEMA)}})
        if reply.stop_reason in ("refusal", "max_tokens"):
            return {"status": "refused" if reply.stop_reason == "refusal" else "truncated", "attempts": attempt}
        raw = reply.content[0].text
        value = normalise_enum(json.loads(raw))
        errors = problems(value, document)
        if not errors:
            return {"status": "ok", "attempts": attempt, "value": value}
        messages += [{"role": "assistant", "content": raw}, {"role": "user", "content": "Rejected:\n" + "\n".join(errors) + "\nReturn corrected JSON."}]
    return {"status": "failed", "attempts": max_attempts, "errors": errors}


DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business."


def body(**fields):
    return json.dumps({"vendor": "Acme Tools", "total": 120.5, "currency": "EUR", "evidence": "Total due: 120.50 EUR", **fields})


REPLIES = [
    message([text(body(currency="Eur"))], model=MODEL),
    message([text(body(evidence="Total due: 999.00 USD"))], model=MODEL),
    message([text(body())], model=MODEL),
    message([text("I can't help with that.")], stop_reason="refusal", model=MODEL),
    message([text('{"vendor": "Acme')], stop_reason="max_tokens", model=MODEL),
]


def main():
    client, transport = scripted_client(*REPLIES)
    print("schema sent to the API:", json.dumps(for_api(LOCAL_SCHEMA)["properties"]["total"], separators=(",", ":")))
    for n in (1, 2):
        print(f"document {n}:", extract(client, DOC))
    print("document 3:", extract(client, DOC))
    print("document 4:", extract(client, DOC))
    print("requests sent:", len(transport.requests), "| each carried output_config.format.type:", {r["output_config"]["format"]["type"] for r in transport.requests})
    print("second call of document 2 sent:", [m["role"] for m in transport.requests[2]["messages"]], "| feedback:", transport.requests[2]["messages"][-1]["content"].splitlines()[1])


if __name__ == "__main__":
    main()
