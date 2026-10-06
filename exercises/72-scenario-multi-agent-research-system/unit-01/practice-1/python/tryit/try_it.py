"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from synthesis import synthesize

def finding(claim, value, source, date="2025-01-01"):
    return {"claim": claim, "value": value, "source": source, "date": date}

def ok(scope, *findings):
    return {"scope": scope, "status": "ok", "findings": list(findings), "error": None}

# Two subagents agree on claim X; a third scope timed out and is not covered.
results = [
    ok("a", finding("X", "1", "s1"), finding("Y", "2", "s2")),
    ok("b", finding("X", "1", "s3")),
    {"scope": "c", "status": "error", "findings": [],
     "error": {"type": "timeout", "query": "q-c", "partial": [], "alternatives": ["q-c-narrow"]}},
]
report = synthesize(["a", "b", "c"], results)
print("status:", report["status"])
print("covered:", report["covered"], "| gaps:", report["gaps"])
print("claims:", [(c["claim"], c["value"], len(c["sources"])) for c in report["claims"]])
print("note:", report["note"])
