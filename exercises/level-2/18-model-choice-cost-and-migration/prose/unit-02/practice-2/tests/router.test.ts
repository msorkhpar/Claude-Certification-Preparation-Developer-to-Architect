import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { NoModelError, requestCost, route } = await import(pathToFileURL(resolve(dir, "router.ts")).href);

// Prices in dollars per million tokens, read from the pricing page on 2026-10-02 (a dollar per MTok is a micro-dollar per token).
const HAIKU = { id: "claude-haiku-4-5-20251001", tier: 1, context: 200_000, max_output: 64_000, input: 1, output: 5, cache_read_multiplier: 0.1 };
const SONNET = { id: "claude-sonnet-5-5", tier: 2, context: 1_000_000, max_output: 128_000, input: 2, output: 10, cache_read_multiplier: 0.1 };
const OPUS = { id: "claude-opus-5-5", tier: 3, context: 1_000_000, max_output: 128_000, input: 4, output: 20, cache_read_multiplier: 0.05 };
const FABLE = { id: "claude-fable-5-1", tier: 4, context: 1_000_000, max_output: 128_000, input: 10, output: 50, cache_read_multiplier: 0.025 };
const CATALOG = [HAIKU, SONNET, OPUS, FABLE];

/** The error fn throws, or null. */
function raised(fn: () => unknown): unknown {
  try {
    fn();
  } catch (err) {
    return err;
  }
  return null;
}

test("m1 cost of a plain request and the cheapest model that meets the tier", () => {
  assert.equal(requestCost(SONNET, { input_tokens: 1200, output_tokens: 300 }), 5400);
  assert.equal(requestCost(HAIKU, { input_tokens: 1000, output_tokens: 200 }), 2000);
  assert.equal(route(CATALOG, { min_tier: 1, usage: { input_tokens: 1000, output_tokens: 200 } }), HAIKU.id);
  assert.equal(route(CATALOG, { min_tier: 2, usage: { input_tokens: 1000, output_tokens: 200 } }), SONNET.id);
});

test("e1 cache reads and writes are priced by their own multipliers", () => {
  const usage = {
    input_tokens: 100, output_tokens: 50, cache_read_input_tokens: 4000, cache_creation_input_tokens: 3000,
    cache_creation: { ephemeral_5m_input_tokens: 1000, ephemeral_1h_input_tokens: 2000 },
  };
  assert.equal(requestCost(OPUS, usage), 23200); // 400 + 5000 + 16000 + 800 + 1000
  assert.equal(requestCost(FABLE, { cache_read_input_tokens: 1_000_000 }), 250000);
  assert.equal(requestCost(SONNET, { cache_read_input_tokens: 1_000_000 }), 200000);
  // without the per-lifetime split, the written tokens are the 5-minute kind
  assert.equal(requestCost(SONNET, { cache_creation_input_tokens: 3000 }), 7500);
});

test("e2 the batch discount halves every part of the cost", () => {
  const usage = {
    input_tokens: 100, output_tokens: 50, cache_read_input_tokens: 4000,
    cache_creation: { ephemeral_5m_input_tokens: 1000, ephemeral_1h_input_tokens: 2000 },
  };
  assert.equal(requestCost(OPUS, usage, true), 11600);
  assert.equal(requestCost(OPUS, usage, true), requestCost(OPUS, usage) / 2);
  assert.equal(requestCost(HAIKU, { output_tokens: 1000 }, true), 2500);
});

test("e3 the router picks by cost and tier not by the order of the catalog", () => {
  const rev = [...CATALOG].reverse();
  assert.equal(route(rev, { min_tier: 3, usage: { input_tokens: 500, output_tokens: 100 } }), OPUS.id);
  assert.equal(route(rev, { min_tier: 1, usage: { input_tokens: 500, output_tokens: 100 } }), HAIKU.id);
  assert.equal(route([FABLE, OPUS], { min_tier: 1, usage: { input_tokens: 500, output_tokens: 100 } }), OPUS.id);
  // batch changes every price by the same factor, so it never changes the winner
  assert.equal(route(CATALOG, { min_tier: 2, batch: true, usage: { input_tokens: 500 } }), SONNET.id);
});

test("e4 a model whose context or output limit is too small is skipped", () => {
  const big = { min_tier: 1, usage: { input_tokens: 250_000, cache_read_input_tokens: 50_000, output_tokens: 100 } };
  assert.equal(route(CATALOG, big), SONNET.id);
  assert.equal(route(CATALOG, { min_tier: 1, max_tokens: 100_000, usage: { input_tokens: 100, output_tokens: 100 } }), SONNET.id);
  assert.equal(route(CATALOG, { min_tier: 1, max_tokens: 64_000, usage: { input_tokens: 100 } }), HAIKU.id);
});

test("e5 deprecated models are skipped and an empty choice raises", () => {
  const old = { ...HAIKU, deprecated: true };
  assert.equal(route([old, SONNET], { min_tier: 1, usage: { input_tokens: 100 } }), SONNET.id);
  assert.ok(raised(() => route([old], { min_tier: 1, usage: { input_tokens: 100 } })) instanceof NoModelError);
  assert.ok(raised(() => route(CATALOG, { min_tier: 5, usage: { input_tokens: 100 } })) instanceof NoModelError);
  assert.ok(raised(() => route(CATALOG, { min_tier: 1, usage: { input_tokens: 2_000_000 } })) instanceof NoModelError);
});

test("e6 a tie goes to the lower tier", () => {
  const reads = { min_tier: 2, usage: { cache_read_input_tokens: 1_000_000 } };
  assert.equal(requestCost(SONNET, reads.usage), 200000);
  assert.equal(requestCost(OPUS, reads.usage), 200000);
  assert.equal(route(CATALOG, reads), SONNET.id);
  assert.equal(route([...CATALOG].reverse(), reads), SONNET.id);
});
