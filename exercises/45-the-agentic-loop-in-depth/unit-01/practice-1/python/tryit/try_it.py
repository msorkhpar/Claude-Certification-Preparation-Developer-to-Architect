"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from agent import run_agent


def lookup(arguments):
    return f"record {arguments['n']}"


# A scripted model, like the one the tests use: it asks for one tool call, then ends its turn.
replies = [
    {"stop_reason": "tool_use", "content": [{"type": "text", "text": "Looking."},
                                            {"type": "tool_use", "id": "t1", "name": "lookup", "input": {"n": 1}}]},
    {"stop_reason": "end_turn", "content": [{"type": "text", "text": "Record 1 found."}]},
]


def model(messages):
    print("model called with", len(messages), "messages")
    return replies.pop(0) if replies else {"stop_reason": "end_turn", "content": [{"type": "text", "text": "script ran out"}]}


result = run_agent(model, {"lookup": lookup}, "find record 1") or {}

print("status:", result.get("status"), "| turns:", result.get("turns"))
print("final text:", result.get("text"))
print("roles:", [m["role"] for m in result.get("messages", [])])
