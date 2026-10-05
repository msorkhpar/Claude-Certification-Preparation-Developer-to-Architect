"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from coordinator import coordinate

# The four model roles are plain functions, like the ones the tests script.
def planner(question):
    """Splits the question into three subtasks, each with the brief its subagent will get."""
    return {"delegate": True, "answer": None, "subtasks": [
        {"scope": "chips", "brief": "chips: find 2024 chip supply news"},
        {"scope": "cars", "brief": "cars: find 2024 car output news"},
        {"scope": "rates", "brief": "rates: find 2024 interest rates"}]}


def subagent(brief):
    """A subagent knows only its brief: here it just reports on the topic that starts it."""
    return brief.split(":")[0] + " report"


def reviewer(question, findings):
    return []  # no gaps: nothing more to ask


def synthesizer(question, findings):
    return " | ".join(f["text"] for f in findings)


result = coordinate(planner, subagent, reviewer, synthesizer, "How did supply change?") or {}

print("status:", result.get("status"), "| subagent calls:", result.get("subagent_calls"), "| rounds:", result.get("rounds"))
print("findings:", result.get("findings"))
print("answer:", result.get("answer"))
