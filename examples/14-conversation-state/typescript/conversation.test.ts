import { test } from "node:test";
import assert from "node:assert/strict";
import Anthropic from "@anthropic-ai/sdk";
import { scriptedFetch } from "../../../harness/ts/scriptedFetch.ts";
import { QUESTIONS, REPLIES, run } from "./conversation.ts";

async function drive() {
  const fake = scriptedFetch(REPLIES);
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  return { fake, result: await run(client, QUESTIONS) };
}

test("every request carries the whole history", async () => {
  const { fake } = await drive();
  assert.deepEqual(fake.seen.map((r) => r.body.messages.length), [1, 3, 5]);
  assert.deepEqual(fake.seen[2].body.messages.map((m: any) => m.role), ["user", "assistant", "user", "assistant", "user"]);
});

test("assistant turns are sent back as received", async () => {
  const { fake } = await drive();
  assert.deepEqual(fake.seen[1].body.messages[1], { role: "assistant", content: [{ type: "text", text: "Paris." }] });
});

test("usage adds up and stop reasons are read", async () => {
  const { result } = await drive();
  assert.deepEqual(result.totals, { input: 89, output: 12 });
  assert.deepEqual(result.replies.map((r) => r.stop_reason), ["end_turn", "max_tokens", "stop_sequence"]);
});

test("system is top-level, never a message role", async () => {
  const { fake } = await drive();
  assert.ok(fake.seen.every((r) => r.body.system && r.body.messages.every((m: any) => m.role !== "system")));
});
