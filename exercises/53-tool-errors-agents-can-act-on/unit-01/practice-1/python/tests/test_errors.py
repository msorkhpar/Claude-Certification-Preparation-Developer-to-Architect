import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from errors import ToolError, make_error, next_action, run_tool, to_tool_result

ARGS = {"order": "A-7", "amount": 40}


class Tool:
    """A scripted tool: each call takes the next script entry (an exception is raised, anything else is returned); every call is kept."""

    def __init__(self, *script):
        self.script, self.calls = list(script), []

    def __call__(self, args):
        self.calls.append(dict(args))
        step = self.script.pop(0) if len(self.script) > 1 else self.script[0]
        if isinstance(step, Exception):
            raise step
        return step


class Clock:
    def __init__(self):
        self.waits = []

    def __call__(self, ms):
        self.waits.append(ms)


def run(tool, policy=None, args=None):
    clock = Clock()
    result = run_tool(tool, dict(ARGS) if args is None else args, policy or {"max_retries": 2, "base_delay_ms": 100}, clock)
    assert result is not None, "run_tool returned nothing"
    return result, clock.waits


def test_m1_a_failed_call_becomes_a_structured_error_with_a_category_a_retry_flag_and_an_error_flag():
    error = make_error("transient", "The billing service timed out after 5 s.")
    assert error == {"is_error": True, "category": "transient", "retryable": True, "message": "The billing service timed out after 5 s.", "attempts": 1}
    block = to_tool_result("toolu_1", error)
    assert block == {"type": "tool_result", "tool_use_id": "toolu_1", "content": "transient error (retryable: yes): The billing service timed out after 5 s.", "is_error": True}
    business = make_error("business", "Refunds above 500 need a person.", explanation="A colleague will contact you about this refund.")
    assert business["retryable"] is False and business["explanation"] == "A colleague will contact you about this refund."
    assert to_tool_result("toolu_2", business)["content"] == "business error (retryable: no): Refunds above 500 need a person. Tell the customer: A colleague will contact you about this refund."
    for kind in ("validation", "permission", "outcome_unknown", "internal"):
        assert make_error(kind, "Specific text.")["retryable"] is False
    assert to_tool_result("toolu_3", {"ok": True, "content": "refund R-1 created"}) == {"type": "tool_result", "tool_use_id": "toolu_3", "content": "refund R-1 created", "is_error": False}


def test_e1_a_generic_message_or_an_unknown_category_is_refused():
    refused = 0
    for message in ("Operation failed", "", "   ", "Failed.", "Error", "Something went wrong"):
        try:
            make_error("transient", message)
        except ValueError:
            refused += 1
    assert refused == 6
    refused = 0
    for kind in ("oops", "timeout", "", "Transient"):
        try:
            make_error(kind, "A specific message that says what to change.")
        except ValueError:
            refused += 1
    assert refused == 4


def test_e2_only_transient_failures_are_retried_with_growing_waits_and_the_other_kinds_return_at_once():
    tool = Tool(ToolError("transient", "Billing is unavailable."), ToolError("transient", "Billing is unavailable."), "refund R-1 created")
    result, waits = run(tool)
    assert result["ok"] is True and result["content"] == "refund R-1 created" and result["attempts"] == 3
    assert waits == [100, 200] and len(tool.calls) == 3
    for kind, message, explanation in (("validation", "amount must be a positive whole number", None), ("permission", "this key may not issue refunds", None), ("business", "Refunds above 500 need a person.", "A colleague will contact you.")):
        tool = Tool(ToolError(kind, message, explanation=explanation), "never reached")
        result, waits = run(tool)
        assert result.get("is_error") is True and result.get("category") == kind and result.get("retryable") is False
        assert result.get("message") == message and result.get("explanation") == explanation
        assert result.get("attempts") == 1 and result.get("attempted") == ARGS and waits == [] and len(tool.calls) == 1


