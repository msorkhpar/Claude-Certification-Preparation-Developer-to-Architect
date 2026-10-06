"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from capability import audit, gateway, plan_loading

CATALOG = {
    "read_ticket": {"access": "read", "tokens": 160},
    "draft_reply": {"access": "draft", "tokens": 220},
    "issue_refund": {"access": "money", "tokens": 240},
    "export_report": {"access": "read", "tokens": 300},
}

# An agent holds more tools than its role needs: the audit names what to remove and which of them are risky.
agent = {"holds": list(CATALOG), "needs": ["read_ticket", "draft_reply"], "used": {"read_ticket": 12, "draft_reply": 9}}
print("audit:", audit(agent, CATALOG))

# Loading a long tool list: the most used tools load now, the rest wait behind a search tool.
tools = {f"t{i:02d}": 100 for i in range(1, 8)}
print("loading plan:", plan_loading(tools, {"t01": 9, "t02": 5}, keep=3))

# The gateway checks one request against the policy.
policy = {"credentials": {"k1": "support"}, "models": {"support": {"standard"}}, "tools": {"support": {"read_ticket", "draft_reply"}},
          "limits": {"support": 2}, "routes": {"standard": "claude-sonnet-5-5"}}
print("allowed:", gateway({"credential": "k1", "model": "standard", "tool": "read_ticket", "recent": 0}, policy))
print("over the limit:", gateway({"credential": "k1", "model": "standard", "tool": "read_ticket", "recent": 2}, policy))
