import { test } from "node:test";
import assert from "node:assert/strict";
import { breakEven, brief, pct, segmentReport, slaLine } from "./tradeoffBrief.ts";

test("percent rounds halves up and survives no cases", () => {
  assert.deepEqual([pct(1, 2), pct(1, 8), pct(1, 3), pct(2, 3)], [50, 13, 33, 67]);
  assert.equal(pct(0, 0), 0);
});

test("break even is where the expected error cost equals the check", () => {
  assert.equal(breakEven(250, 5), 98);
  assert.equal(breakEven(60, 5), 91);
  assert.equal(breakEven(12, 5), 58);
});

test("a service level is met exactly at its edge", () => {
  const latency = { name: "p95 latency", limit: 2000, direction: "max", unit: "ms" };
  assert.ok(slaLine(latency, 2000).endsWith("met"));
  assert.equal(slaLine(latency, 2001), "p95 latency: 2001 ms against a limit of 2000 ms: missed by 1 ms");
  const floor = { name: "availability", limit: 995, direction: "min", unit: "per mille" };
  assert.ok(slaLine(floor, 995).endsWith("met"));
  assert.ok(slaLine(floor, 994).endsWith("missed by 1 per mille"));
});

test("the report lists the costliest segment first and checks below the break even", () => {
  const lines = segmentReport([{ name: "status", right: 98, total: 100, errorCost: 12 }, { name: "credit", right: 63, total: 100, errorCost: 250 }], 5);
  assert.ok(lines[0].startsWith("credit: 63 percent right") && lines[0].endsWith("reviewed (break-even 98)"));
  assert.equal(lines[1], "status: 98 percent right, error cost 12, auto (break-even 58)");
});

test("the two audiences get the same facts in different words", () => {
  const weakest = { name: "credit", right: 63, total: 100, errorCost: 250 };
  const sponsor = brief("sponsor", "Routing", 80000, 315000, weakest, "approve the pilot");
  const engineer = brief("engineer", "Routing", 80000, 315000, weakest, "approve the pilot");
  assert.ok(sponsor.includes("a saving of 235,000") && sponsor.includes("Decision asked: approve the pilot."));
  assert.ok(engineer.includes("saving=235000") && !engineer.toLowerCase().includes("decision"));
});