def test_e3_the_retries_are_bounded_and_a_wait_the_service_asks_for_is_honoured():
    tool = Tool(ToolError("transient", "Billing is unavailable."))
    result, waits = run(tool)
    assert len(tool.calls) == 3 and waits == [100, 200]
    assert result["is_error"] is True and result["category"] == "transient" and result["retryable"] is True and result["attempts"] == 3
    assert "Gave up after 3 attempts" in result["message"] and result["attempted"] == ARGS
    tool = Tool(ToolError("transient", "Rate limited.", retry_after_ms=1500), ToolError("transient", "Rate limited."), "ok")
    result, waits = run(tool)
    assert waits == [1500, 200] and result["ok"] is True and result["attempts"] == 3
    tool = Tool(ToolError("transient", "Billing is unavailable."))
    result, waits = run(tool, {"max_retries": 0, "base_delay_ms": 100})
    assert len(tool.calls) == 1 and waits == [] and result["attempts"] == 1 and result["category"] == "transient"


def test_e4_a_valid_empty_result_is_a_success_and_not_an_error():
    for empty in ([], "", {}, None):
        result, waits = run(Tool(empty))
        assert result["ok"] is True and result["empty"] is True and result["attempts"] == 1 and waits == []
        assert next_action(result) == "accept_empty"
    block = to_tool_result("toolu_1", {"ok": True, "content": ""})
    assert block["is_error"] is False and block["content"] == ""
    result, _ = run(Tool("3 orders"))
    assert result["empty"] is False and next_action(result) == "continue"


def test_e5_a_timeout_on_a_write_is_an_unknown_outcome_and_is_retried_only_when_repeating_it_is_safe():
    tool = Tool(ToolError("timeout", "No answer from the refund service."), "refund R-1 created")
    result, waits = run(tool)
    assert len(tool.calls) == 1 and waits == []
    assert result["is_error"] is True and result["category"] == "outcome_unknown" and result["retryable"] is False
    assert "No answer from the refund service." in result["message"] and "check the current state" in result["message"]
    assert result["attempted"] == ARGS
    tool = Tool(ToolError("timeout", "No answer."), "3 orders")
    result, waits = run(tool, {"max_retries": 2, "base_delay_ms": 100, "read_only": True})
    assert result["ok"] is True and result["attempts"] == 2 and waits == [100]
    tool = Tool(ToolError("timeout", "No answer."), ToolError("timeout", "No answer."), "refund R-1 created")
    mine = dict(ARGS)
    result, waits = run(tool, {"max_retries": 2, "base_delay_ms": 100, "idempotency_key": "k-1"}, args=mine)
    assert result["ok"] is True and result["attempts"] == 3 and waits == [100, 200]
    assert tool.calls == [{**ARGS, "idempotency_key": "k-1"}] * 3
    assert mine == ARGS, "the caller's arguments must not be changed"


def test_e6_the_next_action_follows_the_category():
    expected = {"transient": "retry_later", "validation": "repair_input", "permission": "escalate", "business": "explain", "outcome_unknown": "verify_first", "internal": "escalate"}
    for kind, action in expected.items():
        assert next_action(make_error(kind, "A specific message.")) == action
    assert next_action({"ok": True, "content": "x", "empty": False}) == "continue"
    assert next_action({"ok": True, "content": [], "empty": True}) == "accept_empty"


def test_e7_an_unexpected_exception_becomes_an_internal_error_and_the_run_goes_on():
    for boom in (RuntimeError("boom"), KeyError("missing")):
        tool = Tool(boom, "never reached")
        result, waits = run(tool)
        assert result["is_error"] is True and result["category"] == "internal" and result["retryable"] is False
        assert result["attempts"] == 1 and waits == [] and len(tool.calls) == 1
    result, _ = run(Tool(RuntimeError("boom")))
    assert "boom" in result["message"] and result["attempted"] == ARGS
