import { test } from "node:test";
import assert from "node:assert/strict";
import { MODEL, PARAMS, USAGE, clientFor, cost } from "./cost.ts";
import { message, text } from "../../../harness/ts/scriptedFetch.ts";

const close = (a: number, b: number) => assert.ok(Math.abs(a - b) < 1e-9, `${a} vs ${b}`);

async function usageOf(changes: Record<string, unknown> = {}) {
  const { client } = clientFor(message([text("x")], "end_turn", { ...USAGE, ...changes } as any));
  return (await client.messages.create(PARAMS)).usage;
}

test("cost adds input, cache write and output", async () => {
  close(cost(MODEL, await usageOf()), (240 + 10000 + 3400) / 1_000_000);
});

test("a cache read is a fraction of the input price that depends on the model", async () => {
  const usage = await usageOf({ cache_read_input_tokens: 1_000_000, cache_creation_input_tokens: 0, cache_creation: { ephemeral_5m_input_tokens: 0, ephemeral_1h_input_tokens: 0 }, output_tokens: 0, input_tokens: 0 });
  close(cost("claude-sonnet-5-5", usage), 0.2);
  close(cost("claude-opus-5-5", usage), 0.2);
  close(cost("claude-fable-5-1", usage), 0.25);
  close(cost("claude-haiku-4-5-20251001", usage), 0.1);
});

test("batch halves the whole cost", async () => {
  const usage = await usageOf();
  close(cost(MODEL, usage, true), cost(MODEL, usage) / 2);
});

test("count_tokens is a separate call to its own path", async () => {
  const { fake, client } = clientFor({ input_tokens: 99 });
  assert.equal((await client.messages.countTokens({ model: MODEL, messages: PARAMS.messages })).input_tokens, 99);
  assert.ok(fake.seen[0].url.endsWith("/v1/messages/count_tokens"));
  assert.equal("max_tokens" in fake.seen[0].body, false);
});
