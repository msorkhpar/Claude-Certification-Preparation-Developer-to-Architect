import json
import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from raw_client import ApiError, Response, build_request, send_messages, text_of

KEY = "sk-test-0123456789abcdef"
MSGS = [{"role": "user", "content": "Capital of France?"}]
OK_BODY = {"id": "msg_x", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5",
           "content": [{"type": "text", "text": "Paris."}], "stop_reason": "end_turn", "stop_sequence": None,
           "usage": {"input_tokens": 9, "output_tokens": 3}}


def reply(status, body, headers=None):
    text = body if isinstance(body, str) else json.dumps(body)
    return Response(status, headers or {}, text)


def error_of(fn):
    """The exception the call raised, or None when it did not raise."""
    try:
        fn()
    except Exception as err:  # noqa: BLE001 - the tests judge the type themselves
        return err
    return None


def test_m1_request_has_method_url_three_headers_and_json_body():
    req = build_request(KEY, "claude-sonnet-5-5", MSGS, 64, "Be brief.")
    assert req.method == "POST"
    assert req.url == "https://api.anthropic.com/v1/messages"
    assert req.headers == {"x-api-key": KEY, "anthropic-version": "2023-06-01", "content-type": "application/json"}
    assert json.loads(req.body) == {"model": "claude-sonnet-5-5", "max_tokens": 64, "messages": MSGS, "system": "Be brief."}


def test_e1_blank_or_absent_system_is_left_out_of_the_body():
    for system in (None, "", "   "):
        body = json.loads(build_request(KEY, "m", MSGS, 8, system).body)
        assert "system" not in body, system
    assert json.loads(build_request(KEY, "m", MSGS, 8, "x").body).get("system") == "x"


def test_e2_bad_input_is_refused_before_anything_is_sent():
    calls = []

    def transport(req):
        calls.append(req)
        return reply(200, OK_BODY)

    for bad in ({"max_tokens": 0}, {"max_tokens": -5}, {"messages": []}, {"api_key": "  "}):
        args = {"api_key": KEY, "model": "m", "messages": MSGS, "max_tokens": 8, **bad}
        err = error_of(lambda: send_messages(transport, **args))
        assert isinstance(err, ValueError), (bad, err)
    assert calls == []


def test_e3_success_returns_the_message_and_text_joins_text_blocks_only():
    seen = []

    def transport(req):
        seen.append(req)
        return reply(200, OK_BODY)

    message = send_messages(transport, KEY, "claude-sonnet-5-5", MSGS, 64)
    assert message.get("stop_reason") == "end_turn" and len(seen) == 1 and seen[0].method == "POST"
    mixed = {"content": [{"type": "text", "text": "Let me "}, {"type": "tool_use", "id": "t", "name": "x", "input": {}},
                         {"type": "text", "text": "check."}]}
    assert text_of(mixed) == "Let me check."
    assert text_of({"content": []}) == ""


def test_e4_an_error_reply_becomes_an_api_error_with_the_header_request_id():
    body = {"type": "error", "error": {"type": "rate_limit_error", "message": "Rate limited"}, "request_id": "req_from_body"}
    err = error_of(lambda: send_messages(lambda r: reply(429, body, {"request-id": "req_from_header"}), KEY, "m", MSGS, 8))
    assert isinstance(err, ApiError), err
    assert (err.status, err.error_type, err.detail) == (429, "rate_limit_error", "Rate limited")
    assert err.request_id == "req_from_header"
    err = error_of(lambda: send_messages(lambda r: reply(529, {**body, "error": {"type": "overloaded_error", "message": "Overloaded"}}), KEY, "m", MSGS, 8))
    assert isinstance(err, ApiError) and err.request_id == "req_from_body" and err.error_type == "overloaded_error"


def test_e5_a_reply_that_is_not_json_still_gives_an_api_error():
    html = "<html><body><h1>502 Bad Gateway</h1></body></html>"
    err = error_of(lambda: send_messages(lambda r: reply(502, html), KEY, "m", MSGS, 8))
    assert isinstance(err, ApiError), err
    assert err.status == 502 and err.error_type == "unknown" and "502 Bad Gateway" in err.detail
    assert err.request_id is None


def test_e6_the_api_key_never_appears_in_an_error():
    body = {"type": "error", "error": {"type": "authentication_error", "message": f"invalid x-api-key: {KEY}"}}
    err = error_of(lambda: send_messages(lambda r: reply(401, body), KEY, "m", MSGS, 8))
    assert isinstance(err, ApiError), err
    assert KEY not in str(err) and KEY not in err.detail
    assert "[redacted]" in err.detail
