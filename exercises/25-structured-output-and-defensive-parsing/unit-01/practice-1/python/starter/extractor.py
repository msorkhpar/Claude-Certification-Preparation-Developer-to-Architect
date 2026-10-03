"""Extract structured data from a document with validation and a bounded re-prompt. See ../../statement.md."""


class ParseError(Exception):
    """No JSON object could be read from the model's text."""


def parse_json(text):
    # TODO: the JSON value in a reply: a code fence's body, else the span from the first { to the last }.
    return None


def validate(schema, value, path="$"):
    # TODO: a list of {"path", "message"}, one per way value breaks schema; empty when it conforms.
    return None


def extract(ask, document, schema, max_attempts=3, evidence_fields=()):
    # TODO: ask, parse, validate and, on a problem, re-prompt with the errors, at most max_attempts calls.
    return None
