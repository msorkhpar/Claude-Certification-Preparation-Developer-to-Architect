import { test } from "node:test";
import assert from "node:assert/strict";
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { rawCall, sdkCall } from "./rawVsSdk.ts";

const ok = { body: message([text("Paris.")]) };
const limited = { status: 429, body: { type: "error", error: { type: "rate_limit_error", message: "x" } }, headers: { "request-id": "req_illustrative_0001" } };

test("raw and SDK send the same request contract", async () => {
  const raw = scriptedFetch([ok]);
  const sdk = scriptedFetch([ok]);
  await rawCall(raw.fetch);
  await sdkCall(sdk.fetch);
  assert.equal(raw.seen[0].url, sdk.seen[0].url);
  assert.deepEqual(raw.seen[0].body, sdk.seen[0].body);
  assert.equal(raw.seen[0].headers["anthropic-version"], "2023-06-01");
  assert.equal(sdk.seen[0].headers["anthropic-version"], "2023-06-01");
});

test("the SDK adds headers the raw call does not send", async () => {
  const raw = scriptedFetch([ok]);
  const sdk = scriptedFetch([ok]);
  await rawCall(raw.fetch);
  await sdkCall(sdk.fetch);
  assert.ok("x-stainless-lang" in sdk.seen[0].headers);
  assert.ok(!("x-stainless-lang" in raw.seen[0].headers));
});

test("a 429 is a status for raw code and a typed error for the SDK", async () => {
  assert.equal((await rawCall(scriptedFetch([limited]).fetch)).status, 429);
  await assert.rejects(sdkCall(scriptedFetch([limited]).fetch), (err: any) => err instanceof Anthropic.RateLimitError && err.requestID === "req_illustrative_0001");
});
