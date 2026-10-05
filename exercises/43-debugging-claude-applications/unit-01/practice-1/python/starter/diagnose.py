"""Diagnose a failure from a trace. See ../../statement.md."""
import json
import logging

log = logging.getLogger(__name__)

HTTP = {
    400: ("invalid_request", "integration", "fix_request"),
    401: ("authentication", "account", "fix_credentials"),
    402: ("billing", "account", "fix_billing"),
    403: ("permission", "account", "fix_access"),
    404: ("not_found", "integration", "fix_request"),
    409: ("conflict", "integration", "resolve_then_retry"),
    413: ("request_too_large", "integration", "shrink_request"),
    500: ("server_error", "service", "retry_backoff"),
    504: ("timeout", "service", "stream_or_batch"),
    529: ("overloaded", "service", "retry_backoff"),
}
STOP = {
    "max_tokens": ("truncated", "integration", "raise_max_tokens"),
    "model_context_window_exceeded": ("context_exceeded", "integration", "trim_context"),
    "refusal": ("refusal", "model", "fallback_model"),
    "pause_turn": ("paused", "integration", "continue_turn"),
}


def _rate_limit(event):
    """TODO 1 of 6 (unlocks e1): the diagnosis of a 429.

    Receives the error event. Returns ("rate_limit", "service", "wait_retry_after") when its `headers` have a `retry-after` key (this wins),
    else ("spend_cap", "account", "wait_for_reset") when `error_code` is "enforced_spend_limit_reached", else ("rate_limit", "service",
    "retry_backoff"). Example: a 429 with error_code enforced_spend_limit_reached and no headers -> the spend_cap triple
    """
    return ("rate_limit", "service", "retry_backoff")


def _by_status(status):
    """TODO 6 of 6 (unlocks e1 and m1): the triple for a status.

    Receives the status number. Returns `HTTP[status]` for a documented status; any other status falls back by class: 500 and above
    use the 500 row, everything else the 400 row. Example: _by_status(502) -> HTTP[500], _by_status(418) -> HTTP[400]
    """
    return HTTP[400]


def _http(event):
    status = event.get("status", 0)
    if status == 429:
        return _rate_limit(event)
    if status == 400 and "spend limit" in event.get("message", "").lower():
        return ("spend_limit", "account", "raise_limit")
    return _by_status(status)


def _has_json_object(text):
    """TODO 2 of 6 (unlocks e4): does the text hold a JSON object?

    Receives a text. Returns True when the span from the first `{` to the last `}` parses as JSON and is an object (a dict, not a list
    or a number); False for no braces, a broken span or another JSON type. Example: _has_json_object('ok {"a": 1} done') -> True,
    _has_json_object('see {nope}') -> False
    """
    return False


def _empty_origin(last_blocks):
    """TODO 3 of 6 (unlocks e3): who is to blame for an empty end turn?

    Receives the block types of the last user message, in order. Returns the triple ("empty_response", "integration",
    "remove_text_after_tool_result") when a `text` block comes after a `tool_result` block, else ("empty_response", "model",
    "add_continue_prompt"). Example: ["tool_result", "text"] -> integration; ["text", "tool_result"] -> model
    """
    return ("empty_response", "model", "add_continue_prompt")


def _tool_failure(event, tools):
    """TODO 4 of 6 (unlocks e5): is this tool event a failure, and whose?

    Receives an event and the tool names of the last request (None when unknown). Returns ("unknown_tool", "model", "return_error_result")
    for a `tool_call` whose name is not in `tools`, ("tool_exception", "integration", "fix_tool_code") for a `tool_result` that has an
    `exception`, and None otherwise (an `is_error` flag alone is not our failure).
    Example: {"kind": "tool_call", "name": "get_wether"} with tools ["get_weather"] -> the unknown_tool triple
    """
    return None


def _recovered(trace, i):
    """TODO 5 of 6 (unlocks e6): did a later response recover from the failure at index i?

    Receives the trace and the index of the failure. Returns True when a later event is a response with status 200, stop_reason
    `end_turn` and a non-empty `content`; False otherwise. Example: a failure at 1 and a good end_turn at 5 -> True
    """
    return False

def _classify(trace, i, tools, last_blocks):
    """The (type, origin, recovery) of one event, or None when the event is not a failure."""
    event = trace[i]
    kind = event.get("kind")
    if kind == "error":
        return _http(event)
    if kind == "network_error":
        return ("network", "service", "retry_backoff")
    if kind == "response" and event.get("status") == 200:
        reason = event.get("stop_reason")
        if reason in STOP:
            return STOP[reason]
        if reason == "end_turn" and not event.get("content"):
            return _empty_origin(last_blocks)
    found = _tool_failure(event, tools)
    if found:
        return found
    if kind == "parse" and event.get("ok") is False:
        if _has_json_object(event.get("text", "")):
            return ("parse_failure", "integration", "extract_json")
        return ("parse_failure", "model", "validate_and_retry")
    return None


def diagnose(trace):
    """The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered."""
    log.debug("diagnose input: %r", trace)
    tools, last_blocks = None, []
    for i, event in enumerate(trace):
        if event.get("kind") == "request":
            tools, last_blocks = event.get("tools"), event.get("last_user_blocks", [])
            continue
        found = _classify(trace, i, tools, last_blocks)
        if found:
            recovered = _recovered(trace, i)
            return {"index": i, "type": found[0], "origin": found[1], "recovery": found[2], "recovered": recovered}
    return {"index": -1, "type": "ok", "origin": "none", "recovery": "none", "recovered": False}
