"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from batch_review import choose_api, resubmission_plan, review_plan, submission_interval

# How often to submit a batch so a 30-hour SLA still leaves room for the 24-hour window and 2 hours of handling.
print("interval for a 30 h SLA:", submission_interval(30))
print("API for a blocking check:", choose_api(True))
print("API for a nightly report:", choose_api(False))

# What to do with the results of a batch: (custom id, result kind) pairs and the size of each request.
results = [("a1", "succeeded"), ("big", "expired"), ("bad", "invalid_request"), ("late", "errored")]
print("resubmission plan:", resubmission_plan(results, {"big": 2000, "bad": 10, "late": 10}, 1000))
print("review passes:", review_plan(["a.py", "b.py", "c.py"]))
