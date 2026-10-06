"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from harness import run_eval

# Four test cases, each with its own automated check, like the first main test case.
cases = [
    {"id": "c1", "input": "I love it", "tags": ["core"], "check": {"type": "exact", "expected": "positive"}},
    {"id": "c2", "input": "awful", "tags": ["core"], "check": {"type": "exact", "expected": "negative"}},
    {"id": "c3", "input": "order 7", "tags": ["extract"], "check": {"type": "regex", "pattern": "ORD-\\d{4}"}},
    {"id": "c4", "input": "meh", "tags": ["core", "edge"], "check": {"type": "exact", "expected": "neutral"}},
]
answers = {"I love it": "positive", "awful": "negative", "order 7": "The order is ORD-0007.", "meh": "positive"}

# The application under test is a plain function: here it just looks the answer up.
report = run_eval(cases, lambda text: answers[text]) or {}

print("passed:", report.get("passed"), "of", report.get("total"), "| pass rate:", report.get("pass_rate"))
for result in report.get("results", []):
    print(" ", result["id"], "passed" if result["passed"] else "failed", "-", result["reason"])
