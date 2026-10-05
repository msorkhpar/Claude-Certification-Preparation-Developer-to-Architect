"""A raw Messages API client over an injected transport. See ../../statement.md for the contract."""
import json
import logging
from dataclasses import dataclass, field

log = logging.getLogger(__name__)

URL = "https://api.anthropic.com/v1/messages"


@dataclass
class Request:
    method: str
    url: str
    headers: dict
    body: str  # JSON text


@dataclass
class Response:
    status: int
    headers: dict  # lower-case names
    body: str


class ApiError(Exception):
    """A non-2xx reply: status, error type, message and the request id (None when there is none)."""

    def __init__(self, status, error_type, message, request_id=None):
        super().__init__(f"{status} {error_type}: {message}")
        self.status, self.error_type, self.detail, self.request_id = status, error_type, message, request_id


def _present(value):
    return isinstance(value, str) and value.strip() != ""


def _validate(api_key, messages, max_tokens):
    """TODO 1 of 8 (finish this to pass e2): refuse bad input before anything is sent.

    Receives the key, the messages and max_tokens. Raises ValueError when the key is blank (use _present), the messages are empty or
    max_tokens is not a whole number of at least 1; otherwise returns nothing.
    Example: _validate("  ", [], 8) -> ValueError, _validate("sk-x", [{"role": "user", "content": "hi"}], 8) -> None
    """


def _headers(api_key):
    """TODO 2 of 8 (finish this to pass m1): the three request headers.

    Receives the API key. Returns a dict with exactly `x-api-key` (the key), `anthropic-version` (2023-06-01) and `content-type`
    (application/json).
    Example: _headers("k")["anthropic-version"] -> "2023-06-01"
    """
    return {}


def _body(model, messages, max_tokens, system):
    """TODO 3 of 8 (finish this to pass m1 and e1): the request body as a dict.

    Receives the model, the messages, max_tokens and the system text (may be None). Returns a dict with `model`, `max_tokens` and
    `messages`, plus `system` only when it is present and not blank (use _present).
    Example: _body("m", [], 8, "  ") -> {"model": "m", "max_tokens": 8, "messages": []}
    """
    return {}


def build_request(api_key, model, messages, max_tokens, system=None):
    log.debug("build_request input: %r %r %r %r", model, messages, max_tokens, system)
    _validate(api_key, messages, max_tokens)
    return Request("POST", URL, _headers(api_key), json.dumps(_body(model, messages, max_tokens, system)))


def _error_parts(parsed, text):
    """TODO 4 of 8 (finish this to pass e4 and e5): the error type and message of an error reply.

    Receives the parsed JSON body (None when it is not JSON) and the raw body text. For a body shaped like
    {"error": {"type": ..., "message": ...}} returns that type and message as text; for anything else returns "unknown" and the first
    200 characters of the text, trimmed.
    Example: _error_parts({"error": {"type": "x", "message": "m"}}, "") -> ("x", "m"), _error_parts(None, " <html> ") -> ("unknown", "<html>")
    """
    return "", ""


def _pick_request_id(header_id, body_id):
    """TODO 5 of 8 (finish this to pass e4): which request id the error carries.

    Receives the id from the `request-id` header and the id from the body, either may be None. Returns the header's when there is one,
    else the body's, else None.
    Example: _pick_request_id("h", "b") -> "h", _pick_request_id(None, "b") -> "b"
    """
    return None


def _redact(message, api_key):
    """TODO 6 of 8 (finish this to pass e6): keep the key out of the error.

    Receives the message and the API key. Returns the message with every occurrence of the key replaced by [redacted].
    Example: _redact("bad key sk-1", "sk-1") -> "bad key [redacted]"
    """
    return message


def _error_from(response, api_key):
    try:
        parsed = json.loads(response.body)
    except ValueError:
        parsed = None
    kind, message = _error_parts(parsed, response.body)
    body_id = parsed.get("request_id") if isinstance(parsed, dict) else None
    request_id = _pick_request_id(response.headers.get("request-id"), body_id)
    return ApiError(response.status, kind, _redact(message, api_key), request_id)


def _parse_message(text):
    """TODO 7 of 8 (finish this to pass e3): the message of a good reply.

    Receives the response body text. Returns the parsed JSON object.
    Example: _parse_message('{"stop_reason": "end_turn"}') -> {"stop_reason": "end_turn"}
    """
    return {}


def send_messages(transport, api_key, model, messages, max_tokens, system=None):
    request = build_request(api_key, model, messages, max_tokens, system)
    response = transport(request)
    if 200 <= response.status < 300:
        return _parse_message(response.body)
    raise _error_from(response, api_key)


def text_of(message):
    """TODO 8 of 8 (finish this to pass e3): the text of a message.

    Receives a message dict with a `content` list of blocks. Returns the `text` of the blocks whose `type` is "text", joined with
    nothing between them; every other block type is ignored.
    Example: text_of({"content": [{"type": "text", "text": "a"}, {"type": "tool_use"}, {"type": "text", "text": "b"}]}) -> "ab"
    """
    return ""
