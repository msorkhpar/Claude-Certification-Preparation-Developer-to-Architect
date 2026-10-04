import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { abVerdict, chooseModel, diagnose, percentile, segmentTable, shadowGate } = await import(pathToFileURL(resolve(dir, "evalkit.ts")).href);

const COSTS = { "order status": 1, refund: 20, policy: 5, complaint: 10 };

function rows(...groups: [string, number, number][]): [string, boolean][] {
  return groups.flatMap(([segment, total, right]) => Array.from({ length: total }, (_, i): [string, boolean] => [segment, i < right]));
}

function table(results: [string, boolean][], costs: Record<string, number>): any {
  const result = segmentTable(results, costs);
  assert.ok(Array.isArray(result), "segmentTable returned nothing");
  return result;
}

function gate(pairs: any[], protectedSegments: string[]): any {
  const result = shadowGate(pairs, protectedSegments);
  assert.ok(result !== null && typeof result === "object", "shadowGate returned nothing");
  return result;
}

const rep = (n: number, pair: [string, boolean, boolean]): [string, boolean, boolean][] => Array.from({ length: n }, () => pair);

test("m1 a segment table reports accuracy and error cost with the costliest segment first", () => {
  const results = rows(["order status", 30, 30], ["refund", 8, 5], ["policy", 10, 9], ["complaint", 4, 4]);
  assert.deepEqual(table(results, COSTS), [["refund", 8, 5, 63, 60], ["policy", 10, 9, 90, 5], ["complaint", 4, 4, 100, 0], ["order status", 30, 30, 100, 0]]);
});

test("e1 a segment with no cost entry costs one per error and no results give an empty table", () => {
  assert.deepEqual(table(rows(["odd", 3, 1]), COSTS), [["odd", 3, 1, 33, 2]]);
  assert.deepEqual(table([], COSTS), []);
  assert.deepEqual(table(rows(["b", 2, 1], ["a", 2, 1]), { a: 1, b: 1 }), [["a", 2, 1, 50, 1], ["b", 2, 1, 50, 1]]);
});

test("e2 a percentile uses the nearest rank and does not need sorted input", () => {
  const values = [4800, 800, 1000, 900];
  assert.equal(percentile(values, 50), 900);
  assert.equal(percentile(values, 95), 4800);
  assert.equal(percentile(values, 100), 4800);
  assert.equal(percentile(values, 1), 800);
  assert.equal(percentile([7], 50), 7);
  assert.equal(percentile([], 50), 0);
});

test("e3 a test with fewer cases than the minimum in either arm decides nothing", () => {
  assert.equal(abVerdict(150, 199, 190, 400), "too few cases");
  assert.equal(abVerdict(150, 400, 190, 199), "too few cases");
  assert.equal(abVerdict(100, 200, 160, 200), "new is better");
  assert.equal(abVerdict(10, 100, 19, 100, 50), "no clear difference");
  assert.equal(abVerdict(10, 100, 19, 100), "too few cases");
});

test("e4 a difference is called only when it clears the 95 percent bar and the better side is named", () => {
  assert.equal(abVerdict(410, 500, 438, 500), "new is better");
  assert.equal(abVerdict(438, 500, 410, 500), "old is better");
  assert.equal(abVerdict(410, 500, 431, 500), "no clear difference");
  assert.equal(abVerdict(0, 300, 0, 300), "no clear difference");
  assert.equal(abVerdict(300, 300, 300, 300), "no clear difference");
});

test("e5 a shadow run is held for a regression in a protected segment or for more losses than gains", () => {
  const pairs = [...rep(2, ["refund", false, true]), ["policy", true, false], ["complaint", true, false], ...rep(5, ["policy", true, true])];
  assert.deepEqual(gate(pairs, ["refund", "complaint"]), { decision: "hold", lost: 2, gained: 2, blocked: ["complaint"] });
  assert.deepEqual(gate(pairs, ["refund"]), { decision: "ship", lost: 2, gained: 2, blocked: [] });
  const worse = [...rep(3, ["policy", true, false]), ...rep(2, ["policy", false, true])];
  assert.deepEqual(gate(worse, []), { decision: "hold", lost: 3, gained: 2, blocked: [] });
});

test("e6 diagnosis checks the evidence then the grounding then the format then the stronger model", () => {
  assert.equal(diagnose(false, false, false, false), "retrieval or data");
  assert.equal(diagnose(true, false, false, false), "ungrounded answer");
  assert.equal(diagnose(true, true, false, false), "format instructions");
  assert.equal(diagnose(true, true, true, false), "prompt or task");
  assert.equal(diagnose(true, true, true, true), "model mismatch");
});

test("e7 model choice takes the cheapest option that meets the accuracy floor and the latency limit", () => {
  const options = [["small", 84, 900, 1], ["medium", 91, 1800, 3], ["large", 95, 4200, 9]];
  assert.equal(chooseModel(options, 90, 2000), "medium");
  assert.equal(chooseModel(options, 94, 2000), "none");
  assert.equal(chooseModel(options, 91, 1800), "medium");
  assert.equal(chooseModel(options, 80, 5000), "small");
  assert.equal(chooseModel([["b", 90, 100, 2], ["a", 90, 100, 2]], 90, 100), "a");
});
