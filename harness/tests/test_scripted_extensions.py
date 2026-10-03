"""Header capture, error and function replies, the async transport and server-sent-event replies."""
import asyncio

import anthropic
import httpx2
import pytest

from harness import ScriptedTransport, scripted_async_client, scripted_client, sse_encode, sse_response
from harness.scripted import message, text

PARAMS = dict(model="claude-sonnet-5-5", max_tokens=8, messages=[{"role": "user", "content": "hi"}])


def test_transport_keeps_urls_and_lower_case_request_headers():
    client, t = scripted_client(message([text("ok")]))
    client.messages.create(**PARAMS)
    assert t.urls == ["https://api.anthropic.com/v1/messages"]
    assert t.headers[0]["anthropic-version"] == "2023-06-01" and "x-api-key" in t.headers[0]


def test_a_reply_may_carry_headers_and_the_sdk_reads_the_request_id():
    body = {"type": "error", "error": {"type": "overloaded_error", "message": "x"}}
    client, _ = scripted_client((529, body, {"request-id": "req_illustrative"}))
    with pytest.raises(anthropic.APIStatusError) as err:
        client.messages.create(**PARAMS)
    assert err.value.request_id == "req_illustrative"


def test_a_reply_may_be_an_exception_or_a_function_of_the_request_body():
    client, t = scripted_client(httpx2.ConnectError("scripted"), lambda body: message([text(body["messages"][0]["content"] + "!")]))
    client = client.with_options(max_retries=1)
    assert client.messages.create(**PARAMS).content[0].text == "hi!"
    assert len(t.requests) == 2


def test_message_accepts_usage_and_stop_sequence():
    body = message([text("x")], "stop_sequence", usage={"input_tokens": 7, "output_tokens": 3}, stop_sequence="END")
    assert body["usage"]["input_tokens"] == 7 and body["stop_sequence"] == "END"


def test_async_transport_counts_requests_in_flight():
    client, t = scripted_async_client(*[message([text("ok")])] * 6, delay=0.02)

    async def go():
        await asyncio.gather(*(client.messages.create(**PARAMS) for _ in range(6)))

    asyncio.run(go())
    assert t.max_in_flight == 6 and t.in_flight == 0


def test_sse_encoding_names_every_event():
    body = sse_encode([{"type": "ping"}, {"type": "message_stop"}]).decode()
    assert body == 'event: ping\ndata: {"type": "ping"}\n\nevent: message_stop\ndata: {"type": "message_stop"}\n\n'
    assert sse_response([{"type": "ping"}]).headers["content-type"] == "text/event-stream"
