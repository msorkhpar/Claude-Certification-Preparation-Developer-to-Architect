// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { buildReviewPrompt, categoryReport } from "./reviewSpec.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A review specification like the tests use: explicit criteria first, then examples of both verdicts.
const spec = {
  criteria: [{
    id: "bug",
    report: "A comment whose claimed behaviour contradicts what the code does.",
    skip: "Minor style, naming and patterns the codebase already uses.",
    severity: { high: "A null dereference on a request path.", low: "A misleading variable name." },
  }],
  examples: [
    { verdict: "report", category: "bug", code: "total = price * qty  # sum of the line items", reason: "The comment says the line sums items, the code multiplies." },
    { verdict: "skip", code: "for i in range(n):  # loop", reason: "Terse and accurate." },
  ],
};
const prompt = buildReviewPrompt(spec, "+ x = 1");
console.log("prompt lines:", prompt ? prompt.split("\n").length : prompt);
console.log("first line:", prompt ? prompt.split("\n")[0] : null);
console.log("diff is last:", !!prompt && prompt.trimEnd().endsWith("</diff>"));

// Which categories to switch off, from what reviewers accepted or dismissed.
const finding = (verdict: string) => ({ category: "style", verdict, detected_pattern: "line-length" });
const findings = [finding("dismissed"), finding("dismissed"), finding("dismissed"), finding("dismissed"), finding("accepted")];
console.log("category report:", JSON.stringify(categoryReport(findings)));
