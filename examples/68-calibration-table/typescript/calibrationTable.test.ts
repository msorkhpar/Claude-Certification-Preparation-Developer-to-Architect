import { test } from "node:test";
import assert from "node:assert/strict";
import { accuracyByType, calibrationTable, makeRecords, overallAccuracy, precisionAbove, ratio, type Rec } from "./calibrationTable.ts";

const SMALL: Rec[] = [["a", 95, true], ["a", 95, true], ["b", 55, false], ["b", 55, true]];

test("ratio rounds half up and survives an empty whole", () => {
  assert.deepEqual([ratio(1, 2), ratio(1, 3), ratio(2, 3), ratio(0, 0)], [50, 33, 67, 0]);
});

test("accuracy is reported overall and per type", () => {
  assert.equal(overallAccuracy(SMALL), 75);
  assert.deepEqual(accuracyByType(SMALL), [["a", 2, 2, 100], ["b", 1, 2, 50]]);
});

test("the table compares what a bucket claimed with how often it was right", () => {
  assert.deepEqual(calibrationTable(SMALL), [["50-59", 2, 55, 50], ["90-100", 2, 95, 100]]);
});

test("precision above counts what would skip review", () => {
  assert.deepEqual([precisionAbove(SMALL, 90), precisionAbove(SMALL, 50), precisionAbove(SMALL, 99)], [[2, 100], [4, 75], [0, 0]]);
});

test("the generated records hide a weak type behind the overall figure", () => {
  const records = makeRecords();
  const byType = Object.fromEntries(accuracyByType(records).map(([t, , , p]) => [t, p]));
  assert.equal(records.length, 200);
  assert.ok(byType["handwritten"] < overallAccuracy(records) && overallAccuracy(records) < byType["invoice"]);
  assert.equal(calibrationTable(records).reduce((sum, row) => sum + row[1], 0), 200);
});
