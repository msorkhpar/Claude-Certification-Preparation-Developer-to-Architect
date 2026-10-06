"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from gateway_budget import admit, delivery, route

POLICY = {"allowed": ["haiku", "sonnet"], "routes": {"classify": "haiku", "draft": "sonnet", "review": "opus"}, "default": "sonnet",
          "cheaper": {"opus": "sonnet", "sonnet": "haiku"}}

# The gateway first checks the team's budget, then routes the request: near the limit it picks a cheaper model.
for spend in (0, 850, 990):
    status = admit(spend, 1000, 100)
    print(f"spend {spend}/1000 -> {status}: a review request goes to", route({"task": "review"}, POLICY, status))

# A delivery check: does a 95th-percentile latency of 40 s leave 20 percent of margin under a 60 s timeout?
print("delivery:", delivery(40, 60, 20))
