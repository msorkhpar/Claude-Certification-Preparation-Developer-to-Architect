// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { checkFinding, coverageNote, merge, render } from "./ledger.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Findings from two subagents: the same claim from two sources, and one that disagrees.
const findings = [
  { claim: "revenue 2023", value: "4.1B", source: "Annual report", date: "2024-02-01" },
  { claim: "revenue 2023", value: "4.1B", source: "Press release", date: "2024-02-03" },
  { claim: "headcount", value: "910", source: "Press release", date: "2024-03-01" },
  { claim: "headcount", value: "950", source: "Blog", date: "2024-03-01" },
];
console.log("missing fields:", checkFinding({ claim: "headcount", value: "910" }));

const merged = merge(findings);
for (const entry of merged) console.log("entry:", entry.claim, entry.status, JSON.stringify(entry.values));

// What the report says about coverage, and how an entry is shown.
console.log("coverage:", JSON.stringify(coverageNote(["revenue 2023", "headcount", "patents"], merged, { patents: "the registry timed out" })));
console.log("rendered:", render(merged[0], "news"));
