"""Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md."""
import json

KINDS = {"transient": True, "validation": False, "permission": False, "business": False, "outcome_unknown": False, "internal": False}
GENERIC = {"", "error", "failed", "failure", "operation failed", "something went wrong", "unknown error"}
ACTIONS = {"transient": "retry_later", "validation": "repair_input", "permission": "escalate", "business": "explain", "outcome_unknown": "verify_first", "internal": "escalate"}


class ToolError(Exception):
    """What a tool raises. `kind` is transient, validation, permission, business or timeout (no answer was received, so the effect is unknown)."""

    def __init__(self, kind, message, retry_after_ms=None, explanation=None):
        super().__init__(message)
        self.kind, self.retry_after_ms, self.explanation = kind, retry_after_ms, explanation


def make_error(kind, message, explanation=None, attempts=1, attempted=None):
    """The structured result of a failed call."""
    if kind not in KINDS:
        raise ValueError(f"unknown error kind: {kind}")
    if str(message or "").strip().lower().rstrip(".") in GENERIC:
        raise ValueError("an error message must say what went wrong and what to do")
    error = {"is_error": True, "category": kind, "retryable": KINDS[kind], "message": message.strip(), "attempts": attempts}
    if explanation:
        error["explanation"] = explanation
    if attempted is not None:
        error["attempted"] = attempted
    return error


def to_tool_result(tool_use_id, result):
    """The tool_result block for the API: an error carries is_error true and its category, retry flag and message as text."""
    if result.get("is_error"):
        text = f"{result['category']} error (retryable: {'yes' if result['retryable'] else 'no'}): {result['message']}"
        if result.get("explanation"):
            text += f" Tell the customer: {result['explanation']}"
        return {"type": "tool_result", "tool_use_id": tool_use_id, "content": text, "is_error": True}
    return {"type": "tool_result", "tool_use_id": tool_use_id, "content": str(result.get("content", "")), "is_error": False}


def _empty(value):
    return value is None or value == "" or value == [] or value == {}


def run_tool(tool, args, policy, sleep):
    """Call a tool, recover locally from what is safe to recover from, and return a result or a structured error."""
    key = policy.get("idempotency_key")
    max_retries, base = policy.get("max_retries", 2), policy.get("base_delay_ms", 100)
    safe_to_repeat = bool(policy.get("read_only")) or bool(key)
    attempts = 0
    while True:
        attempts += 1
        call_args = dict(args)
        if key:
            call_args["idempotency_key"] = key
        try:
            value = tool(call_args)
            return {"ok": True, "content": value, "empty": _empty(value), "attempts": attempts}
        except ToolError as error:
            kind = error.kind
            if kind == "timeout":
                if not safe_to_repeat:
                    return make_error("outcome_unknown", f"{error} The call may have taken effect: check the current state before trying again.", attempts=attempts, attempted=dict(args))
                kind = "transient"
            if kind != "transient":
                return make_error(kind, str(error), explanation=error.explanation, attempts=attempts, attempted=dict(args))
            if attempts > max_retries:
                return make_error("transient", f"{error} Gave up after {attempts} attempts.", attempts=attempts, attempted=dict(args))
            sleep(error.retry_after_ms if error.retry_after_ms is not None else base * 2 ** (attempts - 1))
        except Exception as error:  # a bug in the tool must not end the run
            return make_error("internal", f"unexpected failure in the tool: {error}", attempts=attempts, attempted=dict(args))


def next_action(result):
    """What the loop does next with a result."""
    if not result.get("is_error"):
        return "accept_empty" if result.get("empty") else "continue"
    return ACTIONS[result["category"]]
