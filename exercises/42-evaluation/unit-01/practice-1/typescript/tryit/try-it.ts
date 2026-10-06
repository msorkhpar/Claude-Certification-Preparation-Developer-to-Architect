// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { runEval } from "./harness.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Four test cases, each with its own automated check, like the first main test case.
const cases: any[] = [
  { id: "c1", input: "I love it", tags: ["core"], check: { type: "exact", expected: "positive" } },
  { id: "c2", input: "awful", tags: ["core"], check: { type: "exact", expected: "negative" } },
  { id: "c3", input: "order 7", tags: ["extract"], check: { type: "regex", pattern: "ORD-\\d{4}" } },
  { id: "c4", input: "meh", tags: ["core", "edge"], check: { type: "exact", expected: "neutral" } },
];
const answers: Record<string, string> = { "I love it": "positive", awful: "negative", "order 7": "The order is ORD-0007.", meh: "positive" };

// The application under test is a plain function: here it just looks the answer up.
const report: any = runEval(cases, (text) => answers[text]) ?? {};

console.log("passed:", report.passed, "of", report.total, "| pass rate:", report.pass_rate);
for (const result of report.results ?? []) console.log(" ", result.id, result.passed ? "passed" : "failed", "-", result.reason);
