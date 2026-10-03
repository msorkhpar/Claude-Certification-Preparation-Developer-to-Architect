import { test } from "node:test";
import assert from "node:assert/strict";
import { CacheSim, POLICY, stable, stampFirst, stampLast, tokens, usageOf } from "./cacheHits.ts";

test("the first call writes and the second reads the same prefix", async () => {
  const sim = new CacheSim();
  const first = await usageOf(sim, stable("a"));
  const second = await usageOf(sim, stable("b"), 60);
  assert.ok(first.cache_creation_input_tokens > 500 && first.cache_read_input_tokens === 0);
  assert.equal(second.cache_read_input_tokens, first.cache_creation_input_tokens);
  assert.equal(second.cache_creation_input_tokens, 0);
  assert.equal(tokens(POLICY), first.cache_creation_input_tokens);
});

test("a hit refreshes the five minute lifetime and silence loses it", async () => {
  const sim = new CacheSim();
  await usageOf(sim, stable("a"));
  assert.ok((await usageOf(sim, stable("b"), 250)).cache_read_input_tokens > 0);
  assert.ok((await usageOf(sim, stable("c"), 250)).cache_read_input_tokens > 0);
  assert.equal((await usageOf(sim, stable("d"), 400)).cache_read_input_tokens, 0);
});

test("a changing block before the breakpoint defeats the cache", async () => {
  const sim = new CacheSim();
  await usageOf(sim, stampFirst("10:01", "q"));
  assert.equal((await usageOf(sim, stampFirst("10:02", "q"), 60)).cache_read_input_tokens, 0);
});

test("the same block after the breakpoint costs nothing", async () => {
  const sim = new CacheSim();
  await usageOf(sim, stampLast("10:01", "q"));
  assert.ok((await usageOf(sim, stampLast("10:02", "q"), 60)).cache_read_input_tokens > 500);
});

test("a prefix under the minimum is not cached and no error is raised", async () => {
  const sim = new CacheSim(100000);
  const first = await usageOf(sim, stable("a"));
  assert.ok(first.cache_creation_input_tokens === 0 && first.input_tokens > 500);
});
