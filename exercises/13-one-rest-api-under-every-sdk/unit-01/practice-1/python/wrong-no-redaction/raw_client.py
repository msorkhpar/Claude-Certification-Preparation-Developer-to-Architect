"""A raw Messages API client over an injected transport. See ../../statement.md for the contract."""
import json
from dataclasses import dataclass, field

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


def build_request(api_key, model, messages, max_tokens, system=None):
    if not _present(api_key):
        raise ValueError("api_key is required")
    if not messages:
        raise ValueError("messages must not be empty")
    if not isinstance(max_tokens, int) or max_tokens < 1:
        raise ValueError("max_tokens must be at least 1")
    body = {"model": model, "max_tokens": max_tokens, "messages": messages}
    if _present(system):
        body["system"] = system
    headers = {"x-api-key": api_key, "anthropic-version": "2023-06-01", "content-type": "application/json"}
    return Request("POST", URL, headers, json.dumps(body))


def _error_from(response, api_key):
    request_id = response.headers.get("request-id")
    try:
        parsed = json.loads(response.body)
    except ValueError:
        parsed = None
    if isinstance(parsed, dict) and isinstance(parsed.get("error"), dict):
        error = parsed["error"]
        kind, message = error.get("type", "unknown"), str(error.get("message", ""))
        request_id = request_id or parsed.get("request_id")
    else:
        kind, message = "unknown", response.body.strip()[:200]
    return ApiError(response.status, kind, message, request_id)


def send_messages(transport, api_key, model, messages, max_tokens, system=None):
    request = build_request(api_key, model, messages, max_tokens, system)
    response = transport(request)
    if 200 <= response.status < 300:
        return json.loads(response.body)
    raise _error_from(response, api_key)


def text_of(message):
    return "".join(b.get("text", "") for b in message.get("content", []) if b.get("type") == "text")
