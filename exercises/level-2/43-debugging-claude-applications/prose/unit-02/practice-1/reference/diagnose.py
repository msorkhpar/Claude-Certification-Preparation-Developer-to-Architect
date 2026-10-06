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
    if "retry-after" in event.get("headers", {}):
        return ("rate_limit", "service", "wait_retry_after")
    if event.get("error_code") == "enforced_spend_limit_reached":
        return ("spend_cap", "account", "wait_for_reset")
    return ("rate_limit", "service", "retry_backoff")


def _by_status(status):
    if status in HTTP:
        return HTTP[status]
    return HTTP[500] if status >= 500 else HTTP[400]


def _http(event):
    status = event.get("status", 0)
    if status == 429:
        return _rate_limit(event)
    if status == 400 and "spend limit" in event.get("message", "").lower():
        return ("spend_limit", "account", "raise_limit")
    return _by_status(status)


def _has_json_object(text):
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end < start:
        return False
    try:
        return isinstance(json.loads(text[start:end + 1]), dict)
    except ValueError:
        return False


def _empty_origin(last_blocks):
    if "tool_result" in last_blocks and "text" in last_blocks[last_blocks.index("tool_result"):]:
        return ("empty_response", "integration", "remove_text_after_tool_result")
    return ("empty_response", "model", "add_continue_prompt")


def _tool_failure(event, tools):
    kind = event.get("kind")
    if kind == "tool_call" and tools is not None and event.get("name") not in tools:
        return ("unknown_tool", "model", "return_error_result")
    if kind == "tool_result" and event.get("exception"):
        return ("tool_exception", "integration", "fix_tool_code")
    return None


def _recovered(trace, i):
    return any(e.get("kind") == "response" and e.get("status") == 200 and e.get("stop_reason") == "end_turn" and e.get("content")
               for e in trace[i + 1:])

def _stop_failure(reason):
    """The triple for a successful response that still failed by its stop reason, or None."""
    return STOP.get(reason)


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
        stopped = _stop_failure(reason)
        if stopped:
            return stopped
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
