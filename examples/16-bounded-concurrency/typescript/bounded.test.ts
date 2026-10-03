import { test } from "node:test";
import assert from "node:assert/strict";
import Anthropic from "@anthropic-ai/sdk";
import { runAll } from "./bounded.ts";

test("unbounded runs everything at once", async () => {
  assert.equal((await runAll(null)).peak, 12);
});

test("a worker pool caps in-flight requests", async () => {
  assert.equal((await runAll(4)).peak, 4);
});

test("one failure does not cancel the others and order is kept", async () => {
  const { results } = await runAll(4);
  const r = results[6];
  assert.ok(r.status === "rejected" && r.reason instanceof Anthropic.RateLimitError);
  assert.deepEqual(results.filter((_, i) => i !== 6).map((x: any) => x.value), Array.from({ length: 12 }, (_, i) => i + 1).filter((n) => n !== 7).map((n) => `label for ticket ${n}`));
});
