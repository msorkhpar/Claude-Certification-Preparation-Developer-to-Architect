import { test } from "node:test";
import assert from "node:assert/strict";
import { THINKING_BLOCK, USAGE, clientFor, request } from "./thinking.ts";
import { message, text } from "../../../harness/ts/scriptedFetch.ts";

test("the request carries adaptive thinking and effort in output_config", async () => {
  const { fake, client } = clientFor(message([text("x")]));
  await request(client, "medium");
  const sent = fake.seen[0].body;
  assert.deepEqual(sent.thinking, { type: "adaptive" });
  assert.deepEqual(sent.output_config, { effort: "medium" });
  assert.equal(JSON.stringify(sent).includes("budget_tokens"), false);
  assert.equal("temperature" in sent, false);
});

test("a thinking block can be empty and billed tokens exceed visible ones", async () => {
  const { client } = clientFor(message([THINKING_BLOCK, text("A")], "end_turn", USAGE as any));
  const reply: any = await request(client, "high");
  assert.equal(reply.content[0].type, "thinking");
  assert.equal(reply.content[0].thinking, "");
  assert.equal(reply.usage.output_tokens_details.thinking_tokens, 1650);
  assert.equal(reply.usage.output_tokens, 1900);
});

test("a turn may have no thinking block at all", async () => {
  const { client } = clientFor(message([text("B.")]));
  const reply: any = await request(client, "low");
  assert.deepEqual(reply.content.map((b: any) => b.type), ["text"]);
});
