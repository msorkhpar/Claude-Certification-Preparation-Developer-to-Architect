import { test } from "node:test";
import assert from "node:assert/strict";
import { Lcg, LOGITS, greedy, sample, softmax } from "./sampler.ts";

test("probabilities sum to one at any temperature", () => {
  for (const t of [0.1, 1.0, 5.0]) {
    assert.ok(Math.abs(softmax(LOGITS, t).reduce((a, b) => a + b, 0) - 1) < 1e-12);
  }
});

test("low temperature sharpens and high flattens", () => {
  const [low, mid, high] = [0.5, 1.0, 2.0].map((t) => softmax(LOGITS, t)[0]);
  assert.ok(low > mid && mid > high);
});

test("greedy ignores temperature", () => {
  assert.equal(greedy(softmax(LOGITS, 0.5)), 0);
  assert.equal(greedy(softmax(LOGITS, 2.0)), 0);
});

test("same seed gives the same draws", () => {
  const probs = softmax(LOGITS, 2.0);
  const draw = (seed: number) => {
    const r = new Lcg(seed);
    return Array.from({ length: 10 }, () => sample(probs, r));
  };
  assert.deepEqual(draw(7), draw(7));
  assert.notDeepEqual(draw(7), draw(8));
});
