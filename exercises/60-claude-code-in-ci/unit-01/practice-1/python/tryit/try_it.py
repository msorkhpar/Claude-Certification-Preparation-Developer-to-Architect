"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

import json
from review_gate import gate, review_prompt

# A small schema of the model's answer (your review-schema.json is the full one) and the team's policy.
SCHEMA = {"type": "object", "required": ["findings"], "properties": {"findings": {"type": "array", "items": {
    "type": "object", "required": ["file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"],
    "properties": {"file": {"type": "string"}, "line": {"type": "integer"}, "category": {"type": "string"},
                   "severity": {"type": "string", "enum": ["low", "medium", "high"]}, "issue": {"type": "string"},
                   "suggested_fix": {"type": "string"}, "detected_pattern": {"type": "string"}}}}}}
POLICY = {"min_severity": "medium", "disabled_categories": ["style"], "fail_on": ["high"]}

# What `claude -p --output-format json` prints for a successful run with one finding.
finding = {"file": "api.py", "line": 12, "category": "bug", "severity": "medium", "issue": "Unchecked None.",
           "suggested_fix": "Return early.", "detected_pattern": "missing-none-check"}
stdout = json.dumps({"type": "result", "subtype": "success", "is_error": False, "structured_output": {"findings": [finding]}})

print("a valid run:", gate(stdout, 0, SCHEMA, POLICY))
print("claude exited with 2:", gate(stdout, 2, SCHEMA, POLICY))
print("not JSON at all:", gate("Error: no key", 1, SCHEMA, POLICY))
print(review_prompt("+ x = 1").splitlines()[:3])
