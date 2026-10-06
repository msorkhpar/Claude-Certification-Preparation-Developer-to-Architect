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
    if not _present(api_key):
        raise ValueError("api_key is required")
    if not messages:
        raise ValueError("messages must not be empty")
    if not isinstance(max_tokens, int) or max_tokens < 1:
        raise ValueError("max_tokens must be at least 1")


def _headers(api_key):
    return {"x-api-key": api_key, "anthropic-version": "2023-06-01", "content-type": "application/json"}


def _body(model, messages, max_tokens, system):
    body = {"model": model, "max_tokens": max_tokens, "messages": messages}
    if _present(system):
        body["system"] = system
    return body


def build_request(api_key, model, messages, max_tokens, system=None):
    log.debug("build_request input: %r %r %r %r", model, messages, max_tokens, system)
    _validate(api_key, messages, max_tokens)
    return Request("POST", URL, _headers(api_key), json.dumps(_body(model, messages, max_tokens, system)))


def _error_parts(parsed, text):
    if isinstance(parsed, dict) and isinstance(parsed.get("error"), dict):
        error = parsed["error"]
        return error.get("type", "unknown"), str(error.get("message", ""))
    return "unknown", text.strip()[:200]


def _pick_request_id(header_id, body_id):
    return header_id or body_id


def _redact(message, api_key):
    return message.replace(api_key, "[redacted]")


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
    return json.loads(text)


def send_messages(transport, api_key, model, messages, max_tokens, system=None):
    request = build_request(api_key, model, messages, max_tokens, system)
    response = transport(request)
    if 200 <= response.status < 300:
        return _parse_message(response.body)
    raise _error_from(response, api_key)


def text_of(message):
    return "".join(b.get("text", "") for b in message.get("content", []) if b.get("type") == "text")
