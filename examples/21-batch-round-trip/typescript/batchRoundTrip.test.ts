import { test } from "node:test";
import assert from "node:assert/strict";
import { MODEL, TICKETS, clientFor, script } from "./batchRoundTrip.ts";

test("create posts one request per ticket with its custom id", async () => {
  const { fake, client } = clientFor(script());
  const requests = Object.entries(TICKETS).map(([custom_id, body]) => ({ custom_id, params: { model: MODEL, max_tokens: 50, messages: [{ role: "user" as const, content: body }] } }));
  await client.messages.batches.create({ requests });
  assert.ok(fake.seen[0].url.endsWith("/v1/messages/batches"));
  assert.deepEqual(fake.seen[0].body.requests.map((r: any) => r.custom_id), Object.keys(TICKETS));
});

test("results come back in a different order than the requests", async () => {
  const { client } = clientFor(script().slice(3));
  const order: string[] = [];
  for await (const item of await client.messages.batches.results("msgbatch_illustrative")) order.push(item.custom_id);
  assert.deepEqual(order, ["t-3", "t-1", "t-4", "t-2"]);
});

test("each non-success result has its own type", async () => {
  const { client } = clientFor(script().slice(3));
  const kinds: Record<string, string> = {};
  for await (const item of await client.messages.batches.results("msgbatch_illustrative")) kinds[item.custom_id] = item.result.type;
  assert.deepEqual(kinds, { "t-3": "succeeded", "t-1": "succeeded", "t-4": "errored", "t-2": "expired" });
});
