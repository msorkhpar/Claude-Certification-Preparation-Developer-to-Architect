import { test } from "node:test";
import assert from "node:assert/strict";
import { abVerdict, buildCases, chooseModel, diagnose, percentile, segmentTable, shadowGate } from "./evalReport.ts";

test("the two versions have the same overall accuracy and different segments", () => {
  const rows = buildCases();
  assert.equal(rows.length, 52);
  const oldT = segmentTable(rows, "old");
  const newT = segmentTable(rows, "new");
  assert.equal(oldT.reduce((a, t) => a + t[2], 0), 48);
  assert.equal(newT.reduce((a, t) => a + t[2], 0), 48);
  assert.deepEqual(oldT[0], ["refund", 8, 5, 63, 60]);
  assert.deepEqual(newT[0], ["refund", 8, 7, 88, 20]);
});

test("percentile uses the nearest rank and needs no sorted input", () => {
  const values = [4800, 800, 1000, 900];
  assert.equal(percentile(values, 50), 900);
  assert.equal(percentile(values, 95), 4800);
  assert.equal(percentile([], 95), 0);
});

test("abVerdict names the better side only when it clears the bar", () => {
  assert.equal(abVerdict(410, 500, 438, 500), "new is better");
  assert.equal(abVerdict(438, 500, 410, 500), "old is better");
  assert.equal(abVerdict(410, 500, 425, 500), "no clear difference");
  assert.equal(abVerdict(82, 100, 90, 100), "too few cases");
  assert.equal(abVerdict(0, 300, 0, 300), "no clear difference");
});

test("the shadow gate holds for a protected regression or a net loss", () => {
  assert.deepEqual(shadowGate(buildCases(), new Set(["refund", "complaint"])), { decision: "hold", lost: 2, gained: 2, blocked: ["complaint"] });
  assert.equal(shadowGate(buildCases(), new Set(["refund"])).decision, "ship");
});

test("diagnose checks the evidence before the prompt and the model last", () => {
  assert.equal(diagnose(false, false, false, false), "retrieval or data");
  assert.equal(diagnose(true, true, false, true), "format instructions");
  assert.equal(diagnose(true, true, true, true), "model mismatch");
});

test("chooseModel takes the cheapest that meets both limits", () => {
  const options: [string, number, number, number][] = [["small", 84, 900, 1], ["medium", 91, 1800, 3], ["large", 95, 4200, 9]];
  assert.equal(chooseModel(options, 90, 2000), "medium");
  assert.equal(chooseModel(options, 94, 2000), "none");
});
