import { test } from "node:test";
import assert from "node:assert/strict";
import { POOR, SPLIT, lint, overlap, page } from "./toolLint.ts";

test("the first set breaks the rules and overlaps", () => {
  assert.deepEqual(lint(POOR[0]), ["no-boundary", "no-use-when", "short-description"]);
  assert.deepEqual(lint(POOR[1]), ["no-boundary", "no-use-when", "param-undescribed", "short-description"]);
  assert.equal(overlap(POOR[0], POOR[1]).toFixed(2), "0.71");
});

test("the split set is clean and does not overlap", () => {
  assert.deepEqual(SPLIT.map(lint), [[], [], [], []]);
  let most = 0;
  SPLIT.forEach((a, i) => SPLIT.slice(i + 1).forEach((b) => { most = Math.max(most, overlap(a, b)); }));
  assert.ok(most < 0.6);
});

test("a page carries a cursor until the last row", () => {
  const rows = ["0", "1", "2", "3", "4", "5"];
  const [first, cursor, note] = page(rows, null, 4);
  assert.deepEqual(first, ["0", "1", "2", "3"]);
  assert.ok(cursor && String(note).startsWith("Showing 4 of 6"));
  const [second, next, last] = page(rows, cursor, 4);
  assert.deepEqual(second, ["4", "5"]);
  assert.ok(next === null && last === null);
});
