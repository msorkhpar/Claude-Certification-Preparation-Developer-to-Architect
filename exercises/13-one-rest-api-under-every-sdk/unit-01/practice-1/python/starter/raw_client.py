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


def build_request(api_key, model, messages, max_tokens, system=None):
    # TODO: method, url, the three headers and the JSON body, as the statement says.
    return Request("GET", "", {}, "{}")


def send_messages(transport, api_key, model, messages, max_tokens, system=None):
    # TODO: build the request, call transport(request), return the parsed message or raise ApiError.
    return {}


def text_of(message):
    # TODO: the text of the text blocks, joined.
    return ""
