import { test } from "node:test";
import assert from "node:assert/strict";
import Anthropic from "@anthropic-ai/sdk";
import { EVENTS, FAILING, PARAMS, clientFor } from "./streaming.ts";

test("the text handler receives the text deltas only", async () => {
  const { client } = clientFor(EVENTS);
  const pieces: string[] = [];
  await client.messages.stream(PARAMS).on("text", (t) => pieces.push(t)).finalMessage();
  assert.deepEqual(pieces, ["Let me ", "check."]);
});

test("the final message assembles text, tool input and usage", async () => {
  const { client } = clientFor(EVENTS);
  const final = await client.messages.stream(PARAMS).finalMessage();
  assert.equal(final.stop_reason, "tool_use");
  assert.equal((final.content[0] as any).text, "Let me check.");
  assert.deepEqual((final.content[1] as any).input, { city: "Paris" });
  assert.deepEqual([final.usage.input_tokens, final.usage.output_tokens], [52, 38]);
});

test("an error event after a 200 rejects", async () => {
  const { client } = clientFor(FAILING);
  await assert.rejects(client.messages.stream(PARAMS).finalMessage(), (e: any) => e instanceof Anthropic.APIError);
});
