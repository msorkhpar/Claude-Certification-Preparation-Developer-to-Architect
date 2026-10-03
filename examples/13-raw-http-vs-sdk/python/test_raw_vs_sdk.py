import anthropic
import pytest

from harness import ScriptedTransport
from raw_vs_sdk import LIMITED, OK, PAYLOAD, raw_call, sdk_call


def test_raw_and_sdk_send_the_same_request_contract():
    raw, sdk = ScriptedTransport(OK), ScriptedTransport(OK)
    raw_call(raw)
    sdk_call(sdk)
    assert raw.urls == sdk.urls == ["https://api.anthropic.com/v1/messages"]
    assert raw.requests[0] == sdk.requests[0] == PAYLOAD
    assert raw.headers[0]["anthropic-version"] == sdk.headers[0]["anthropic-version"] == "2023-06-01"
    assert raw.headers[0]["x-api-key"] == sdk.headers[0]["x-api-key"]


def test_sdk_adds_headers_the_raw_call_does_not_send():
    raw, sdk = ScriptedTransport(OK), ScriptedTransport(OK)
    raw_call(raw)
    sdk_call(sdk)
    assert "x-stainless-lang" in sdk.headers[0] and "x-stainless-lang" not in raw.headers[0]


def test_a_429_is_a_status_for_raw_code_and_a_typed_error_for_the_sdk():
    assert raw_call(ScriptedTransport(LIMITED)).status_code == 429
    with pytest.raises(anthropic.RateLimitError) as err:
        sdk_call(ScriptedTransport(LIMITED))
    assert err.value.request_id == "req_illustrative_0001"
