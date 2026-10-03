"""Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md."""
import json


class ParseError(Exception):
    """No JSON object could be read from the model's text."""


def parse_json(text):
    """The JSON value in a model reply: the body of a code fence, else the span from the first { to the last }."""
    body = text
    fence = text.find("```")
    if fence != -1:
        start = text.find("\n", fence)
        end = text.find("```", start if start != -1 else fence + 3)
        if start != -1 and end != -1:
            body = text[start + 1:end]
    first, last = body.find("{"), body.rfind("}")
    if first == -1 or last < first:
        raise ParseError("no JSON object found in the reply")
    try:
        return json.loads(body[first:last + 1])
    except ValueError as err:
        raise ParseError(f"invalid JSON: {err}") from None


def _is_integer(value):
    return (isinstance(value, int) and not isinstance(value, bool)) or (isinstance(value, float) and value.is_integer())


def _is_number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool)


TYPES = {
    "string": lambda v: isinstance(v, str),
    "integer": _is_integer,
    "number": _is_number,
    "boolean": lambda v: isinstance(v, bool),
    "array": lambda v: isinstance(v, list),
    "object": lambda v: isinstance(v, dict),
    "null": lambda v: v is None,
}


def validate(schema, value, path="$"):
    """A list of {"path", "message"} for every way value breaks schema; empty when it conforms."""
    errors = []
    want = schema.get("type")
    if want is not None and not TYPES[want](value):
        return [{"path": path, "message": f"must be of type {want}"}]
    if "enum" in schema and value not in schema["enum"]:
        errors.append({"path": path, "message": f"must be one of {schema['enum']}"})
    if _is_number(value):
        if "minimum" in schema and value < schema["minimum"]:
            errors.append({"path": path, "message": f"must be at least {schema['minimum']}"})
        if "maximum" in schema and value > schema["maximum"]:
            errors.append({"path": path, "message": f"must be at most {schema['maximum']}"})
    if isinstance(value, dict):
        for key in schema.get("required", []):
            if key not in value:
                errors.append({"path": f"{path}.{key}", "message": "is required"})
        properties = schema.get("properties", {})
        for key, sub in properties.items():
            if key in value:
                errors += validate(sub, value[key], f"{path}.{key}")
        if schema.get("additionalProperties") is False:
            for key in value:
                if key not in properties:
                    errors.append({"path": f"{path}.{key}", "message": "is not allowed"})
    if isinstance(value, list) and "items" in schema:
        for i, item in enumerate(value):
            errors += validate(schema["items"], item, f"{path}[{i}]")
    return errors


def _text(reply):
    return "".join(block["text"] for block in reply["content"] if block["type"] == "text")


def _prompt(document, schema):
    return ("Extract the data from the document as one JSON object that follows this JSON Schema. Reply with the JSON only.\n"
            f"<schema>{json.dumps(schema)}</schema>\n<document>\n{document}\n</document>")


def _feedback(errors):
    lines = "\n".join(f"- {e['path']}: {e['message']}" for e in errors)
    return f"Your reply was rejected:\n{lines}\nReturn the corrected JSON only."


def extract(ask, document, schema, max_attempts=3, evidence_fields=()):
    """Ask, parse, validate and, on a problem, re-prompt with the errors, at most max_attempts calls."""
    messages = [{"role": "user", "content": _prompt(document, schema)}]
    errors = []
    for attempt in range(1, max_attempts + 1):
        reply = ask(messages)
        if reply.get("stop_reason") == "max_tokens":
            return {"status": "truncated", "value": None, "attempts": attempt, "errors": []}
        text = _text(reply)
        try:
            value = parse_json(text)
            errors = validate(schema, value)
            for name in evidence_fields:
                quoted = value.get(name) if isinstance(value, dict) else None
                if isinstance(quoted, str) and quoted not in document:
                    errors.append({"path": f"$.{name}", "message": "is not found in the document"})
        except ParseError as err:
            value, errors = None, [{"path": "$", "message": str(err)}]
        if not errors:
            return {"status": "ok", "value": value, "attempts": attempt, "errors": []}
        if attempt < max_attempts:
            messages = messages + [{"role": "assistant", "content": text}, {"role": "user", "content": _feedback(errors)}]
    return {"status": "failed", "value": None, "attempts": max_attempts, "errors": errors}
