// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { gate, reviewPrompt } from "./reviewGate.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A small schema of the model's answer (your review-schema.json is the full one) and the team's policy.
const schema = { type: "object", required: ["findings"], properties: { findings: { type: "array", items: {
  type: "object", required: ["file", "line", "category", "severity", "issue", "suggested_fix", "detected_pattern"],
  properties: { file: { type: "string" }, line: { type: "integer" }, category: { type: "string" },
    severity: { type: "string", enum: ["low", "medium", "high"] }, issue: { type: "string" },
    suggested_fix: { type: "string" }, detected_pattern: { type: "string" } } } } } };
const policy = { min_severity: "medium", disabled_categories: ["style"], fail_on: ["high"] };

// What `claude -p --output-format json` prints for a successful run with one finding.
const finding = { file: "api.py", line: 12, category: "bug", severity: "medium", issue: "Unchecked None.",
  suggested_fix: "Return early.", detected_pattern: "missing-none-check" };
const stdout = JSON.stringify({ type: "result", subtype: "success", is_error: false, structured_output: { findings: [finding] } });

console.log("a valid run:", JSON.stringify(gate(stdout, 0, schema, policy)));
console.log("claude exited with 2:", JSON.stringify(gate(stdout, 2, schema, policy)));
console.log("not JSON at all:", JSON.stringify(gate("Error: no key", 1, schema, policy)));
console.log(reviewPrompt("+ x = 1").split("\n").slice(0, 3));
