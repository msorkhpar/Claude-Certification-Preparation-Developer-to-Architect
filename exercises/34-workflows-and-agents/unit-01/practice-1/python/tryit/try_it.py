"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from workflows import orchestrate

PLAN = '["research the topic", "draft the outline", "check the facts"]'


def model(prompt):
    """A stand-in for the model, like the one the tests script: the start of the prompt says which step is asking."""
    if prompt.startswith("Plan"):
        return PLAN
    if prompt.startswith("Subtask"):
        return "done: " + prompt.split("\n")[0][len("Subtask: "):]
    return "FINAL"


# The orchestrator asks for a plan, runs one worker per subtask, then combines the results.
result = orchestrate(model, "Write a guide") or {}

print("status:", result.get("status"), "| fallback:", result.get("fallback"), "| calls:", result.get("calls"))
print("plan:", result.get("plan"))
print("results:", [(r["subtask"], r["status"]) for r in result.get("results", [])])
print("answer:", result.get("answer"))
