"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from review_spec import build_review_prompt, category_report

# A review specification like the tests use: explicit criteria first, then examples of both verdicts.
spec = {
    "criteria": [{
        "id": "bug",
        "report": "A comment whose claimed behaviour contradicts what the code does.",
        "skip": "Minor style, naming and patterns the codebase already uses.",
        "severity": {"high": "A null dereference on a request path.", "low": "A misleading variable name."},
    }],
    "examples": [
        {"verdict": "report", "category": "bug", "code": "total = price * qty  # sum of the line items",
         "reason": "The comment says the line sums items, the code multiplies."},
        {"verdict": "skip", "code": "for i in range(n):  # loop", "reason": "Terse and accurate."},
    ],
}
prompt = build_review_prompt(spec, "+ x = 1")
print("prompt lines:", len(prompt.splitlines()) if prompt else prompt)
print("first line:", prompt.splitlines()[0] if prompt else None)
print("diff is last:", bool(prompt) and prompt.rstrip().endswith("</diff>"))

# Which categories to switch off, from what reviewers accepted or dismissed.
findings = [{"category": "style", "verdict": "dismissed", "detected_pattern": "line-length"}] * 4 + \
           [{"category": "style", "verdict": "accepted", "detected_pattern": "line-length"}]
print("category report:", category_report(findings))
