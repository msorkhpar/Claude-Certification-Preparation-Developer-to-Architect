"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from router import NoModelError, request_cost, route

# Two of the course models, with prices in dollars per million tokens, as the tests use them.
HAIKU = {"id": "claude-haiku-4-5-20251001", "tier": 1, "context": 200_000, "max_output": 64_000, "input": 1, "output": 5, "cache_read_multiplier": 0.1}
SONNET = {"id": "claude-sonnet-5-5", "tier": 2, "context": 1_000_000, "max_output": 128_000, "input": 2, "output": 10, "cache_read_multiplier": 0.1}
catalog = [HAIKU, SONNET]
usage = {"input_tokens": 1200, "output_tokens": 300}

print("sonnet cost (micro-dollars):", request_cost(SONNET, usage))
print("haiku cost (micro-dollars):", request_cost(HAIKU, usage))
try:
    print("tier 1 task goes to:", route(catalog, {"min_tier": 1, "usage": usage}))
    print("tier 2 task goes to:", route(catalog, {"min_tier": 2, "usage": usage}))
except NoModelError as err:
    print("no model:", err)
