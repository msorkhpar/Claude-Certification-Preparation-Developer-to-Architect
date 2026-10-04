import { test } from "node:test";
import assert from "node:assert/strict";
import { batchEntry, matchResults, reviewRequest, worstCaseWait } from "./batchAndReview.ts";

test("the worst wait is an interval plus the window plus the handling", () => {
  assert.deepEqual([worstCaseWait(4), worstCaseWait(6), worstCaseWait(1, 24, 0)], [30, 32, 25]);
});

test("a custom id is one to sixty four letters digits hyphens or underscores", () => {
  assert.equal(batchEntry("invoice-0042_a", {}).custom_id, "invoice-0042_a");
  for (const bad of ["", "has space", "dot.dot", "x".repeat(65)]) assert.throws(() => batchEntry(bad, {}));
  assert.equal(batchEntry("x".repeat(64), {}).custom_id, "x".repeat(64));
});

test("stream speed and a zero max tokens are refused", () => {
  for (const params of [{ stream: true }, { speed: "fast" }, { max_tokens: 0 }]) assert.throws(() => batchEntry("a1", params));
  assert.deepEqual(batchEntry("a1", { stream: false, max_tokens: 10 }).params, { stream: false, max_tokens: 10 });
});

test("results are paired by custom id whatever their order", () => {
  const [matched, orphans] = matchResults(["a1", "a2", "a3"], [["a2", "expired"], ["z9", "succeeded"], ["a1", "succeeded"]]);
  assert.deepEqual(matched, [["a1", "succeeded"], ["a2", "expired"], ["a3", "missing"]]);
  assert.deepEqual(orphans, ["z9"]);
});

test("an independent review request leaves the generators reasoning out", () => {
  assert.ok(reviewRequest("code", "because", false).includes("because"));
  const independent = reviewRequest("code", "because", true);
  assert.ok(!independent.includes("because") && independent.includes("<code>code</code>"));
});
