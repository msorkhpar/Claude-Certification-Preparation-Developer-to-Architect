import json
import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from diagnose import diagnose

REQ = {"kind": "request", "model": "claude-sonnet-5-5", "max_tokens": 1024, "tools": ["get_weather"], "last_user_blocks": ["text"]}


def one(*events):
    """The diagnosis of a trace that starts with the usual request, as 'type|origin|recovery'."""
    r = diagnose([REQ, *events]) or {}
    return f"{r.get('type')}|{r.get('origin')}|{r.get('recovery')}"


def error(status, error_type="x_error", **extra):
    return {"kind": "error", "status": status, "error_type": error_type, "message": "m", **extra}


def reply(stop_reason, content=None):
    return {"kind": "response", "status": 200, "stop_reason": stop_reason, "content": content if content is not None else [{"type": "text", "text": "hi"}]}


def test_m1_each_documented_http_error_maps_to_a_type_an_origin_and_a_recovery():
    assert one(error(400)) == "invalid_request|integration|fix_request"
    assert one(error(401)) == "authentication|account|fix_credentials"
    assert one(error(402)) == "billing|account|fix_billing"
    assert one(error(403)) == "permission|account|fix_access"
    assert one(error(404)) == "not_found|integration|fix_request"
    assert one(error(409)) == "conflict|integration|resolve_then_retry"
    assert one(error(413)) == "request_too_large|integration|shrink_request"
    assert one(error(500)) == "server_error|service|retry_backoff"
    assert one(error(504)) == "timeout|service|stream_or_batch"
    assert one(error(529)) == "overloaded|service|retry_backoff"


def test_e1_a_429_is_a_rate_limit_or_a_spend_cap_and_other_statuses_fall_back_by_class():
    assert one(error(429, headers={"retry-after": "12"})) == "rate_limit|service|wait_retry_after"
    assert one(error(429, error_code="enforced_spend_limit_reached")) == "spend_cap|account|wait_for_reset"
    assert one(error(429, headers={"retry-after": "3"}, error_code="enforced_spend_limit_reached")) == "rate_limit|service|wait_retry_after"
    assert one(error(429)) == "rate_limit|service|retry_backoff"
    assert one(error(400, message="You have reached your workspace Spend Limit")) == "spend_limit|account|raise_limit"
    assert one(error(418)) == "invalid_request|integration|fix_request"
    assert one(error(502)) == "server_error|service|retry_backoff"


def test_e2_a_successful_response_can_still_fail_by_its_stop_reason():
    assert one(reply("max_tokens")) == "truncated|integration|raise_max_tokens"
    assert one(reply("model_context_window_exceeded")) == "context_exceeded|integration|trim_context"
    assert one(reply("refusal")) == "refusal|model|fallback_model"
    assert one(reply("pause_turn")) == "paused|integration|continue_turn"
    for fine in ("end_turn", "stop_sequence", "tool_use"):
        assert one(reply(fine)) == "ok|none|none", fine


def test_e3_an_empty_end_turn_is_the_integration_when_text_followed_the_tool_result_and_the_model_otherwise():
    def trace(blocks):
        return [{**REQ, "last_user_blocks": blocks}, reply("end_turn", [])]

    def label(blocks):
        r = diagnose(trace(blocks)) or {}
        return f"{r.get('index')}|{r.get('type')}|{r.get('origin')}|{r.get('recovery')}"

    assert label(["tool_result", "text"]) == "1|empty_response|integration|remove_text_after_tool_result"
    assert label(["tool_result"]) == "1|empty_response|model|add_continue_prompt"
    assert label(["text"]) == "1|empty_response|model|add_continue_prompt"
    assert label(["text", "tool_result"]) == "1|empty_response|model|add_continue_prompt"
    assert label(["tool_result", "tool_result", "text"]) == "1|empty_response|integration|remove_text_after_tool_result"
    assert one(reply("end_turn")) == "ok|none|none"


def test_e4_a_parse_failure_is_the_integration_when_a_json_object_is_in_the_text_and_the_model_when_not():
    def parse(text):
        return {"kind": "parse", "ok": False, "text": text}

    assert one(parse('Here you go: {"label": "spam"} hope it helps')) == "parse_failure|integration|extract_json"
    assert one(parse('```json\n{"a": 1}\n```')) == "parse_failure|integration|extract_json"
    assert one(parse("I cannot decide")) == "parse_failure|model|validate_and_retry"
    assert one(parse('{"label": "spam"')) == "parse_failure|model|validate_and_retry"
    assert one(parse('see [1, 2] and {nope}')) == "parse_failure|model|validate_and_retry"
    assert one({"kind": "parse", "ok": True, "text": '{"a": 1}'}) == "ok|none|none"


def test_e5_tool_failures_split_into_a_model_that_called_a_missing_tool_and_our_tool_that_raised():
    assert one({"kind": "tool_call", "name": "get_weather", "input": {}}) == "ok|none|none"
    assert one({"kind": "tool_call", "name": "get_wether", "input": {}}) == "unknown_tool|model|return_error_result"
    assert one({"kind": "tool_result", "name": "get_weather", "is_error": False}) == "ok|none|none"
    assert one({"kind": "tool_result", "name": "get_weather", "is_error": True, "exception": "KeyError: 'city'"}) == "tool_exception|integration|fix_tool_code"
    assert one({"kind": "tool_result", "name": "get_weather", "is_error": True}) == "ok|none|none"
    other = diagnose([{"kind": "tool_call", "name": "anything", "input": {}}]) or {}
    assert other.get("type") == "ok"


def test_e6_the_first_failure_names_the_cause_and_a_later_good_response_marks_it_recovered():
    trace = [REQ, error(529), REQ, reply("max_tokens"), REQ, reply("end_turn")]
    r = diagnose(trace) or {}
    assert (r.get("index"), r.get("type"), r.get("recovered")) == (1, "overloaded", True)
    r = diagnose([REQ, error(529), REQ, error(529)]) or {}
    assert (r.get("index"), r.get("recovered")) == (1, False)
    r = diagnose([REQ, error(401), REQ, reply("end_turn", [])]) or {}
    assert (r.get("index"), r.get("type"), r.get("recovered")) == (1, "authentication", False)
    clean = diagnose([REQ, reply("end_turn")]) or {}
    assert clean == {"index": -1, "type": "ok", "origin": "none", "recovery": "none", "recovered": False}
    assert diagnose([]) == clean


def test_e7_a_dropped_connection_has_no_status_and_belongs_to_the_service_side():
    r = diagnose([REQ, reply("end_turn"), REQ, {"kind": "network_error", "message": "connection reset"}]) or {}
    assert (r.get("index"), r.get("type"), r.get("origin"), r.get("recovery")) == (3, "network", "service", "retry_backoff")
    assert r.get("recovered") is False
    r = diagnose([REQ, {"kind": "network_error", "message": "timed out"}, REQ, reply("end_turn")]) or {}
    assert (r.get("index"), r.get("type"), r.get("recovered")) == (1, "network", True)
