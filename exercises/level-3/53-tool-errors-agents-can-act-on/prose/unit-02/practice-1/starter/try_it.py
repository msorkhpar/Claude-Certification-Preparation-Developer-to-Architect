"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from errors import ToolError, next_action, run_tool, to_tool_result

# A scripted tool, like the one the tests use: it times out twice (a transient failure), then works.
script = [ToolError("transient", "The billing service timed out after 5 s."),
          ToolError("transient", "The billing service timed out after 5 s."),
          "refund R-1 created"]
waits = []


def tool(args):
    step = script.pop(0)
    if isinstance(step, Exception):
        raise step
    return step


# The sleep is injected, so nothing really waits: it records the delays asked for.
result = run_tool(tool, {"order": "A-7", "amount": 40}, {"max_retries": 2, "base_delay_ms": 100}, waits.append) or {}

print("ok:", result.get("ok"), "| content:", result.get("content"), "| attempts:", result.get("attempts"))
print("waits (ms):", waits)
print("tool_result block:", to_tool_result("toolu_1", result))
print("next action:", next_action(result))
