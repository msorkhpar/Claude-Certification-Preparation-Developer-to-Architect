import { test } from "node:test";
import assert from "node:assert/strict";
import Anthropic from "@anthropic-ai/sdk";
import { BAD_REQUEST, HELLO, OVERLOADED, SPEND_CAP, clientFor } from "./sdkRetries.ts";

test("two retries recover from two overloads", async () => {
  const { fake, client, params } = clientFor([OVERLOADED, OVERLOADED, HELLO], 2);
  assert.equal(((await client.messages.create(params)).content[0] as any).text, "Hello.");
  assert.deepEqual(fake.seen.map((r) => r.headers["x-stainless-retry-count"]), ["0", "1", "2"]);
});

test("no retries surfaces the status and the request id", async () => {
  const { fake, client, params } = clientFor([OVERLOADED, HELLO], 0);
  await assert.rejects(client.messages.create(params), (e: any) => e instanceof Anthropic.APIError && e.status === 529 && e.requestID === "req_illustrative_0529");
  assert.equal(fake.seen.length, 1);
});

test("a 400 is not retried", async () => {
  const { fake, client, params } = clientFor([BAD_REQUEST, HELLO], 2);
  await assert.rejects(client.messages.create(params), (e: any) => e instanceof Anthropic.BadRequestError);
  assert.equal(fake.seen.length, 1);
});

test("the SDK retries a spend-cap 429 although it cannot succeed", async () => {
  const { fake, client, params } = clientFor([SPEND_CAP, SPEND_CAP, SPEND_CAP], 2);
  await assert.rejects(client.messages.create(params), (e: any) => e instanceof Anthropic.RateLimitError);
  assert.equal(fake.seen.length, 3);
});
