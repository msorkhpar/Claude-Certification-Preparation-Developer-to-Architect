import { test } from "node:test";
import assert from "node:assert/strict";
import type { Request } from "./rolloutGate.ts";
import { TARGET, gate, migrateRequest, retirementStatus, rolloutStep, suite } from "./rolloutGate.ts";

test("retirement status counts days and ranks by urgency", () => {
  const models: [string, string, boolean][] = [["b", "2026-11-30", false], ["a", "2026-10-15", true], ["c", "2026-08-05", false]];
  assert.deepEqual(retirementStatus(models, "2026-10-04"), ["c: -60 days, retired", "a: 11 days, urgent (tentative)", "b: 57 days, migrate now"]);
  assert.deepEqual(retirementStatus([["x", "2027-01-01", false]], "2026-10-04"), ["x: 89 days, watch"]);
});

test("migration removes what the new model refuses and keeps the rest", () => {
  const old: Request = { model: "claude-sonnet-4-5-20250929", temperature: 0.7, topP: 0.9, topK: null, thinking: "budget", toolChoice: "tool", strict: false, prefill: true };
  const [next, changes] = migrateRequest(old);
  assert.deepEqual(next, { model: TARGET, temperature: null, topP: null, topK: null, thinking: "adaptive", toolChoice: "auto", strict: true, prefill: false });
  assert.equal(changes.length, 6);
  assert.equal(changes[0], "model set to claude-sonnet-5-5");
  const clean: Request = { model: TARGET, temperature: null, topP: null, topK: null, thinking: "adaptive", toolChoice: "auto", strict: true, prefill: false };
  assert.deepEqual(migrateRequest(clean), [clean, []]);
});

test("the gate is go only when no check fails", () => {
  assert.deepEqual(gate(suite(), new Set(), 40, 2000), { decision: "go", reasons: [] });
  assert.deepEqual(gate(suite(), new Set(["refund"]), 25, 2000).reasons, ["protected segment lost answers: refund", "cost up 35% over the 25% limit"]);
});

test("a cost rise exactly at the limit passes", () => {
  assert.equal(gate(suite(), new Set(), 35, 2000).decision, "go");
  assert.deepEqual(gate(suite(), new Set(), 34, 2000).reasons, ["cost up 35% over the 34% limit"]);
});

test("the tail is the nearest rank 95th percentile", () => {
  assert.equal(gate(suite(), new Set(), 40, 1800).decision, "go");
  assert.deepEqual(gate(suite(), new Set(), 40, 1799).reasons, ["p95 latency 1800 ms over the 1799 ms limit"]);
});

test("a must-pass failure and a net loss are named", () => {
  const broken = suite().map((c) => (c.id === "b1" || c.id === "r1" ? { ...c, newOk: false } : c));
  const reasons = gate(broken, new Set(), 40, 2000).reasons;
  assert.equal(reasons[0], "must-pass failed: b1, r1");
  assert.equal(reasons[1], "net loss: lost 3, gained 2");
});

test("a rollout advances holds or rolls back", () => {
  assert.equal(rolloutStep(1, 2000, 6, 1000, 5), "advance to 5");
  assert.equal(rolloutStep(5, 300, 0, 1000, 5), "hold at 5");
  assert.equal(rolloutStep(25, 50000, 400, 1000, 5), "rollback to 0");
  assert.equal(rolloutStep(100, 50000, 10, 1000, 5), "complete");
  assert.equal(rolloutStep(25, 1000, 5, 1000, 5), "advance to 100");
});
