"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from audit import audit

def step(tool, ok=True, right=None):
    return {"tool": tool, "ok": ok, **({"right_tool": right} if right else {})}

def session(steps, outcome="resolved", needs_human=False, refund=0):
    return {"id": "s", "steps": steps, "outcome": outcome, "needs_human": needs_human, "refund_cents": refund, "limit_cents": 10000}

# Sessions of a support agent: a clean one, one that skipped the customer check, and one escalated without need.
clean = [step("get_customer"), step("lookup_order"), step("process_refund")]
sessions = [
    session(clean, refund=2000),
    session([step("lookup_order"), step("get_customer"), step("process_refund")]),
    session([step("get_customer")], outcome="escalated"),
]
report = audit(sessions)
for key, value in report.items():
    print(f"{key}: {value}")
