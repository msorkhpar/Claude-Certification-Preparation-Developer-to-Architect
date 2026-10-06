"""Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md."""
import json
import logging

log = logging.getLogger(__name__)

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
    # TODO 2 of 9 (finish this to pass e1): the refusals of make_error. Refuse with an error when the kind is not one of
    #   KINDS, and when the message, trimmed, lower-cased and without final periods, is in GENERIC. Example:
    #   make_error("transient", "Operation failed.") -> raises.
    # TODO 1 of 9 (finish this to pass m1): the structured error. Receives the kind, the message and the attempts. Build
    #   the map: is_error true, category the kind, retryable the flag KINDS holds for that kind, message trimmed,
    #   attempts. Example: make_error("transient", "Service busy, retry later") -> is_error true, retryable true.
    error = {"is_error": False, "category": kind, "retryable": False, "message": message, "attempts": attempts}
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
    # TODO 5 of 9 (finish this to pass e4): the empty check. Return true for none, an empty text, an empty list and an
    #   empty map, and false for anything else (0 and false are real values). Example: [] -> true, 0 -> false.
    return False


def run_tool(tool, args, policy, sleep):
    """Call a tool, recover locally from what is safe to recover from, and return a result or a structured error."""
    log.debug("run_tool input: %r", args)
    key = policy.get("idempotency_key")
    max_retries, base = policy.get("max_retries", 2), policy.get("base_delay_ms", 100)
    safe_to_repeat = bool(policy.get("read_only")) or bool(key)
    attempts = 0
    while True:
        attempts += 1
        call_args = dict(args)
        # TODO 7 of 9 (finish this to pass e5): the idempotency key. When the policy holds a key, add it to `call_args`,
        #   the copy of the arguments that this attempt sends (never to the caller's own map). Example: key "k-1" -> every
        #   attempt receives idempotency_key "k-1".
        try:
            value = tool(call_args)
            return {"ok": True, "content": value, "empty": _empty(value), "attempts": attempts}
        except ToolError as error:
            kind = error.kind
            # TODO 6 of 9 (finish this to pass e5): the timeout. When the kind is timeout: if the call is not safe to
            #   repeat, return make_error("outcome_unknown", the message plus "The call may have taken effect: check the
            #   current state before trying again.", attempts, a copy of the arguments); otherwise treat it as transient.
            #   Example: timeout on a write with no key -> outcome_unknown.
            if kind == "timeout":
                kind = "transient"
            # TODO 3 of 9 (finish this to pass e2): the kinds that are not retried. When the kind is not transient,
            #   return the structured error at once (message and explanation of the tool error, the attempts so far, a
            #   copy of the arguments). Example: a validation error on the first attempt -> make_error(validation, ...,
            #   attempts=1) and no sleep.
            if attempts > max_retries:
                return make_error("transient", f"{error} Gave up after {attempts} attempts.", attempts=attempts, attempted=dict(args))
            # TODO 4 of 9 (finish this to pass e2, e3): the wait before the next attempt, in milliseconds. Receives the
            #   tool error's retry-after value (or none), the base delay and the attempt number from 1. Sleep the value
            #   the service asked for when there is one, otherwise base times 2 to the power of attempts - 1. Example: no
            #   retry-after, base 100 -> waits 100, 200, 400.
            sleep(base)
        # TODO 9 of 9 (finish this to pass e7): the unexpected exception. When the tool raises something that is not a
        #   ToolError, return make_error("internal", "unexpected failure in the tool: " + its message, attempts, a copy of
        #   the arguments) instead of letting it end the run. Example: a tool that raises ValueError("boom") -> category
        #   internal.
        except Exception as error:
            return make_error("transient", f"unexpected failure in the tool: {error}", attempts=attempts, attempted=dict(args))


def next_action(result):
    # TODO 8 of 9 (finish this to pass e6): the next action of the loop. Receives a result. For an error return the
    #   action ACTIONS holds for its category; for a success return accept_empty when it is empty, otherwise continue.
    #   Example: a permission error -> escalate.
    return "continue"
