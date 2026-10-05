"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from params import RejectedRequest, build_params

SONNET, HAIKU = "claude-sonnet-5-5", "claude-haiku-4-5-20251001"

# What the application wants, turned into the request parameters one model accepts.
print("sonnet adaptive:", build_params(SONNET, 4096, {"thinking": {"type": "adaptive"}, "effort": "medium"}))
print("haiku budget:", build_params(HAIKU, 4096, {"thinking": {"type": "enabled", "budget_tokens": 2048}}))

# A combination the API would answer with a 400 is refused, naming the parameter.
try:
    print("haiku with effort:", build_params(HAIKU, 4096, {"effort": "high"}))
except RejectedRequest as err:
    print("refused:", err.param)
