"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from cacheplan import PlanError, plan_request


def block(id, section, tokens, **extra):
    return {"id": id, "section": section, "tokens": tokens, **extra}


# A request whose blocks arrive in the wrong order: the volatile date sits first, in the system prompt.
blocks = [
    block("date", "system", 20, volatile=True),
    block("tools", "tools", 2000),
    block("rules", "system", 3000, breakpoint=True),
    block("manual", "messages", 6000, breakpoint=True),
    block("question", "messages", 40),
]
try:
    plan = plan_request(blocks, min_tokens=1024) or []
    print("order:", [p["id"] for p in plan])
    print("cache per block:", {p["id"]: p["cache"] for p in plan})
    print("breakpoints at:", [p["id"] for p in plan if p["cache"]])
except PlanError as err:
    print("plan error:", err)
