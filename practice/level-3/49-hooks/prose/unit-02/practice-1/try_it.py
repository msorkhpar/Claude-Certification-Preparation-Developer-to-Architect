"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

import asyncio

from hooks import pre_refund


def check(amount):
    """Run the refund gate the way the Agent SDK does: with the event of one tool call about to run."""
    event = {"hook_event_name": "PreToolUse", "tool_name": "process_refund", "tool_input": {"amount": amount}}
    out = asyncio.run(pre_refund(event, "t1", None)) or {}
    return out.get("hookSpecificOutput", {})


# A small refund goes through, a middle one asks a person, a large one is denied.
for amount in (50, 350, 900):
    verdict = check(amount)
    print(f"refund of {amount}:", verdict.get("permissionDecision"), "-", verdict.get("permissionDecisionReason", ""))
