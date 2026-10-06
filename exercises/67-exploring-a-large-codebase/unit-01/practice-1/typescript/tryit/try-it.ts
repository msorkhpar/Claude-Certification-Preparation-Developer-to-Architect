// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { addFinding, buildManifest, compactCommand, renderScratchpad, resumePlan } from "./recovery.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// The scratchpad: one line per finding, a fact is recorded once per area.
let findings: any[] = addFinding([], "auth", "tokens are signed in TokenSigner", "auth/TokenSigner.java:12");
findings = addFinding(findings, "billing", "invoices use cents", "billing/Money.java:5");
findings = addFinding(findings, "auth", "tokens are signed in TokenSigner", "auth/Other.java:99");
console.log(renderScratchpad(findings));

// The manifest of the subagents, and what to do with each after a crash.
const agents = [
  { name: "search", state_file: "state/search.md", status: "running" },
  { name: "auth", state_file: "state/auth.md", status: "done" },
  { name: "billing", state_file: "state/billing.md", status: "failed" },
];
const manifest = buildManifest(agents);
console.log("manifest:", JSON.stringify(manifest));
console.log("resume plan:", JSON.stringify(resumePlan(manifest, new Set(["state/auth.md", "state/search.md"]))));
console.log("compact command:", compactCommand(["the open questions", "file paths"]));
