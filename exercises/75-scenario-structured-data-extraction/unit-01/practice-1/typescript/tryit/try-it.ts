// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { audit } from "./runAudit.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const policy = { target: 90, min_n: 3, gap: 5 };
const run = (kind = "typed", status = "valid", correct = true, invented = false, wasted = false) =>
  ({ id: "d", kind, status, correct, invented, retried_absent: wasted, sum_ok: true });

// A run of seven documents: three typed ones right, two scans, two handwritten ones that went wrong.
const runs = [run(), run(), run(), run("scanned"), run("scanned", "needs_review", false),
  run("handwritten", "failed", false, true), run("handwritten", "needs_review", false, false, true)];
const report = audit(runs, policy);
console.log("documents:", report.n, "| valid:", report.valid, "| needs review:", report.needs_review, "| failed:", report.failed);
console.log("accuracy, all vs validated:", report.accuracy_all, report.accuracy_validated);
console.log("segments:", JSON.stringify(report.segments));
console.log("first fix:", report.first_fix);
