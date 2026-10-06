"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from ledger import check_finding, coverage_note, merge, render

# Findings from two subagents: the same claim from two sources, and one that disagrees.
findings = [
    {"claim": "revenue 2023", "value": "4.1B", "source": "Annual report", "date": "2024-02-01"},
    {"claim": "revenue 2023", "value": "4.1B", "source": "Press release", "date": "2024-02-03"},
    {"claim": "headcount", "value": "910", "source": "Press release", "date": "2024-03-01"},
    {"claim": "headcount", "value": "950", "source": "Blog", "date": "2024-03-01"},
]
print("missing fields:", check_finding({"claim": "headcount", "value": "910"}))

merged = merge(findings)
for entry in merged:
    print("entry:", entry["claim"], entry["status"], entry["values"])

# What the report says about coverage, and how an entry is shown.
print("coverage:", coverage_note(["revenue 2023", "headcount", "patents"], merged, {"patents": "the registry timed out"}))
print("rendered:", render(merged[0], "news"))
