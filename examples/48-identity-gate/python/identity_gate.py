"""The same scripted model, which skips identity verification, run against a loop that trusts the prompt and a loop that enforces the prerequisite in code.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The system prompt asks for
verification first in both runs: what differs is whether the code that runs the tools checks it.
"""
from harness import scripted_client
from harness.scripted import message, text, tool_use
import logging

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
SYSTEM = "You are a support agent. Verify the customer's identity before any refund."
TOOLS = [{"name": n, "description": d, "input_schema": {"type": "object", "properties": p, "required": list(p)}} for n, d, p in [
    ("verify_identity", "Check the customer's identity code. Use it before any order or refund action.", {"code": {"type": "string"}}),
    ("lookup_order", "Look up one order of the verified customer.", {"order_id": {"type": "string"}}),
    ("process_refund", "Refund an amount in cents on a looked-up order.", {"order_id": {"type": "string"}, "amount_cents": {"type": "integer"}})]]


class Backend:
    def __init__(self):
        self.log, self.verified = [], False

    def call(self, name, args):
        self.log.append(name)
        if name == "verify_identity":
            self.verified = args["code"] == "1234"
            return "verified=yes" if self.verified else "verified=no"
        return {"lookup_order": "order_id=O1; total_cents=5000", "process_refund": "refund_id=R1"}[name]


def gate(backend, name, args):
    """The prerequisite, in code: nothing but verification runs before the customer is verified."""
    if name != "verify_identity" and not backend.verified:
        return "BLOCKED identity_required: Verify the customer's identity before this action.", True
    return backend.call(name, args), False


def run(client, backend, gated):
    messages = [{"role": "user", "content": "Please refund 20.00 on order O1. My code is 1234."}]
    results = []
    while True:
        reply = client.messages.create(model=MODEL, max_tokens=500, system=SYSTEM, tools=TOOLS, messages=messages)
        messages.append({"role": "assistant", "content": reply.content})
        if reply.stop_reason != "tool_use":
            return results
        out = []
        for block in reply.content:
            if block.type == "tool_use":
                content, is_error = gate(backend, block.name, block.input) if gated else (backend.call(block.name, block.input), False)
                results.append((block.name, content, is_error))
                out.append({"type": "tool_result", "tool_use_id": block.id, "content": content, **({"is_error": True} if is_error else {})})
        messages.append({"role": "user", "content": out})


def replies(gated):
    first = message([text("Refunding now."), tool_use("toolu_01", "process_refund", order_id="O1", amount_cents=2000)], stop_reason="tool_use", model=MODEL)
    if not gated:
        return [first, message([text("Refund issued.")], model=MODEL)]
    return [first,
            message([tool_use("toolu_02", "verify_identity", code="1234")], stop_reason="tool_use", model=MODEL),
            message([tool_use("toolu_03", "lookup_order", order_id="O1")], stop_reason="tool_use", model=MODEL),
            message([tool_use("toolu_04", "process_refund", order_id="O1", amount_cents=2000)], stop_reason="tool_use", model=MODEL),
            message([text("Refund issued after verification.")], model=MODEL)]


def main():
    print("system prompt in both runs:", SYSTEM)
    for label, gated in (("prompt only", False), ("code gate  ", True)):
        client, transport = scripted_client(*replies(gated))
        backend = Backend()
        results = run(client, backend, gated)
        before = backend.log.index("process_refund") < backend.log.index("verify_identity") if "verify_identity" in backend.log else "process_refund" in backend.log
        print(f"{label}: backend calls = {backend.log}; refund before verification: {before}")
        if gated:
            print(f"{label}: first result sent back to the model: {results[0][1]} (is_error={results[0][2]})")
        assert all(r["system"] == SYSTEM for r in transport.requests)


if __name__ == "__main__":
    main()
