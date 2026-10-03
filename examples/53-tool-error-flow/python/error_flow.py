"""What a loop does with seven failed or odd tool calls: a structured result for each, bounded retries, and the next action.

The tools are scripted functions, so the output shows the control flow and the text the model would be given, and nothing about how a model would
answer. The refund service, the orders and the limits are illustrative.
"""


class ToolError(Exception):
    def __init__(self, kind, message, retry_after_ms=None, explanation=None):
        super().__init__(message)
        self.kind, self.retry_after_ms, self.explanation = kind, retry_after_ms, explanation


RETRYABLE = {"transient": "yes", "validation": "no", "permission": "no", "business": "no", "outcome_unknown": "no", "internal": "no"}
ACTION = {"transient": "retry later", "validation": "repair the input", "permission": "escalate", "business": "explain to the customer", "outcome_unknown": "check the state first", "internal": "escalate"}


def failure(kind, message, attempts, explanation=None):
    text = f"{kind} error (retryable: {RETRYABLE[kind]}): {message}"
    if explanation:
        text += f" Tell the customer: {explanation}"
    return {"is_error": True, "kind": kind, "content": text, "attempts": attempts}


def run(tool, args, key=None, read_only=False, max_retries=2, base_ms=100):
    """Retry what is safe to retry, give up with a structured error on the rest. Returns the result and the waits."""
    waits, attempts = [], 0
    while True:
        attempts += 1
        call = dict(args, idempotency_key=key) if key else dict(args)
        try:
            value = tool(call)
            return {"is_error": False, "content": value, "attempts": attempts, "empty": value in ("", [], None)}, waits
        except ToolError as error:
            kind = error.kind
            if kind == "timeout":
                if not (key or read_only):
                    return failure("outcome_unknown", f"{error} The call may have taken effect: check the current state before trying again.", attempts), waits
                kind = "transient"
            if kind != "transient":
                return failure(kind, str(error), attempts, error.explanation), waits
            if attempts > max_retries:
                return failure("transient", f"{error} Gave up after {attempts} attempts.", attempts), waits
            waits.append(error.retry_after_ms if error.retry_after_ms is not None else base_ms * 2 ** (attempts - 1))
        except Exception as error:
            return failure("internal", f"unexpected failure in the tool: {error}", attempts), waits


def next_step(result):
    if not result["is_error"]:
        return "accept the empty result" if result["empty"] else "continue"
    return ACTION[result["kind"]]


def scripted(*steps):
    """A tool that does what the script says, one entry per call; the last entry repeats."""
    state = {"n": 0, "seen": []}

    def tool(args):
        state["seen"].append(args.get("idempotency_key"))
        step = steps[min(state["n"], len(steps) - 1)]
        state["n"] += 1
        if isinstance(step, Exception):
            raise step
        return step

    tool.state = state
    return tool


def scenarios():
    return [
        ("get_order order=A-7", scripted(ToolError("transient", "The order service is busy."), ToolError("transient", "The order service is busy."), "order A-7: 2 items"), {}),
        ("process_refund amount=-5", scripted(ToolError("validation", "amount must be a positive whole number, for example 40")), {}),
        ("process_refund amount=900", scripted(ToolError("business", "Refunds above 500 need a person.", explanation="A colleague will contact you about this refund.")), {}),
        ("process_refund amount=40, no key", scripted(ToolError("timeout", "No answer from the refund service.")), {}),
        ("process_refund amount=40, key refund-A-7-1", scripted(ToolError("timeout", "No answer from the refund service."), "refund R-1 created"), {"key": "refund-A-7-1"}),
        ("list_orders customer=C-9", scripted([]), {"read_only": True}),
        ("process_refund amount=40, tool bug", scripted(RuntimeError("the currency table is missing")), {}),
    ]


def main():
    for number, (title, tool, options) in enumerate(scenarios(), start=1):
        result, waits = run(tool, {"order": "A-7"}, **options)
        print(f"{number}. {title}")
        print(f"   attempts {result['attempts']}, waits {waits}, keys sent {tool.state['seen']}")
        print(f"   tool_result is_error={'true' if result['is_error'] else 'false'}: {result['content']!r}")
        print(f"   next: {next_step(result)}")


if __name__ == "__main__":
    main()
