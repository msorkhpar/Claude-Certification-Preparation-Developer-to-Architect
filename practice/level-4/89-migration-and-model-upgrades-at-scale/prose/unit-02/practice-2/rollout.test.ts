import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { TARGET, gate, migrateRequest, retirementStatus, rolloutStep } = await import(pathToFileURL(resolve(dir, "rollout.ts")).href);

type C = { id: string; segment: string; mustPass: boolean; oldOk: boolean; newOk: boolean; oldCost: number; newCost: number; newMs: number };
const mk = (id: string, segment: string, mustPass: boolean, oldOk: boolean, newOk: boolean, oldCost: number, newCost: number, newMs: number): C => ({ id, segment, mustPass, oldOk, newOk, oldCost, newCost, newMs });

/** Ten cases: billing a1, a2 (must pass), refund b1 (must pass) b2 b3, faq c1 to c5. By default b3 is lost and c4 is gained. */
function base(fail: Record<string, boolean> = {}): C[] {
  const rows: C[] = [mk("a1", "billing", true, true, true, 4, 5, 900), mk("a2", "billing", true, true, true, 4, 5, 1000), mk("b1", "refund", true, true, true, 6, 7, 1500),
    mk("b2", "refund", false, true, true, 6, 7, 1600), mk("b3", "refund", false, true, false, 6, 7, 1700)];
  for (let i = 1; i <= 5; i++) rows.push(mk(`c${i}`, "faq", false, i !== 4 && i !== 5, i !== 5, 2, 2, 550 + 50 * i));
  return rows.map((c) => (c.id in fail ? { ...c, newOk: fail[c.id] } : c));
}

const fixed = () => base({ b3: true });

function verdict(cases: C[], protectedSegments: string[], maxCostUp = 20, maxP95 = 2000): any {
  const result = gate(cases, protectedSegments, maxCostUp, maxP95);
  assert.ok(result !== null && typeof result === "object", "gate returned nothing");
  return result;
}

function lines(result: any): any {
  assert.ok(Array.isArray(result), "a list was expected");
  return result;
}

test("m1 a change that regresses nothing and stays inside its limits gets a go with no reasons", () => {
  assert.deepEqual(verdict(fixed(), ["refund"]), { decision: "go", reasons: [] });
});

test("e1 a must pass case that fails blocks the change and the ids are listed in order", () => {
  assert.deepEqual(verdict(base({ b3: true, a1: false }), []).reasons, ["must-pass failed: a1"]);
  assert.equal(verdict(base({ b3: true, a2: false, b1: false }), []).reasons[0], "must-pass failed: a2, b1");
});

test("e2 a protected segment that lost answers blocks the change even when gains elsewhere match the losses", () => {
  assert.deepEqual(verdict(base(), ["refund"]).reasons, ["protected segment lost answers: refund"]);
  assert.deepEqual(verdict(base(), []), { decision: "go", reasons: [] });
});

test("e3 more losses than gains blocks the change and both counts are named", () => {
  assert.deepEqual(verdict(base({ b3: true, c1: false, c2: false }), []), { decision: "no-go", reasons: ["net loss: lost 2, gained 1"] });
});

test("e4 a cost rise over the limit blocks the change and a rise exactly at the limit does not", () => {
  assert.equal(verdict(fixed(), [], 13).decision, "go");
  assert.deepEqual(verdict(fixed(), [], 12).reasons, ["cost up 13% over the 12% limit"]);
  const cheaper = fixed().map((c) => ({ ...c, newCost: 1 }));
  assert.equal(verdict(cheaper, [], 0).decision, "go");
});

test("e5 the tail is the nearest rank 95th percentile and a single slow case does not block", () => {
  const rows = Array.from({ length: 40 }, (_, k) => mk(`c${k + 1}`, "faq", false, true, true, 1, 1, k + 1 === 40 ? 9000 : 1000));
  assert.deepEqual(verdict(rows, [], 20, 2000), { decision: "go", reasons: [] });
  const slow = rows.map((r, k) => (k < 3 ? { ...r, newMs: 3000 } : r));
  assert.deepEqual(verdict(slow, [], 20, 2000).reasons, ["p95 latency 3000 ms over the 2000 ms limit"]);
});

test("e6 a roll out advances when healthy holds with too few requests and rolls back to zero when errors pass the limit", () => {
  assert.equal(rolloutStep(1, 2000, 6, 1000, 5), "advance to 5");
  assert.equal(rolloutStep(5, 300, 0, 1000, 5), "hold at 5");
  assert.equal(rolloutStep(5, 300, 300, 1000, 5), "hold at 5");
  assert.equal(rolloutStep(25, 50000, 400, 1000, 5), "rollback to 0");
  assert.equal(rolloutStep(25, 1000, 5, 1000, 5), "advance to 100");
  assert.equal(rolloutStep(100, 50000, 10, 1000, 5), "complete");
});

test("e7 the retirement calendar counts days ranks the nearest first and names the level", () => {
  const models = [["b", "2026-11-30", false], ["a", "2026-10-18", true], ["c", "2026-08-05", false], ["d", "2027-01-01", false], ["e", "2026-10-19", false]];
  assert.deepEqual(lines(retirementStatus(models, "2026-10-04")), ["c: -60 days, retired", "a: 14 days, urgent (tentative)", "e: 15 days, migrate now", "b: 57 days, migrate now", "d: 89 days, watch"]);
  assert.deepEqual(lines(retirementStatus([["z", "2026-12-03", false]], "2026-10-04")), ["z: 60 days, migrate now"]);
  assert.deepEqual(lines(retirementStatus([["z", "2026-12-04", false]], "2026-10-04")), ["z: 61 days, watch"]);
});

test("e8 migration removes the settings the new model refuses and names each change", () => {
  const old = { model: "claude-sonnet-4-5-20250929", temperature: 0.7, topP: 0.9, topK: 40, thinking: "disabled", toolChoice: "any", strict: false, prefill: true };
  const result = migrateRequest(old);
  assert.ok(Array.isArray(result), "migrateRequest returned nothing");
  const [next, changes] = result;
  assert.deepEqual(next, { model: TARGET, temperature: null, topP: null, topK: null, thinking: "between_tools", toolChoice: "auto", strict: true, prefill: false });
  assert.deepEqual(changes, ["model set to claude-sonnet-5-5", "removed temperature", "removed top_p", "removed top_k", "thinking disabled replaced by between_tools",
    "forced tool choice replaced by auto with strict tools", "assistant prefill removed; state the format in the instructions"]);
  const clean = { model: TARGET, temperature: null, topP: null, topK: null, thinking: "adaptive", toolChoice: "auto", strict: false, prefill: false };
  assert.deepEqual(migrateRequest(clean), [clean, []]);
  assert.equal(migrateRequest({ ...clean, thinking: "budget", toolChoice: "tool" })![0].thinking, "adaptive");
});
