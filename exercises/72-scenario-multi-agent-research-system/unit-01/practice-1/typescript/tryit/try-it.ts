// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { synthesize } from "./synthesis.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const finding = (claim: string, value: string, source: string, date = "2025-01-01") => ({ claim, value, source, date });
const ok = (scope: string, ...findings: any[]) => ({ scope, status: "ok" as const, findings, error: null });

// Two subagents agree on claim X; a third scope timed out and is not covered.
const results = [
  ok("a", finding("X", "1", "s1"), finding("Y", "2", "s2")),
  ok("b", finding("X", "1", "s3")),
  { scope: "c", status: "error" as const, findings: [], error: { type: "timeout", query: "q-c", partial: [], alternatives: ["q-c-narrow"] } },
];
const report = synthesize(["a", "b", "c"], results);
console.log("status:", report.status);
console.log("covered:", report.covered, "| gaps:", report.gaps);
console.log("claims:", report.claims.map((c: any) => `${c.claim}=${c.value} (${c.sources.length} sources)`));
console.log("note:", report.note);
