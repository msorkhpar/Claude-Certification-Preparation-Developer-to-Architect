"""A scripted model behind the SDK's own transport hook.

The hook is `anthropic.Anthropic(http_client=httpx2.Client(transport=...))`: the SDK (anthropic
1.11.0) depends on `httpx2`, not `httpx`, and rejects `httpx` objects. The reader's code calls
`client.messages.create(...)` unchanged; the request goes to this transport, never to a socket.
"""
import json

import anthropic
import httpx2
from anthropic.types import Message


def message(content, stop_reason="end_turn", model="claude-sonnet-5-5", id="msg_illustrative",
            usage=None, stop_sequence=None):
    """A Messages API response body. Validated against the SDK's own type before use."""
    body = {"id": id, "type": "message", "role": "assistant", "model": model,
            "content": content, "stop_reason": stop_reason, "stop_sequence": stop_sequence,
            "usage": usage or {"input_tokens": 1, "output_tokens": 1}}
    Message.model_validate(body)  # a scripted turn must be a response the real API could return
    return body


def text(t):
    return {"type": "text", "text": t}


def tool_use(id, name, **input):
    return {"type": "tool_use", "id": id, "name": name, "input": input}


def sse_encode(events):
    """The body of a streaming reply: one named server-sent event per dict, as the API frames them."""
    return "".join(f"event: {e['type']}\ndata: {json.dumps(e)}\n\n" for e in events).encode("utf-8")


def sse_response(events, headers=None):
    """A 200 streaming reply built from event dicts (a script entry for ScriptedTransport)."""
    return httpx2.Response(200, headers={"content-type": "text/event-stream", **(headers or {})},
                           content=sse_encode(events))


def _answer(transport, request):
    """Record the request, then return the next scripted reply. A script entry is a body dict, a
    (status, body) pair, a (status, body, headers) triple, a ready httpx2.Response, an exception to
    raise (a failed connection or a timeout), or a function of the request body returning any of these."""
    transport.requests.append(json.loads(request.content or b"{}"))
    transport.headers.append({k.lower(): v for k, v in request.headers.items()})
    transport.urls.append(str(request.url))
    if not transport.script:
        return httpx2.Response(500, json={"type": "error", "error": {
            "type": "api_error", "message": "scripted model ran out of replies"}})
    item = transport.script.pop(0)
    if callable(item) and not isinstance(item, (httpx2.Response, BaseException)):
        item = item(transport.requests[-1])
    if isinstance(item, BaseException):
        raise item
    if isinstance(item, httpx2.Response):
        return item
    if isinstance(item, tuple):
        status, body, *rest = item
        return httpx2.Response(status, json=body, headers=rest[0] if rest else None)
    return httpx2.Response(200, json=item)


class ScriptedTransport(httpx2.BaseTransport):
    """Pops one scripted reply per request. `requests` keeps every request body the SDK sent,
    `headers` the lower-cased request headers and `urls` the URLs, in order."""

    def __init__(self, *script):
        self.script = list(script)
        self.requests, self.headers, self.urls = [], [], []

    def handle_request(self, request):
        return _answer(self, request)


class AsyncScriptedTransport(httpx2.AsyncBaseTransport):
    """The same script for the async client. `delay` seconds pass before each reply, and `in_flight`
    and `max_in_flight` count requests that are inside the transport at the same time."""

    def __init__(self, *script, delay=0.0):
        self.script = list(script)
        self.requests, self.headers, self.urls = [], [], []
        self.delay, self.in_flight, self.max_in_flight = delay, 0, 0

    async def handle_async_request(self, request):
        import asyncio
        self.in_flight += 1
        self.max_in_flight = max(self.max_in_flight, self.in_flight)
        try:
            await asyncio.sleep(self.delay)
            return _answer(self, request)
        finally:
            self.in_flight -= 1


def scripted_client(*script):
    """(client, transport): a real `anthropic.Anthropic` wired to a ScriptedTransport (no SDK retries)."""
    transport = ScriptedTransport(*script)
    client = anthropic.Anthropic(api_key="placeholder", max_retries=0,
                                 http_client=httpx2.Client(transport=transport))
    return client, transport


def scripted_async_client(*script, delay=0.0, max_retries=0):
    """(client, transport): a real `anthropic.AsyncAnthropic` wired to an AsyncScriptedTransport."""
    transport = AsyncScriptedTransport(*script, delay=delay)
    client = anthropic.AsyncAnthropic(api_key="placeholder", max_retries=max_retries,
                                      http_client=httpx2.AsyncClient(transport=transport))
    return client, transport
