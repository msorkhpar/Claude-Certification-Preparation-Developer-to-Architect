"""A scripted model behind the SDK's own transport hook.

The hook is `anthropic.Anthropic(http_client=httpx2.Client(transport=...))`: the SDK (anthropic
1.11.0) depends on `httpx2`, not `httpx`, and rejects `httpx` objects. The reader's code calls
`client.messages.create(...)` unchanged; the request goes to this transport, never to a socket.
"""
import json

import anthropic
import httpx2
from anthropic.types import Message


def message(content, stop_reason="end_turn", model="claude-sonnet-5-5", id="msg_illustrative"):
    """A Messages API response body. Validated against the SDK's own type before use."""
    body = {"id": id, "type": "message", "role": "assistant", "model": model,
            "content": content, "stop_reason": stop_reason, "stop_sequence": None,
            "usage": {"input_tokens": 1, "output_tokens": 1}}
    Message.model_validate(body)  # a scripted turn must be a response the real API could return
    return body


def text(t):
    return {"type": "text", "text": t}


def tool_use(id, name, **input):
    return {"type": "tool_use", "id": id, "name": name, "input": input}


class ScriptedTransport(httpx2.BaseTransport):
    """Pops one scripted body per request. An entry may be a body dict or an (status, body) pair
    to script an error such as a 429 or 529. `requests` keeps every request body the SDK sent."""

    def __init__(self, *script):
        self.script = list(script)
        self.requests = []

    def handle_request(self, request):
        self.requests.append(json.loads(request.content or b"{}"))
        if not self.script:
            return httpx2.Response(500, json={"type": "error", "error": {
                "type": "api_error", "message": "scripted model ran out of replies"}})
        item = self.script.pop(0)
        status, body = item if isinstance(item, tuple) else (200, item)
        return httpx2.Response(status, json=body)


def scripted_client(*script):
    """(client, transport): a real `anthropic.Anthropic` wired to a ScriptedTransport."""
    transport = ScriptedTransport(*script)
    client = anthropic.Anthropic(api_key="placeholder", max_retries=0,
                                 http_client=httpx2.Client(transport=transport))
    return client, transport
